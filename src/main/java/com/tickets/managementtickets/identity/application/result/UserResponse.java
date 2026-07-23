package com.tickets.managementtickets.identity.application.result;

import com.tickets.managementtickets.identity.domain.model.Role;

import java.time.Instant;

public record UserResponse(
    String id,
    String firstName,
    String lastName,
    String email,
    Role role,
    boolean active,
    Instant lastLoginAt,
    long version
) {
}
