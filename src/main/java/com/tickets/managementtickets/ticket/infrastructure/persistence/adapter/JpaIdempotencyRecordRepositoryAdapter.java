package com.tickets.managementtickets.ticket.infrastructure.persistence.adapter;

import com.tickets.managementtickets.ticket.application.port.IdempotencyRecordRepositoryPort;
import com.tickets.managementtickets.ticket.domain.model.IdempotencyRecord;
import com.tickets.managementtickets.ticket.infrastructure.persistence.entity.IdempotencyRecordEntity;
import com.tickets.managementtickets.ticket.infrastructure.persistence.repository.IdempotencyRecordRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class JpaIdempotencyRecordRepositoryAdapter implements IdempotencyRecordRepositoryPort {

    private final IdempotencyRecordRepository repository;
    private final IdempotencyRecordPersistenceMapper mapper;

    public JpaIdempotencyRecordRepositoryAdapter(IdempotencyRecordRepository repository) {
        this.repository = repository;
        this.mapper = new IdempotencyRecordPersistenceMapper();
    }

    @Override
    public Optional<IdempotencyRecord> findByIdempotencyKeyAndUserId(String idempotencyKey, String userId) {
        return repository.findByIdempotencyKeyAndUserId(idempotencyKey, userId).map(mapper::toDomain);
    }

    @Override
    public IdempotencyRecord save(IdempotencyRecord record) {
        return mapper.toDomain(repository.save(mapper.toEntity(record)));
    }

    @Override
    public void deleteByExpiresAtBefore(Instant expiresAt) {
        repository.deleteByExpiresAtBefore(expiresAt);
    }

}
