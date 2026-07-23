package com.tickets.managementtickets.ticket.infrastructure.persistence.adapter;

import com.tickets.managementtickets.ticket.domain.model.IdempotencyRecord;
import com.tickets.managementtickets.ticket.infrastructure.persistence.entity.IdempotencyRecordEntity;

final class IdempotencyRecordPersistenceMapper {

    IdempotencyRecord toDomain(IdempotencyRecordEntity entity) {
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

    IdempotencyRecordEntity toEntity(IdempotencyRecord record) {
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
