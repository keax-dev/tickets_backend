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

    public JpaIdempotencyRecordRepositoryAdapter(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<IdempotencyRecord> findByIdempotencyKeyAndUserId(String idempotencyKey, String userId) {
        return repository.findByIdempotencyKeyAndUserId(idempotencyKey, userId).map(this::toDomain);
    }

    @Override
    public IdempotencyRecord save(IdempotencyRecord record) {
        return toDomain(repository.save(toEntity(record)));
    }

    @Override
    public void deleteByExpiresAtBefore(Instant expiresAt) {
        repository.deleteByExpiresAtBefore(expiresAt);
    }

    private IdempotencyRecord toDomain(IdempotencyRecordEntity entity) {
        return new IdempotencyRecord(
            entity.getId(),
            entity.getIdempotencyKey(),
            entity.getUserId(),
            entity.getRequestHash(),
            entity.getResponseStatus(),
            entity.getResponseBody(),
            entity.getResourceId(),
            entity.getCreatedAt(),
            entity.getExpiresAt()
        );
    }

    private IdempotencyRecordEntity toEntity(IdempotencyRecord record) {
        IdempotencyRecordEntity entity = new IdempotencyRecordEntity();
        entity.setIdempotencyKey(record.idempotencyKey());
        entity.setUserId(record.userId());
        entity.setRequestHash(record.requestHash());
        entity.setResponseStatus(record.responseStatus());
        entity.setResponseBody(record.responseBody());
        entity.setResourceId(record.resourceId());
        entity.setExpiresAt(record.expiresAt());
        return entity;
    }
}
