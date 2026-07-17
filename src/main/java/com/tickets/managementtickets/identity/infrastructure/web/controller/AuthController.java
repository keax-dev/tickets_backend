package com.tickets.managementtickets.identity.infrastructure.web.controller;

import com.tickets.managementtickets.identity.application.service.AuthService;
import com.tickets.managementtickets.identity.infrastructure.security.SecurityProperties;
import com.tickets.managementtickets.identity.infrastructure.web.dto.AuthResponse;
import com.tickets.managementtickets.identity.infrastructure.web.dto.CurrentUserResponse;
import com.tickets.managementtickets.identity.infrastructure.web.dto.LoginRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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
public class AuthController {

    private final AuthService authService;
    private final SecurityProperties securityProperties;

    public AuthController(AuthService authService, SecurityProperties securityProperties) {
        this.authService = authService;
        this.securityProperties = securityProperties;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletResponse response
    ) {
        AuthService.AuthResult authResult = authService.login(request.email(), request.password());
        response.addHeader("Set-Cookie", authResult.refreshCookie().toString());
        return ResponseEntity.ok(AuthResponse.from(authResult.response()));
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        AuthService.AuthResult authResult = authService.refresh(extractRefreshCookie(request));
        response.addHeader("Set-Cookie", authResult.refreshCookie().toString());
        return AuthResponse.from(authResult.response());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(extractRefreshCookie(request));
        response.addHeader("Set-Cookie", authService.clearRefreshCookie().toString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
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
}
