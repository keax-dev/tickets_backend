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
    private final TicketCommentPersistenceMapper mapper;

    public JpaTicketCommentRepositoryAdapter(TicketCommentRepository repository) {
        this.repository = repository;
        this.mapper = new TicketCommentPersistenceMapper();
    }

    @Override
    public List<TicketComment> findAllByTicketIdOrderByCreatedAtAsc(String ticketId) {
        return repository.findAllByTicketIdOrderByCreatedAtAsc(ticketId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public TicketComment save(TicketComment comment) {
        TicketCommentEntity entity = comment.id() == null
            ? new TicketCommentEntity()
            : repository.findById(comment.id()).orElseGet(TicketCommentEntity::new);
        return mapper.toDomain(repository.save(mapper.toEntity(comment, entity)));
    }
}
