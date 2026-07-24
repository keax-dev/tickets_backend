package com.tickets.managementtickets.identity.infrastructure.web.controller;

import com.tickets.managementtickets.identity.application.model.AuthCookie;
import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.application.port.AccessTokenService;
import com.tickets.managementtickets.identity.application.result.AuthResult;
import com.tickets.managementtickets.identity.application.service.AuthService;
import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.identity.domain.model.Role;
import com.tickets.managementtickets.identity.infrastructure.security.AccessTokenAuthenticationFilter;
import com.tickets.managementtickets.identity.infrastructure.security.CurrentUserService;
import com.tickets.managementtickets.identity.infrastructure.security.JsonAccessDeniedHandler;
import com.tickets.managementtickets.identity.infrastructure.security.JsonAuthenticationEntryPoint;
import com.tickets.managementtickets.identity.infrastructure.web.dto.LoginRequest;
import com.tickets.managementtickets.shared.application.exception.TooManyRequestsException;
import com.tickets.managementtickets.shared.infrastructure.web.ApiExceptionHandler;
import com.tickets.managementtickets.shared.infrastructure.web.CorrelationIdFilter;
import com.tickets.managementtickets.support.WebSecurityTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// This integration test suite verifies the real HTTP contract of the authentication controller, including login cookies, protected /me access, and rate-limit problem details.
@WebMvcTest(AuthController.class)
@Import({
    WebSecurityTestConfiguration.class,
    AccessTokenAuthenticationFilter.class,
    CurrentUserService.class,
    JsonAuthenticationEntryPoint.class,
    JsonAccessDeniedHandler.class,
    CorrelationIdFilter.class,
    ApiExceptionHandler.class
})
class AuthControllerIT {

    private static final String VALID_TOKEN = "valid-auth-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AccessTokenService accessTokenService;

    @MockitoBean
    private com.tickets.managementtickets.identity.infrastructure.web.ratelimit.LoginRateLimiter loginRateLimiter;

    @Test
    void shouldReturnAccessTokenAndRefreshCookieOnSuccessfulLogin() throws Exception {
        // Arrange: mock the successful authentication result returned by the application service.
        when(authService.login(eq("agent.alvarez@tickets.local"), eq("Password123!"))).thenReturn(authResult());
        String payload = """
            {
              "email": "agent.alvarez@tickets.local",
              "password": "Password123!"
            }
            """;

        // Act and assert: the controller should emit the auth body and the rotated HttpOnly refresh cookie.
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("jwt-access-token"))
            .andExpect(jsonPath("$.user.email").value("agent.alvarez@tickets.local"))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("refresh_token=refresh-token-value")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("HttpOnly")));
    }

    @Test
    void shouldReturnRateLimitProblemDetailWhenLoginAttemptsAreBlocked() throws Exception {
        // Arrange: force the login rate limiter to reject the request before the auth service runs.
        doThrow(new TooManyRequestsException("LOGIN_RATE_LIMITED", "Too many login attempts. Please try again later."))
            .when(loginRateLimiter)
            .checkAllowed(any());
        String payload = """
            {
              "email": "agent.alvarez@tickets.local",
              "password": "Password123!"
            }
            """;

        // Act and assert: the controller advice should convert the rate-limit exception into HTTP 429.
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value("LOGIN_RATE_LIMITED"))
            .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER));
    }

    @Test
    void shouldReturnCurrentUserForAuthenticatedMeEndpoint() throws Exception {
        // Arrange: authenticate the request via the JWT filter and stub the /me application response.
        when(accessTokenService.parseAccessToken(VALID_TOKEN)).thenReturn(authenticatedUser());
        when(authService.me()).thenReturn(new com.tickets.managementtickets.identity.application.result.AuthenticatedUserResponse(
            "user-1",
            "Ana",
            "Alvarez",
            "agent.alvarez@tickets.local",
            Role.SUPPORT_AGENT,
            java.util.List.of("TICKET_READ_ASSIGNED", "TICKET_RESOLVE")
        ));

        // Act and assert: the protected endpoint should serialize the authenticated profile.
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("user-1"))
            .andExpect(jsonPath("$.role").value("SUPPORT_AGENT"))
            .andExpect(jsonPath("$.permissions[0]").value("TICKET_READ_ASSIGNED"));
    }

    private AuthResult authResult() {
        // Build the application-layer login result used by the controller to set both body and cookie.
        return new AuthResult(
            new com.tickets.managementtickets.identity.application.result.AuthResponse(
                "jwt-access-token",
                Instant.parse("2026-07-24T12:15:00Z"),
                new com.tickets.managementtickets.identity.application.result.AuthenticatedUserResponse(
                    "user-1",
                    "Ana",
                    "Alvarez",
                    "agent.alvarez@tickets.local",
                    Role.SUPPORT_AGENT,
                    java.util.List.of("TICKET_READ_ASSIGNED", "TICKET_RESOLVE")
                )
            ),
            new AuthCookie("refresh_token", "refresh-token-value", true, false, "Strict", "/api/v1/auth", 604800)
        );
    }

    private AuthenticatedUser authenticatedUser() {
        // Build the authenticated principal that the JWT filter should install in the security context.
        return new AuthenticatedUser(
            "user-1",
            "agent.alvarez@tickets.local",
            "Ana",
            "Alvarez",
            Role.SUPPORT_AGENT,
            Set.of(Permission.TICKET_READ_ASSIGNED, Permission.TICKET_RESOLVE)
        );
    }
}
