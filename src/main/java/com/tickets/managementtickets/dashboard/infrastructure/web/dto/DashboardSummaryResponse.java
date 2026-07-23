package com.tickets.managementtickets.dashboard.infrastructure.web.dto;

import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;

import java.util.Map;

public record DashboardSummaryResponse(
    long activeTickets,
    long createdToday,
    long unassignedTickets,
    long breachedTickets,
    long dueSoonTickets,
    long assignedToCurrentUser,
    Map<TicketStatus, Long> ticketsByStatus,
    Map<TicketPriority, Long> ticketsByPriority
) {

    public static DashboardSummaryResponse from(com.tickets.managementtickets.dashboard.application.result.DashboardSummaryResponse response) {
        return new DashboardSummaryResponse(
            response.activeTickets(),
            response.createdToday(),
            response.unassignedTickets(),
            response.breachedTickets(),
            response.dueSoonTickets(),
            response.assignedToCurrentUser(),
            response.ticketsByStatus(),
            response.ticketsByPriority()
        );
    }
}
