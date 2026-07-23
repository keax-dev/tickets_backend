package com.tickets.managementtickets.sla.infrastructure.persistence.adapter;

import com.tickets.managementtickets.sla.application.port.SlaPolicyRepositoryPort;
import com.tickets.managementtickets.sla.domain.model.SlaPolicy;
import com.tickets.managementtickets.sla.infrastructure.persistence.entity.SlaPolicyEntity;
import com.tickets.managementtickets.sla.infrastructure.persistence.repository.SlaPolicyRepository;
import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaSlaPolicyRepositoryAdapter implements SlaPolicyRepositoryPort {

    private final SlaPolicyRepository repository;
    private final SlaPolicyPersistenceMapper mapper;

    public JpaSlaPolicyRepositoryAdapter(SlaPolicyRepository repository) {
        this.repository = repository;
        this.mapper = new SlaPolicyPersistenceMapper();
    }

    @Override
    public List<SlaPolicy> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<SlaPolicy> findByPriority(TicketPriority priority) {
        return repository.findByPriority(priority).map(mapper::toDomain);
    }

    @Override
    public SlaPolicy save(SlaPolicy policy) {
        SlaPolicyEntity entity = policy.id() == null
            ? new SlaPolicyEntity()
            : repository.findById(policy.id()).orElseGet(SlaPolicyEntity::new);
        return mapper.toDomain(repository.save(mapper.toEntity(policy, entity)));
    }
}
