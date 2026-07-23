package com.tickets.managementtickets.dashboard.application.result;

import com.tickets.managementtickets.ticket.domain.model.TicketHistoryAction;

import java.time.Instant;

public record RecentActivityResponse(
    String id,
    String ticketId,
    TicketHistoryAction action,
    String performedByName,
    Instant createdAt
) {
}
