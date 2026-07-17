package com.tickets.managementtickets.identity.infrastructure.web.dto;

import com.tickets.managementtickets.identity.application.service.UserManagementService;
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

    public static UserResponse from(UserManagementService.UserResponse response) {
        return new UserResponse(
            response.id(),
            response.firstName(),
            response.lastName(),
            response.email(),
            response.role(),
            response.active(),
            response.lastLoginAt(),
            response.version()
        );
    }
}
