package com.tickets.managementtickets.dashboard.application.result;

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
}
