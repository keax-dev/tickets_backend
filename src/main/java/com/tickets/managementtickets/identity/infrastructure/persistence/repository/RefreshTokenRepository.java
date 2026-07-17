package com.tickets.managementtickets.identity.infrastructure.persistence.repository;

import com.tickets.managementtickets.identity.infrastructure.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, String> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    void deleteByExpiresAtBefore(Instant expiresAt);
}
