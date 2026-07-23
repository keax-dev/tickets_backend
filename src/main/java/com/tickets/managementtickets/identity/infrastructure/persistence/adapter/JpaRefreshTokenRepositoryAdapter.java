package com.tickets.managementtickets.identity.infrastructure.persistence.adapter;

import com.tickets.managementtickets.identity.application.port.RefreshTokenRepositoryPort;
import com.tickets.managementtickets.identity.domain.model.RefreshToken;
import com.tickets.managementtickets.identity.infrastructure.persistence.entity.RefreshTokenEntity;
import com.tickets.managementtickets.identity.infrastructure.persistence.repository.RefreshTokenRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class JpaRefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenRepository repository;
    private final RefreshTokenPersistenceMapper mapper;

    public JpaRefreshTokenRepositoryAdapter(RefreshTokenRepository repository) {
        this.repository = repository;
        this.mapper = new RefreshTokenPersistenceMapper();
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenEntity entity = refreshToken.id() == null
            ? new RefreshTokenEntity()
            : repository.findById(refreshToken.id()).orElseGet(RefreshTokenEntity::new);
        return mapper.toDomain(repository.save(mapper.toEntity(refreshToken, entity)));
    }

    @Override
    public void deleteByExpiresAtBefore(Instant expiresAt) {
        repository.deleteByExpiresAtBefore(expiresAt);
    }

}
