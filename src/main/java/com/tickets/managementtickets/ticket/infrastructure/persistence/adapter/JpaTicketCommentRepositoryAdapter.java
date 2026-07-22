package com.tickets.managementtickets.ticket.infrastructure.persistence.adapter;

import com.tickets.managementtickets.ticket.application.port.TicketCommentRepositoryPort;
import com.tickets.managementtickets.ticket.domain.model.TicketComment;
import com.tickets.managementtickets.ticket.infrastructure.persistence.entity.TicketCommentEntity;
import com.tickets.managementtickets.ticket.infrastructure.persistence.repository.TicketCommentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaTicketCommentRepositoryAdapter implements TicketCommentRepositoryPort {

    private final TicketCommentRepository repository;

    public JpaTicketCommentRepositoryAdapter(TicketCommentRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<TicketComment> findAllByTicketIdOrderByCreatedAtAsc(String ticketId) {
        return repository.findAllByTicketIdOrderByCreatedAtAsc(ticketId).stream().map(this::toDomain).toList();
    }

    @Override
    public TicketComment save(TicketComment comment) {
        return toDomain(repository.save(toEntity(comment)));
    }

    private TicketComment toDomain(TicketCommentEntity entity) {
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

    private TicketCommentEntity toEntity(TicketComment comment) {
        TicketCommentEntity entity = comment.id() == null
            ? new TicketCommentEntity()
            : repository.findById(comment.id()).orElseGet(TicketCommentEntity::new);
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
