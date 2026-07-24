package com.tickets.managementtickets.identity.infrastructure.web.controller;

import com.tickets.managementtickets.identity.application.service.AuthService;
import com.tickets.managementtickets.identity.application.model.AuthCookie;
import com.tickets.managementtickets.identity.application.result.AuthResult;
import com.tickets.managementtickets.identity.infrastructure.security.SecurityProperties;
import com.tickets.managementtickets.identity.infrastructure.web.ratelimit.LoginRateLimiter;
import com.tickets.managementtickets.identity.infrastructure.web.dto.AuthResponse;
import com.tickets.managementtickets.identity.infrastructure.web.dto.CurrentUserResponse;
import com.tickets.managementtickets.identity.infrastructure.web.dto.LoginRequest;
import com.tickets.managementtickets.shared.application.exception.ApplicationException;
import com.tickets.managementtickets.shared.infrastructure.web.ApiProblemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and current-session endpoints.")
public class AuthController {

    private final AuthService authService;
    private final SecurityProperties securityProperties;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(AuthService authService, SecurityProperties securityProperties, LoginRateLimiter loginRateLimiter) {
        this.authService = authService;
        this.securityProperties = securityProperties;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a user", description = "Validates credentials, issues a JWT access token, and rotates the HttpOnly refresh token cookie.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authentication succeeded.", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Request validation failed.", content = @Content(schema = @Schema(implementation = ApiProblemResponse.class))),
        @ApiResponse(responseCode = "401", description = "Credentials are invalid or the user is inactive.", content = @Content(schema = @Schema(implementation = ApiProblemResponse.class))),
        @ApiResponse(responseCode = "429", description = "Login rate limit exceeded.", content = @Content(schema = @Schema(implementation = ApiProblemResponse.class)))
    })
    public ResponseEntity<AuthResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest servletRequest,
        HttpServletResponse response
    ) {
        String rateLimitKey = loginRateLimiter.keyFor(servletRequest.getRemoteAddr(), request.email());
        loginRateLimiter.checkAllowed(rateLimitKey);
        AuthResult authResult;
        try {
            authResult = authService.login(request.email(), request.password());
            loginRateLimiter.reset(rateLimitKey);
        } catch (ApplicationException exception) {
            loginRateLimiter.recordFailure(rateLimitKey);
            throw exception;
        }
        response.addHeader("Set-Cookie", toResponseCookie(authResult.refreshCookie()).toString());
        return ResponseEntity.ok(AuthResponse.from(authResult.response()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh the access token", description = "Uses the refresh token cookie to issue a new access token and a rotated refresh cookie.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Refresh succeeded.", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Refresh token is missing, invalid, expired, or revoked.", content = @Content(schema = @Schema(implementation = ApiProblemResponse.class)))
    })
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        AuthResult authResult = authService.refresh(extractRefreshCookie(request));
        response.addHeader("Set-Cookie", toResponseCookie(authResult.refreshCookie()).toString());
        return AuthResponse.from(authResult.response());
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Log out the current session", description = "Revokes the refresh token associated with the current cookie and clears the cookie on the client.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Logout succeeded and the refresh cookie was cleared."),
        @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiProblemResponse.class)))
    })
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(extractRefreshCookie(request));
        response.addHeader("Set-Cookie", toResponseCookie(authService.clearRefreshCookie()).toString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get the current authenticated user", description = "Returns the authenticated user profile together with the resolved permissions granted to that session.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Current user returned successfully.", content = @Content(schema = @Schema(implementation = CurrentUserResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiProblemResponse.class)))
    })
    public CurrentUserResponse me() {
        return CurrentUserResponse.from(authService.me());
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if (securityProperties.getRefreshCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private ResponseCookie toResponseCookie(AuthCookie cookie) {
        return ResponseCookie.from(cookie.name(), cookie.value())
            .httpOnly(cookie.httpOnly())
            .secure(cookie.secure())
            .sameSite(cookie.sameSite())
            .path(cookie.path())
            .maxAge(cookie.maxAgeSeconds())
            .build();
    }
}
