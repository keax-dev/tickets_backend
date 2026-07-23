package com.tickets.managementtickets.ticket.application.service;

import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.identity.domain.model.Role;
import com.tickets.managementtickets.ticket.application.port.TicketVisibility;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

// This test suite verifies permission-to-ticket-visibility resolution shared by ticket and dashboard flows.
class TicketVisibilityPolicyTest {

    // Use the real policy because the mapping from permissions to visibility is pure logic.
    private final TicketVisibilityPolicy visibilityPolicy = new TicketVisibilityPolicy();

    @Test
    void shouldResolveGlobalVisibilityForReadAllPermission() {
        // Arrange: create a user with global ticket read permission.
        AuthenticatedUser user = user(Permission.TICKET_READ_ALL);

        // Act: resolve the visibility scope.
        TicketVisibility visibility = visibilityPolicy.resolveFor(user);

        // Assert: verify global permission maps to all tickets.
        assertEquals(TicketVisibility.ALL, visibility);
    }

    @Test
    void shouldResolveAssignedVisibilityWhenUserCanReadAssignedTickets() {
        // Arrange: create a user with assigned ticket read permission.
        AuthenticatedUser user = user(Permission.TICKET_READ_ASSIGNED);

        // Act: resolve the visibility scope.
        TicketVisibility visibility = visibilityPolicy.resolveFor(user);

        // Assert: verify assigned permission maps to assigned-or-unassigned tickets.
        assertEquals(TicketVisibility.ASSIGNED_OR_UNASSIGNED, visibility);
    }

    @Test
    void shouldResolveRequesterVisibilityByDefault() {
        // Arrange: create a user with only requester-level ticket read permission.
        AuthenticatedUser user = user(Permission.TICKET_READ_OWN);

        // Act: resolve the visibility scope.
        TicketVisibility visibility = visibilityPolicy.resolveFor(user);

        // Assert: verify the fallback visibility is requester-owned tickets.
        assertEquals(TicketVisibility.REQUESTER, visibility);
    }

    private AuthenticatedUser user(Permission permission) {
        // Build a small authenticated principal containing one permission at a time,
        // which makes each test isolate a single branch of the visibility policy.
        return new AuthenticatedUser(
            "user-1",
            "user@test.com",
            "User",
            "Test",
            Role.CUSTOMER,
            Set.of(permission)
        );
    }
}
