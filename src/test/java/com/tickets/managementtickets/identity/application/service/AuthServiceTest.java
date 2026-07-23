package com.tickets.managementtickets.identity.application.service;

import com.tickets.managementtickets.identity.application.port.AccessTokenService;
import com.tickets.managementtickets.identity.application.port.AuthSecuritySettings;
import com.tickets.managementtickets.identity.application.port.CurrentAuthenticatedUserProvider;
import com.tickets.managementtickets.identity.application.port.RefreshTokenGenerator;
import com.tickets.managementtickets.identity.application.port.RefreshTokenRepositoryPort;
import com.tickets.managementtickets.identity.application.port.UserRepositoryPort;
import com.tickets.managementtickets.identity.domain.model.Role;
import com.tickets.managementtickets.identity.domain.model.User;
import com.tickets.managementtickets.shared.application.exception.UnauthorizedException;
import com.tickets.managementtickets.shared.application.port.HashingService;
import com.tickets.managementtickets.shared.application.port.PasswordHashingService;
import com.tickets.managementtickets.shared.application.port.TransactionRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-16T00:00:00Z");

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private RefreshTokenRepositoryPort refreshTokenRepository;

    @Mock
    private PasswordHashingService passwordHashingService;

    @Mock
    private AccessTokenService accessTokenService;

    @Mock
    private RefreshTokenGenerator refreshTokenGenerator;

    @Mock
    private HashingService hashingService;

    @Mock
    private CurrentAuthenticatedUserProvider currentUserProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
            userRepository,
            refreshTokenRepository,
            passwordHashingService,
            new RolePermissionService(),
            accessTokenService,
            refreshTokenGenerator,
            new TestAuthSecuritySettings(),
            hashingService,
            currentUserProvider,
            Clock.fixed(NOW, ZoneOffset.UTC),
            new DirectTransactionRunner()
        );
    }

    @Test
    void shouldLockAccountAfterConfiguredFailedAttempts() {
        User user = new User(
            "user-1",
            "Ana",
            "Agent",
            "ana@test.com",
            "encoded-password",
            Role.SUPPORT_AGENT,
            true,
            4,
            null,
            null,
            0
        );
        when(userRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(user));
        when(passwordHashingService.matches("bad-password", "encoded-password")).thenReturn(false);

        UnauthorizedException exception = assertThrows(
            UnauthorizedException.class,
            () -> authService.login("ana@test.com", "bad-password")
        );

        assertEquals("INVALID_CREDENTIALS", exception.getCode());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(5, userCaptor.getValue().failedLoginAttempts());
        assertEquals(NOW.plusSeconds(15 * 60L), userCaptor.getValue().lockedUntil());
    }

    @Test
    void shouldRejectLockedAccountBeforeCheckingPassword() {
        User user = new User(
            "user-1",
            "Ana",
            "Agent",
            "ana@test.com",
            "encoded-password",
            Role.SUPPORT_AGENT,
            true,
            5,
            null,
            NOW.plusSeconds(60),
            0
        );
        when(userRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(user));

        UnauthorizedException exception = assertThrows(
            UnauthorizedException.class,
            () -> authService.login("ana@test.com", "any-password")
        );

        assertEquals("ACCOUNT_LOCKED", exception.getCode());
        verify(passwordHashingService, never()).matches("any-password", "encoded-password");
        assertTrue(user.isLoginLocked(NOW));
    }

    private static final class DirectTransactionRunner implements TransactionRunner {

        @Override
        public <T> T readOnly(java.util.function.Supplier<T> action) {
            return action.get();
        }

        @Override
        public <T> T required(java.util.function.Supplier<T> action) {
            return action.get();
        }
    }

    private static final class TestAuthSecuritySettings implements AuthSecuritySettings {

        @Override
        public int getAccessTokenExpirationMinutes() {
            return 15;
        }

        @Override
        public int getRefreshTokenExpirationDays() {
            return 7;
        }

        @Override
        public String getIssuer() {
            return "management-tickets-test";
        }

        @Override
        public String getJwtSecret() {
            return "management-tickets-test-secret-key-that-is-long-enough";
        }

        @Override
        public String getRefreshCookieName() {
            return "refresh_token";
        }

        @Override
        public boolean isRefreshCookieSecure() {
            return false;
        }

        @Override
        public int getMaxFailedLoginAttempts() {
            return 5;
        }

        @Override
        public int getAccountLockMinutes() {
            return 15;
        }
    }
}
