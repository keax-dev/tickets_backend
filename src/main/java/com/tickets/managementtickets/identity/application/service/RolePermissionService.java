package com.tickets.managementtickets.identity.application.service;

import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.identity.domain.model.Role;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class RolePermissionService {

    private final Map<Role, Set<Permission>> permissionsByRole = new EnumMap<>(Role.class);

    public RolePermissionService() {
        permissionsByRole.put(Role.ADMIN, EnumSet.allOf(Permission.class));
        permissionsByRole.put(Role.SUPPORT_MANAGER, EnumSet.of(
            Permission.USER_READ,
            Permission.CATEGORY_READ,
            Permission.SLA_READ,
            Permission.TICKET_READ_ALL,
            Permission.TICKET_UPDATE,
            Permission.TICKET_ASSIGN,
            Permission.TICKET_REASSIGN,
            Permission.TICKET_CHANGE_PRIORITY,
            Permission.TICKET_CHANGE_STATUS,
            Permission.TICKET_RESOLVE,
            Permission.TICKET_CLOSE,
            Permission.TICKET_REOPEN,
            Permission.TICKET_CANCEL,
            Permission.COMMENT_CREATE_PUBLIC,
            Permission.COMMENT_CREATE_INTERNAL,
            Permission.COMMENT_READ_INTERNAL,
            Permission.AUDIT_READ,
            Permission.DASHBOARD_READ_GLOBAL,
            Permission.NOTIFICATION_READ
        ));
        permissionsByRole.put(Role.SUPPORT_AGENT, EnumSet.of(
            Permission.CATEGORY_READ,
            Permission.TICKET_READ_ASSIGNED,
            Permission.TICKET_CHANGE_STATUS,
            Permission.TICKET_RESOLVE,
            Permission.COMMENT_CREATE_PUBLIC,
            Permission.COMMENT_CREATE_INTERNAL,
            Permission.COMMENT_READ_INTERNAL,
            Permission.DASHBOARD_READ_PERSONAL,
            Permission.NOTIFICATION_READ
        ));
        permissionsByRole.put(Role.CUSTOMER, EnumSet.of(
            Permission.CATEGORY_READ,
            Permission.TICKET_CREATE,
            Permission.TICKET_READ_OWN,
            Permission.TICKET_UPDATE,
            Permission.TICKET_CLOSE,
            Permission.TICKET_REOPEN,
            Permission.TICKET_CANCEL,
            Permission.COMMENT_CREATE_PUBLIC,
            Permission.DASHBOARD_READ_PERSONAL,
            Permission.NOTIFICATION_READ
        ));
    }

    public Set<Permission> resolvePermissions(Role role) {
        return permissionsByRole.getOrDefault(role, EnumSet.noneOf(Permission.class));
    }

    public Set<Role> supportRoles() {
        return Set.of(Role.ADMIN, Role.SUPPORT_MANAGER, Role.SUPPORT_AGENT);
    }

    public Role[] assignableSupportRoles() {
        return Arrays.stream(Role.values())
            .filter(role -> role == Role.SUPPORT_AGENT || role == Role.SUPPORT_MANAGER)
            .toArray(Role[]::new);
    }
}
