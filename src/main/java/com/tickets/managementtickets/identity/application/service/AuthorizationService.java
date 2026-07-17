package com.tickets.managementtickets.identity.application.service;

import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.shared.application.exception.ForbiddenException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    public void requirePermission(AuthenticatedUser user, Permission permission) {
        if (!user.hasPermission(permission)) {
            throwAccessDenied();
        }
    }

    public void requireAnyPermission(AuthenticatedUser user, Permission... permissions) {
        for (Permission permission : permissions) {
            if (user.hasPermission(permission)) {
                return;
            }
        }
        throwAccessDenied();
    }

    private void throwAccessDenied() {
        throw new ForbiddenException("ACCESS_DENIED", "You do not have permission to perform this action.");
    }
}
