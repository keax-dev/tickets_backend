package com.tickets.managementtickets.ticket.application.port;

import com.tickets.managementtickets.shared.application.model.SortDirection;
import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;

import java.time.Instant;

public record TicketQuery(
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
    SortDirection direction,
    TicketVisibility visibility,
    String currentUserId
) {
}
