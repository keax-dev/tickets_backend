package com.tickets.managementtickets.dashboard.application.service;

import com.tickets.managementtickets.dashboard.application.result.DashboardSummaryResponse;
import com.tickets.managementtickets.dashboard.application.result.RecentActivityResponse;
import com.tickets.managementtickets.identity.domain.model.User;
import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import com.tickets.managementtickets.ticket.domain.model.TicketHistory;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;

import java.util.Map;

final class DashboardResponseMapper {

    DashboardSummaryResponse toSummaryResponse(
        long activeTickets,
        long createdToday,
        long unassignedTickets,
        long breachedTickets,
        long dueSoonTickets,
        long assignedToCurrentUser,
        Map<TicketStatus, Long> byStatus,
        Map<TicketPriority, Long> byPriority
    ) {
        return new DashboardSummaryResponse(
            activeTickets,
            createdToday,
            unassignedTickets,
            breachedTickets,
            dueSoonTickets,
            assignedToCurrentUser,
            byStatus,
            byPriority
        );
    }

    RecentActivityResponse toRecentActivityResponse(TicketHistory entry, Map<String, User> usersById) {
        User performer = usersById.get(entry.performedBy());
        return new RecentActivityResponse(
            entry.id(),
            entry.ticketId(),
            entry.action(),
            performer == null ? null : performer.displayName(),
            entry.createdAt()
        );
    }
}
