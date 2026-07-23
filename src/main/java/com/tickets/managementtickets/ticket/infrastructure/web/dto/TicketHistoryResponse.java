package com.tickets.managementtickets.ticket.infrastructure.web.dto;

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

    public static TicketHistoryResponse from(com.tickets.managementtickets.ticket.application.result.TicketHistoryResponse response) {
        return new TicketHistoryResponse(
            response.id(),
            response.action(),
            response.performedBy(),
            response.performedByName(),
            response.previousValue(),
            response.newValue(),
            response.metadataJson(),
            response.createdAt()
        );
    }
}
