package com.tickets.managementtickets.ticket.application.result;

import com.tickets.managementtickets.ticket.domain.model.TicketHistoryAction;

import java.time.Instant;

public record TicketHistoryResponse(
    String id,
    TicketHistoryAction action,
    String performedBy,
    String performedByName,
    String previousValue,
    String newValue,
    String metadataJson,
    Instant createdAt
) {
}
