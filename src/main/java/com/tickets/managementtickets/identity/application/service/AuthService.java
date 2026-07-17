package com.tickets.managementtickets.identity.application.service;

import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.application.port.AccessTokenService;
import com.tickets.managementtickets.identity.application.port.AuthSecuritySettings;
import com.tickets.managementtickets.identity.application.port.CurrentAuthenticatedUserProvider;
import com.tickets.managementtickets.identity.application.port.RefreshTokenGenerator;
import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.identity.domain.model.Role;
import com.tickets.managementtickets.identity.infrastructure.persistence.entity.RefreshTokenEntity;
import com.tickets.managementtickets.identity.infrastructure.persistence.entity.UserEntity;
import com.tickets.managementtickets.identity.infrastructure.persistence.repository.RefreshTokenRepository;
import com.tickets.managementtickets.identity.infrastructure.persistence.repository.UserRepository;
import com.tickets.managementtickets.shared.application.exception.NotFoundException;
import com.tickets.managementtickets.shared.application.exception.UnauthorizedException;
import com.tickets.managementtickets.shared.application.port.HashingService;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final RolePermissionService rolePermissionService;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final AuthSecuritySettings securityProperties;
    private final HashingService hashingService;
    private final CurrentAuthenticatedUserProvider currentUserProvider;
    private final Clock clock;
    private final Environment environment;

    public AuthService(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        RolePermissionService rolePermissionService,
        AccessTokenService accessTokenService,
        RefreshTokenGenerator refreshTokenGenerator,
        AuthSecuritySettings securityProperties,
        HashingService hashingService,
        CurrentAuthenticatedUserProvider currentUserProvider,
        Clock clock,
        Environment environment
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.rolePermissionService = rolePermissionService;
        this.accessTokenService = accessTokenService;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.securityProperties = securityProperties;
        this.hashingService = hashingService;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
        this.environment = environment;
    }

    @Transactional
    public AuthResult login(String email, String password) {
        UserEntity user = userRepository.findByEmail(normalizeEmail(email))
            .orElseThrow(() -> new UnauthorizedException("INVALID_CREDENTIALS", "Invalid credentials."));

        if (!user.isActive()) {
            throw new UnauthorizedException("USER_INACTIVE", "The user is inactive.");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            throw new UnauthorizedException("INVALID_CREDENTIALS", "Invalid credentials.");
        }

        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(clock.instant());
        return issueTokens(buildAuthenticatedUser(user), null);
    }

    @Transactional
    public AuthResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException("REFRESH_TOKEN_MISSING", "A refresh token is required.");
        }

        String tokenHash = hashingService.hash(rawRefreshToken);
        RefreshTokenEntity existingToken = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new UnauthorizedException("INVALID_REFRESH_TOKEN", "The refresh token is invalid."));

        if (existingToken.getRevokedAt() != null) {
            throw new UnauthorizedException("REFRESH_TOKEN_REVOKED", "The refresh token has already been revoked.");
        }
        if (existingToken.getExpiresAt().isBefore(clock.instant())) {
            throw new UnauthorizedException("REFRESH_TOKEN_EXPIRED", "The refresh token has expired.");
        }

        UserEntity user = userRepository.findById(existingToken.getUserId())
            .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "The user could not be found."));

        if (!user.isActive()) {
            throw new UnauthorizedException("USER_INACTIVE", "The user is inactive.");
        }

        return issueTokens(buildAuthenticatedUser(user), existingToken);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        refreshTokenRepository.findByTokenHash(hashingService.hash(rawRefreshToken))
            .ifPresent(token -> token.setRevokedAt(clock.instant()));
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        return toUserResponse(currentUserProvider.requireCurrentUser());
    }

    public ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(securityProperties.getRefreshCookieName(), "")
            .httpOnly(true)
            .secure(isProduction())
            .sameSite("Lax")
            .path("/api/v1/auth")
            .maxAge(0)
            .build();
    }

    @Transactional
    public void purgeExpiredRefreshTokens() {
        refreshTokenRepository.deleteByExpiresAtBefore(clock.instant());
    }

    private AuthResult issueTokens(AuthenticatedUser user, RefreshTokenEntity previousToken) {
        String rawRefreshToken = refreshTokenGenerator.generateToken();

        RefreshTokenEntity newRefreshToken = new RefreshTokenEntity();
        newRefreshToken.setUserId(user.id());
        newRefreshToken.setTokenHash(hashingService.hash(rawRefreshToken));
        newRefreshToken.setExpiresAt(clock.instant().plus(securityProperties.getRefreshTokenExpirationDays(), ChronoUnit.DAYS));
        refreshTokenRepository.save(newRefreshToken);

        if (previousToken != null) {
            previousToken.setRevokedAt(clock.instant());
            previousToken.setReplacedByTokenId(newRefreshToken.getId());
        }

        return new AuthResult(
            new AuthResponse(
                accessTokenService.generateAccessToken(user),
                accessTokenService.resolveAccessTokenExpiration(),
                toUserResponse(user)
            ),
            ResponseCookie.from(securityProperties.getRefreshCookieName(), rawRefreshToken)
                .httpOnly(true)
                .secure(isProduction())
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(securityProperties.getRefreshTokenExpirationDays() * 24L * 60L * 60L)
                .build()
        );
    }

    private boolean isProduction() {
        return environment.acceptsProfiles(Profiles.of("prod"));
    }

    private AuthenticatedUser buildAuthenticatedUser(UserEntity user) {
        Role role = user.getRole();
        return new AuthenticatedUser(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            role,
            rolePermissionService.resolvePermissions(role)
        );
    }

    private UserResponse toUserResponse(AuthenticatedUser user) {
        List<String> permissions = user.permissions().stream().map(Permission::name).toList();
        return new UserResponse(user.id(), user.firstName(), user.lastName(), user.email(), user.role(), permissions);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public record AuthResponse(String accessToken, Instant expiresAt, UserResponse user) {
    }

    public record UserResponse(
        String id,
        String firstName,
        String lastName,
        String email,
        Role role,
        List<String> permissions
    ) {
    }

    public record AuthResult(AuthResponse response, ResponseCookie refreshCookie) {
    }
}
