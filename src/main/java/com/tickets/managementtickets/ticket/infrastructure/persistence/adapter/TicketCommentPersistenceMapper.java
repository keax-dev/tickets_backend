package com.tickets.managementtickets.ticket.infrastructure.persistence.adapter;

import com.tickets.managementtickets.ticket.domain.model.TicketComment;
import com.tickets.managementtickets.ticket.infrastructure.persistence.entity.TicketCommentEntity;

final class TicketCommentPersistenceMapper {

    TicketComment toDomain(TicketCommentEntity entity) {
        return new TicketComment(
            entity.getId(),
            entity.getTicketId(),
            entity.getAuthorId(),
            entity.getContent(),
            entity.getVisibility(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    TicketCommentEntity toEntity(TicketComment comment, TicketCommentEntity entity) {
        if (comment.id() != null) {
            entity.setId(comment.id());
        }
        entity.setTicketId(comment.ticketId());
        entity.setAuthorId(comment.authorId());
        entity.setContent(comment.content());
        entity.setVisibility(comment.visibility());
        return entity;
    }
}
