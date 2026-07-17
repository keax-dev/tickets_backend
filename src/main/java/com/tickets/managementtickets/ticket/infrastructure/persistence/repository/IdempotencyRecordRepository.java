package com.tickets.managementtickets.ticket.infrastructure.persistence.repository;

import com.tickets.managementtickets.ticket.infrastructure.persistence.entity.IdempotencyRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecordEntity, String> {

    Optional<IdempotencyRecordEntity> findByIdempotencyKeyAndUserId(String idempotencyKey, String userId);

    void deleteByExpiresAtBefore(Instant expiresAt);
}
