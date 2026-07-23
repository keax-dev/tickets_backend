package com.tickets.managementtickets.ticket.application.result;

import com.tickets.managementtickets.ticket.domain.model.CommentVisibility;

import java.time.Instant;

public record TicketCommentResponse(
    String id,
    String ticketId,
    String authorId,
    String authorName,
    String content,
    CommentVisibility visibility,
    Instant createdAt,
    Instant updatedAt
) {
}
