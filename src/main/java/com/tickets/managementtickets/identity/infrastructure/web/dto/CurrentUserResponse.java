package com.tickets.managementtickets.identity.infrastructure.web.dto;

import com.tickets.managementtickets.identity.domain.model.Role;

import java.util.List;

public record CurrentUserResponse(
    String id,
    String firstName,
    String lastName,
    String email,
    Role role,
    List<String> permissions
) {

    public static CurrentUserResponse from(com.tickets.managementtickets.identity.application.result.AuthenticatedUserResponse response) {
        return new CurrentUserResponse(
            response.id(),
            response.firstName(),
            response.lastName(),
            response.email(),
            response.role(),
            response.permissions()
        );
    }
}
