package com.tickets.managementtickets.identity.application.service;

import com.tickets.managementtickets.identity.application.model.AuthCookie;
import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.application.port.AccessTokenService;
import com.tickets.managementtickets.identity.application.port.AuthMetricsPort;
import com.tickets.managementtickets.identity.application.port.AuthSecuritySettings;
import com.tickets.managementtickets.identity.application.port.CurrentAuthenticatedUserProvider;
import com.tickets.managementtickets.identity.application.port.RefreshTokenRepositoryPort;
import com.tickets.managementtickets.identity.application.port.RefreshTokenGenerator;
import com.tickets.managementtickets.identity.application.port.UserRepositoryPort;
import com.tickets.managementtickets.identity.application.result.AuthResult;
import com.tickets.managementtickets.identity.application.result.AuthenticatedUserResponse;
import com.tickets.managementtickets.identity.domain.model.RefreshToken;
import com.tickets.managementtickets.identity.domain.model.Role;
import com.tickets.managementtickets.identity.domain.model.User;
import com.tickets.managementtickets.shared.application.exception.NotFoundException;
import com.tickets.managementtickets.shared.application.exception.UnauthorizedException;
import com.tickets.managementtickets.shared.application.port.HashingService;
import com.tickets.managementtickets.shared.application.port.PasswordHashingService;
import com.tickets.managementtickets.shared.application.port.TransactionRunner;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class AuthService {

    private final UserRepositoryPort userRepository;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final PasswordHashingService passwordHashingService;
    private final RolePermissionService rolePermissionService;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final AuthSecuritySettings securityProperties;
    private final HashingService hashingService;
    private final CurrentAuthenticatedUserProvider currentUserProvider;
    private final Clock clock;
    private final TransactionRunner transactionRunner;
    private final IdentityResponseMapper responseMapper;
    private final AuthMetricsPort metricsPort;

    public AuthService(
        UserRepositoryPort userRepository,
        RefreshTokenRepositoryPort refreshTokenRepository,
        PasswordHashingService passwordHashingService,
        RolePermissionService rolePermissionService,
        AccessTokenService accessTokenService,
        RefreshTokenGenerator refreshTokenGenerator,
        AuthSecuritySettings securityProperties,
        HashingService hashingService,
        CurrentAuthenticatedUserProvider currentUserProvider,
        Clock clock,
        TransactionRunner transactionRunner
    ) {
        this(
            userRepository,
            refreshTokenRepository,
            passwordHashingService,
            rolePermissionService,
            accessTokenService,
            refreshTokenGenerator,
            securityProperties,
            hashingService,
            currentUserProvider,
            clock,
            transactionRunner,
            AuthMetricsPort.NO_OP
        );
    }

    public AuthService(
        UserRepositoryPort userRepository,
        RefreshTokenRepositoryPort refreshTokenRepository,
        PasswordHashingService passwordHashingService,
        RolePermissionService rolePermissionService,
        AccessTokenService accessTokenService,
        RefreshTokenGenerator refreshTokenGenerator,
        AuthSecuritySettings securityProperties,
        HashingService hashingService,
        CurrentAuthenticatedUserProvider currentUserProvider,
        Clock clock,
        TransactionRunner transactionRunner,
        AuthMetricsPort metricsPort
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHashingService = passwordHashingService;
        this.rolePermissionService = rolePermissionService;
        this.accessTokenService = accessTokenService;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.securityProperties = securityProperties;
        this.hashingService = hashingService;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
        this.transactionRunner = transactionRunner;
        this.responseMapper = new IdentityResponseMapper();
        this.metricsPort = metricsPort;
    }

    public AuthResult login(String email, String password) {
        return transactionRunner.required(() -> {
            User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> unauthorized("INVALID_CREDENTIALS", "Invalid credentials."));

            if (!user.active()) {
                throw unauthorized("USER_INACTIVE", "The user is inactive.");
            }

            Instant now = clock.instant();
            User loginCandidate = user.clearExpiredLoginLock(now);
            if (loginCandidate.isLoginLocked(now)) {
                throw unauthorized("ACCOUNT_LOCKED", "The account is temporarily locked. Please try again later.");
            }

            if (!passwordHashingService.matches(password, loginCandidate.passwordHash())) {
                userRepository.save(loginCandidate.recordFailedLogin(
                    now,
                    securityProperties.getMaxFailedLoginAttempts(),
                    securityProperties.getAccountLockMinutes()
                ));
                throw unauthorized("INVALID_CREDENTIALS", "Invalid credentials.");
            }

            User loggedInUser = userRepository.save(loginCandidate.recordSuccessfulLogin(now));
            metricsPort.recordLoginSuccess(loggedInUser.role());
            return issueTokens(buildAuthenticatedUser(loggedInUser), null);
        });
    }

    public AuthResult refresh(String rawRefreshToken) {
        return transactionRunner.required(() -> {
            if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
                throw refreshUnauthorized("REFRESH_TOKEN_MISSING", "A refresh token is required.");
            }

            String tokenHash = hashingService.hash(rawRefreshToken);
            RefreshToken existingToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> refreshUnauthorized("INVALID_REFRESH_TOKEN", "The refresh token is invalid."));

            if (existingToken.revokedAt() != null) {
                throw refreshUnauthorized("REFRESH_TOKEN_REVOKED", "The refresh token has already been revoked.");
            }
            if (existingToken.expiresAt().isBefore(clock.instant())) {
                throw refreshUnauthorized("REFRESH_TOKEN_EXPIRED", "The refresh token has expired.");
            }

            User user = userRepository.findById(existingToken.userId())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "The user could not be found."));

            if (!user.active()) {
                throw refreshUnauthorized("USER_INACTIVE", "The user is inactive.");
            }

            metricsPort.recordRefreshSuccess(user.role());
            return issueTokens(buildAuthenticatedUser(user), existingToken);
        });
    }

    public void logout(String rawRefreshToken) {
        transactionRunner.required(() -> {
            if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
                return;
            }

            refreshTokenRepository.findByTokenHash(hashingService.hash(rawRefreshToken))
                .ifPresent(token -> refreshTokenRepository.save(token.revoke(clock.instant())));
        });
    }

    public AuthenticatedUserResponse me() {
        return transactionRunner.readOnly(() -> responseMapper.toAuthenticatedUserResponse(currentUserProvider.requireCurrentUser()));
    }

    public AuthCookie clearRefreshCookie() {
        return refreshCookie("", 0);
    }

    public void purgeExpiredRefreshTokens() {
        transactionRunner.required(() -> refreshTokenRepository.deleteByExpiresAtBefore(clock.instant()));
    }

    private AuthResult issueTokens(AuthenticatedUser user, RefreshToken previousToken) {
        String rawRefreshToken = refreshTokenGenerator.generateToken();

        RefreshToken newRefreshToken = refreshTokenRepository.save(RefreshToken.issue(
            user.id(),
            hashingService.hash(rawRefreshToken),
            clock.instant().plus(securityProperties.getRefreshTokenExpirationDays(), ChronoUnit.DAYS)
        ));

        if (previousToken != null) {
            refreshTokenRepository.save(previousToken.replaceBy(newRefreshToken.id(), clock.instant()));
        }

        return new AuthResult(
            responseMapper.toAuthResponse(
                accessTokenService.generateAccessToken(user),
                accessTokenService.resolveAccessTokenExpiration(),
                user
            ),
            refreshCookie(rawRefreshToken, securityProperties.getRefreshTokenExpirationDays() * 24L * 60L * 60L)
        );
    }

    private AuthCookie refreshCookie(String value, long maxAgeSeconds) {
        return new AuthCookie(
            securityProperties.getRefreshCookieName(),
            value,
            true,
            securityProperties.isRefreshCookieSecure(),
            "Strict",
            "/api/v1/auth",
            maxAgeSeconds
        );
    }

    private AuthenticatedUser buildAuthenticatedUser(User user) {
        Role role = user.role();
        return new AuthenticatedUser(
            user.id(),
            user.email(),
            user.firstName(),
            user.lastName(),
            role,
            rolePermissionService.resolvePermissions(role)
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private UnauthorizedException unauthorized(String code, String message) {
        metricsPort.recordLoginFailure(code);
        return new UnauthorizedException(code, message);
    }

    private UnauthorizedException refreshUnauthorized(String code, String message) {
        metricsPort.recordRefreshFailure(code);
        return new UnauthorizedException(code, message);
    }

}
