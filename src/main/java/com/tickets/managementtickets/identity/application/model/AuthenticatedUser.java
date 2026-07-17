package com.tickets.managementtickets.identity.application.model;

import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.identity.domain.model.Role;

import java.security.Principal;
import java.util.Set;

public record AuthenticatedUser(
    String id,
    String email,
    String firstName,
    String lastName,
    Role role,
    Set<Permission> permissions
) implements Principal {

    @Override
    public String getName() {
        return email;
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }
}
