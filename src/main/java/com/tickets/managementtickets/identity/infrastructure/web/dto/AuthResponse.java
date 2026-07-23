package com.tickets.managementtickets.identity.infrastructure.web.dto;

import java.time.Instant;

public record AuthResponse(String accessToken, Instant expiresAt, CurrentUserResponse user) {

    public static AuthResponse from(com.tickets.managementtickets.identity.application.result.AuthResponse response) {
        return new AuthResponse(response.accessToken(), response.expiresAt(), CurrentUserResponse.from(response.user()));
    }
}
