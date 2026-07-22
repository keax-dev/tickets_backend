package com.tickets.managementtickets.ticket.domain.model;

import java.time.Instant;

public record TicketHistory(
    String id,
    String ticketId,
    TicketHistoryAction action,
    String performedBy,
    String previousValue,
    String newValue,
    String metadataJson,
    Instant createdAt
) {

    public static TicketHistory create(
        String ticketId,
        TicketHistoryAction action,
        String performedBy,
        String previousValue,
        String newValue,
        String metadataJson
    ) {
        return new TicketHistory(null, ticketId, action, performedBy, previousValue, newValue, metadataJson, null);
    }
}
