package com.tickets.managementtickets.ticket.infrastructure.persistence.adapter;

import com.tickets.managementtickets.ticket.application.port.TicketHistoryRepositoryPort;
import com.tickets.managementtickets.ticket.application.port.TicketVisibility;
import com.tickets.managementtickets.ticket.domain.model.TicketHistory;
import com.tickets.managementtickets.ticket.infrastructure.persistence.entity.TicketHistoryEntity;
import com.tickets.managementtickets.ticket.infrastructure.persistence.repository.TicketHistoryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaTicketHistoryRepositoryAdapter implements TicketHistoryRepositoryPort {

    private final TicketHistoryRepository repository;
    private final TicketHistoryPersistenceMapper mapper;

    public JpaTicketHistoryRepositoryAdapter(TicketHistoryRepository repository) {
        this.repository = repository;
        this.mapper = new TicketHistoryPersistenceMapper();
    }

    @Override
    public List<TicketHistory> findAllByTicketIdOrderByCreatedAtDesc(String ticketId) {
        return repository.findAllByTicketIdOrderByCreatedAtDesc(ticketId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<TicketHistory> findRecent(TicketVisibility visibility, String currentUserId, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return (switch (visibility) {
            case ALL -> repository.findAllByOrderByCreatedAtDesc(pageRequest);
            case ASSIGNED_OR_UNASSIGNED -> repository.findRecentVisibleForAssignedUser(currentUserId, pageRequest);
            case REQUESTER -> repository.findRecentVisibleForRequester(currentUserId, pageRequest);
        }).stream().map(mapper::toDomain).toList();
    }

    @Override
    public TicketHistory save(TicketHistory history) {
        return mapper.toDomain(repository.save(mapper.toEntity(history)));
    }
}
