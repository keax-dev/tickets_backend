package com.tickets.managementtickets.ticket.domain.model;

import java.time.Instant;

public record TicketComment(
    String id,
    String ticketId,
    String authorId,
    String content,
    CommentVisibility visibility,
    Instant createdAt,
    Instant updatedAt
) {

    public static TicketComment create(
        String ticketId,
        String authorId,
        String content,
        CommentVisibility visibility
    ) {
        return new TicketComment(null, ticketId, authorId, content, visibility, null, null);
    }
}
