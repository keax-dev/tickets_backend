package com.tickets.managementtickets.identity.application.result;

import com.tickets.managementtickets.identity.domain.model.Role;

import java.util.List;

public record AuthenticatedUserResponse(
    String id,
    String firstName,
    String lastName,
    String email,
    Role role,
    List<String> permissions
) {
}
