package com.tickets.managementtickets.ticket.application.query;

import com.tickets.managementtickets.shared.application.model.SortDirection;
import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;

import java.time.Instant;

public record TicketFilterRequest(
    String search,
    TicketStatus status,
    TicketPriority priority,
    String categoryId,
    String assignedAgentId,
    Instant createdFrom,
    Instant createdTo,
    int page,
    int size,
    String sortBy,
    SortDirection direction
) {
}
