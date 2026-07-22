package com.tickets.managementtickets.identity.application.port;

import com.tickets.managementtickets.identity.domain.model.RefreshToken;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepositoryPort {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    RefreshToken save(RefreshToken refreshToken);

    void deleteByExpiresAtBefore(Instant expiresAt);
}
