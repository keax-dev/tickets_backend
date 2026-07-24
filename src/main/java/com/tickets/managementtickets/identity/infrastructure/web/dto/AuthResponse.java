package com.tickets.managementtickets.identity.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "AuthResponse", description = "Authentication payload returned after login or refresh.")
public record AuthResponse(
    @Schema(description = "Short-lived JWT access token.", example = "eyJhbGciOiJIUzI1NiJ9...")
    String accessToken,
    @Schema(description = "Instant when the access token expires.")
    Instant expiresAt,
    @Schema(description = "Authenticated user profile and permissions.")
    CurrentUserResponse user
) {

    public static AuthResponse from(com.tickets.managementtickets.identity.application.result.AuthResponse response) {
        return new AuthResponse(response.accessToken(), response.expiresAt(), CurrentUserResponse.from(response.user()));
    }
}
