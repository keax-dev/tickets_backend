package com.tickets.managementtickets.identity.infrastructure.security;

import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.application.port.AuthSecuritySettings;
import com.tickets.managementtickets.identity.application.service.RolePermissionService;
import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.identity.domain.model.Role;
import com.tickets.managementtickets.shared.application.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// This test suite verifies JWT generation and parsing without starting the Spring security stack.
class JwtTokenServiceTest {

    // Use a far-future instant so generated tokens are never accidentally expired during test execution.
    private static final Instant NOW = Instant.parse("2099-07-16T00:00:00Z");

    @Test
    void shouldGenerateAndParseAccessTokenWithExpectedClaims() {
        // Arrange: create the token service with fixed security settings and time.
        JwtTokenService tokenService = tokenService("management-tickets-test");
        AuthenticatedUser user = new AuthenticatedUser(
            "user-1",
            "agent@test.com",
            "Ana",
            "Agent",
            Role.SUPPORT_AGENT,
            Set.of(Permission.TICKET_READ_ASSIGNED)
        );

        // Act: generate and parse an access token for the authenticated user.
        String token = tokenService.generateAccessToken(user);
        AuthenticatedUser parsedUser = tokenService.parseAccessToken(token);

        // Assert: verify identity claims and permissions are restored from the role.
        assertEquals("user-1", parsedUser.id());
        assertEquals("agent@test.com", parsedUser.email());
        assertEquals("Ana", parsedUser.firstName());
        assertEquals("Agent", parsedUser.lastName());
        assertEquals(Role.SUPPORT_AGENT, parsedUser.role());
        assertTrue(parsedUser.permissions().contains(Permission.TICKET_READ_ASSIGNED));
        assertEquals(NOW.plusSeconds(15 * 60L), tokenService.resolveAccessTokenExpiration());
    }

    @Test
    void shouldRejectInvalidAccessToken() {
        // Arrange: create the token service with fixed security settings.
        JwtTokenService tokenService = tokenService("management-tickets-test");

        // Act: parse a malformed token.
        UnauthorizedException exception = assertThrows(
            UnauthorizedException.class,
            () -> tokenService.parseAccessToken("not-a-valid-jwt")
        );

        // Assert: verify invalid tokens are mapped to the application-level auth error.
        assertEquals("INVALID_ACCESS_TOKEN", exception.getCode());
    }

    @Test
    void shouldRejectTokenSignedForAnotherIssuer() {
        // Arrange: generate a token with one issuer and parse it with another expected issuer.
        JwtTokenService issuerA = tokenService("issuer-a");
        JwtTokenService issuerB = tokenService("issuer-b");
        String token = issuerA.generateAccessToken(new AuthenticatedUser(
            "user-1",
            "agent@test.com",
            "Ana",
            "Agent",
            Role.SUPPORT_AGENT,
            Set.of(Permission.TICKET_READ_ASSIGNED)
        ));

        // Act: parse the token with the wrong issuer expectation.
        UnauthorizedException exception = assertThrows(
            UnauthorizedException.class,
            () -> issuerB.parseAccessToken(token)
        );

        // Assert: verify issuer mismatches are rejected as invalid access tokens.
        assertEquals("INVALID_ACCESS_TOKEN", exception.getCode());
    }

    private JwtTokenService tokenService(String issuer) {
        // Create the real token service with deterministic time, deterministic configuration,
        // and the real role-permission mapper used by authentication flows.
        return new JwtTokenService(
            new TestAuthSecuritySettings(issuer),
            Clock.fixed(NOW, ZoneOffset.UTC),
            new RolePermissionService()
        );
    }

    // This tiny settings implementation replaces external configuration and keeps the test self-contained.
    private record TestAuthSecuritySettings(String issuer) implements AuthSecuritySettings {

        @Override
        public int getAccessTokenExpirationMinutes() {
            // Access tokens expire after 15 minutes in these tests.
            return 15;
        }

        @Override
        public int getRefreshTokenExpirationDays() {
            // Refresh token duration is required by the contract even if these tests focus on access tokens.
            return 7;
        }

        @Override
        public String getIssuer() {
            // Return the issuer chosen by each test scenario.
            return issuer;
        }

        @Override
        public String getJwtSecret() {
            // Provide a sufficiently long HMAC secret accepted by the JWT library.
            return "management-tickets-test-secret-key-that-is-long-enough";
        }

        @Override
        public String getRefreshCookieName() {
            // Included for completeness because the settings interface requires it.
            return "refresh_token";
        }

        @Override
        public boolean isRefreshCookieSecure() {
            // False is fine in tests because no browser/cookie integration is exercised here.
            return false;
        }

        @Override
        public int getMaxFailedLoginAttempts() {
            // Unused in this suite, but required by the settings contract.
            return 5;
        }

        @Override
        public int getAccountLockMinutes() {
            // Unused in this suite, but required by the settings contract.
            return 15;
        }
    }
}
