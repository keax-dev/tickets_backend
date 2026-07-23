package com.tickets.managementtickets.ticket.application.service;

import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.ticket.application.port.TicketVisibility;

public final class TicketVisibilityPolicy {

    public TicketVisibility resolveFor(AuthenticatedUser currentUser) {
        if (currentUser.hasPermission(Permission.TICKET_READ_ALL)) {
            return TicketVisibility.ALL;
        }
        if (currentUser.hasPermission(Permission.TICKET_READ_ASSIGNED)) {
            return TicketVisibility.ASSIGNED_OR_UNASSIGNED;
        }
        return TicketVisibility.REQUESTER;
    }
}
