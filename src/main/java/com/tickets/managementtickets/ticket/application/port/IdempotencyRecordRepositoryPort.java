package com.tickets.managementtickets.ticket.application.port;

import com.tickets.managementtickets.ticket.domain.model.IdempotencyRecord;

import java.time.Instant;
import java.util.Optional;

public interface IdempotencyRecordRepositoryPort {

    Optional<IdempotencyRecord> findByIdempotencyKeyAndUserId(String idempotencyKey, String userId);

    IdempotencyRecord save(IdempotencyRecord record);

    void deleteByExpiresAtBefore(Instant expiresAt);
}
