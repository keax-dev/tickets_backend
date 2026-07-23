package com.tickets.managementtickets.ticket.infrastructure.web.dto;

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

    public static TicketCommentResponse from(com.tickets.managementtickets.ticket.application.result.TicketCommentResponse response) {
        return new TicketCommentResponse(
            response.id(),
            response.ticketId(),
            response.authorId(),
            response.authorName(),
            response.content(),
            response.visibility(),
            response.createdAt(),
            response.updatedAt()
        );
    }
}
