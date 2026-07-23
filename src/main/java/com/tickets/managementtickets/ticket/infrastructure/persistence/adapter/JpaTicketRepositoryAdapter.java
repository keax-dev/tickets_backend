package com.tickets.managementtickets.ticket.infrastructure.persistence.adapter;

import com.tickets.managementtickets.shared.application.model.PageResponse;
import com.tickets.managementtickets.shared.application.model.SortDirection;
import com.tickets.managementtickets.ticket.application.port.TicketCountQuery;
import com.tickets.managementtickets.ticket.application.port.TicketQuery;
import com.tickets.managementtickets.ticket.application.port.TicketRepositoryPort;
import com.tickets.managementtickets.ticket.application.port.TicketVisibility;
import com.tickets.managementtickets.ticket.domain.model.Ticket;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;
import com.tickets.managementtickets.ticket.infrastructure.persistence.entity.TicketEntity;
import com.tickets.managementtickets.ticket.infrastructure.persistence.repository.TicketRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class JpaTicketRepositoryAdapter implements TicketRepositoryPort {

    private final TicketRepository repository;
    private final TicketPersistenceMapper mapper;

    public JpaTicketRepositoryAdapter(TicketRepository repository) {
        this.repository = repository;
        this.mapper = new TicketPersistenceMapper();
    }

    @Override
    public PageResponse<Ticket> findAll(TicketQuery query) {
        Page<Ticket> page = repository.findAll(
            buildSpecification(query),
            PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(toSpringDirection(query.direction()), query.sortBy())
            )
        ).map(mapper::toDomain);

        return PageResponse.of(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.getSort().stream()
                .map(order -> order.getProperty() + "," + order.getDirection().name().toLowerCase())
                .toList()
        );
    }

    @Override
    public Optional<Ticket> findById(String id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Ticket save(Ticket ticket) {
        TicketEntity entity = ticket.getId() == null
            ? new TicketEntity()
            : repository.findById(ticket.getId()).orElseGet(TicketEntity::new);
        return mapper.toDomain(repository.save(mapper.toEntity(ticket, entity)));
    }

    @Override
    public long count(TicketCountQuery query) {
        return repository.count(buildSpecification(query));
    }

    @Override
    public List<Ticket> findAllByStatusAndResolvedAtBefore(TicketStatus status, Instant resolvedAt) {
        return repository.findAllByStatusAndResolvedAtBefore(status, resolvedAt).stream().map(mapper::toDomain).toList();
    }

    private Specification<TicketEntity> buildSpecification(TicketQuery query) {
        Specification<TicketEntity> specification = accessSpecification(query.visibility(), query.currentUserId());
        if (query.search() != null && !query.search().isBlank()) {
            String likeExpression = "%" + query.search().trim().toLowerCase() + "%";
            specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), likeExpression),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), likeExpression),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likeExpression)
            ));
        }
        if (query.status() != null) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), query.status()));
        }
        if (query.priority() != null) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("priority"), query.priority()));
        }
        if (query.categoryId() != null && !query.categoryId().isBlank()) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("categoryId"), query.categoryId()));
        }
        if (query.assignedAgentId() != null && !query.assignedAgentId().isBlank()) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("assignedAgentId"), query.assignedAgentId()));
        }
        if (query.createdFrom() != null) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), query.createdFrom()));
        }
        if (query.createdTo() != null) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), query.createdTo()));
        }
        return specification;
    }

    private Specification<TicketEntity> buildSpecification(TicketCountQuery query) {
        Specification<TicketEntity> specification = accessSpecification(query.visibility(), query.currentUserId());
        if (query.status() != null) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), query.status()));
        }
        if (query.priority() != null) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("priority"), query.priority()));
        }
        if (query.excludeTerminalStatuses()) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.not(root.get("status").in(TicketStatus.CLOSED, TicketStatus.CANCELLED)));
        }
        if (query.createdAtFrom() != null) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), query.createdAtFrom()));
        }
        if (query.onlyUnassigned()) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.isNull(root.get("assignedAgentId")));
        }
        if (query.onlyBreachedSla()) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.isTrue(root.get("slaFirstResponseBreached")),
                criteriaBuilder.isTrue(root.get("slaResolutionBreached"))
            ));
        }
        if (query.resolutionDueAfter() != null) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.greaterThan(root.get("resolutionDueAt"), query.resolutionDueAfter()));
        }
        if (query.resolutionDueAtOrBefore() != null) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("resolutionDueAt"), query.resolutionDueAtOrBefore()));
        }
        if (query.assignedAgentId() != null) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("assignedAgentId"), query.assignedAgentId()));
        }
        return specification;
    }

    private Specification<TicketEntity> accessSpecification(TicketVisibility visibility, String currentUserId) {
        return switch (visibility) {
            case ALL -> (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.conjunction();
            case ASSIGNED_OR_UNASSIGNED -> (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.equal(root.get("assignedAgentId"), currentUserId),
                criteriaBuilder.isNull(root.get("assignedAgentId"))
            );
            case REQUESTER -> (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("requesterId"), currentUserId);
        };
    }

    private Sort.Direction toSpringDirection(SortDirection direction) {
        return direction == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

}
