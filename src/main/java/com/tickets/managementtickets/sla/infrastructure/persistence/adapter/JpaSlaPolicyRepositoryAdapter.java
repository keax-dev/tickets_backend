package com.tickets.managementtickets.sla.infrastructure.persistence.adapter;

import com.tickets.managementtickets.sla.application.port.SlaPolicyRepositoryPort;
import com.tickets.managementtickets.sla.domain.model.SlaPolicy;
import com.tickets.managementtickets.sla.infrastructure.persistence.entity.SlaPolicyEntity;
import com.tickets.managementtickets.sla.infrastructure.persistence.repository.SlaPolicyRepository;
import com.tickets.managementtickets.ticket.domain.model.TicketPriority;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaSlaPolicyRepositoryAdapter implements SlaPolicyRepositoryPort {

    private final SlaPolicyRepository repository;

    public JpaSlaPolicyRepositoryAdapter(SlaPolicyRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<SlaPolicy> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<SlaPolicy> findByPriority(TicketPriority priority) {
        return repository.findByPriority(priority).map(this::toDomain);
    }

    @Override
    public SlaPolicy save(SlaPolicy policy) {
        return toDomain(repository.save(toEntity(policy)));
    }

    private SlaPolicy toDomain(SlaPolicyEntity entity) {
        return new SlaPolicy(
            entity.getId(),
            entity.getPriority(),
            entity.getFirstResponseHours(),
            entity.getResolutionHours(),
            entity.isActive(),
            entity.getVersion()
        );
    }

    private SlaPolicyEntity toEntity(SlaPolicy policy) {
        SlaPolicyEntity entity = policy.id() == null
            ? new SlaPolicyEntity()
            : repository.findById(policy.id()).orElseGet(SlaPolicyEntity::new);
        if (policy.id() != null) {
            entity.setId(policy.id());
        }
        entity.setPriority(policy.priority());
        entity.setFirstResponseHours(policy.firstResponseHours());
        entity.setResolutionHours(policy.resolutionHours());
        entity.setActive(policy.active());
        return entity;
    }
}
