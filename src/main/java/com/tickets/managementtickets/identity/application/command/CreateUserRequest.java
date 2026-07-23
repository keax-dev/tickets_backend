package com.tickets.managementtickets.identity.application.command;

import com.tickets.managementtickets.identity.domain.model.Role;

public record CreateUserRequest(String firstName, String lastName, String email, String password, Role role) {
}
