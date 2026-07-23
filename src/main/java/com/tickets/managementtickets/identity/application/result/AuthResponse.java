package com.tickets.managementtickets.identity.application.result;

import java.time.Instant;

public record AuthResponse(String accessToken, Instant expiresAt, AuthenticatedUserResponse user) {
}
