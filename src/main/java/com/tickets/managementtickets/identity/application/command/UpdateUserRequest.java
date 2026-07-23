package com.tickets.managementtickets.identity.application.command;

import com.tickets.managementtickets.identity.domain.model.Role;

public record UpdateUserRequest(long version, String firstName, String lastName, String email, Role role) {
}
