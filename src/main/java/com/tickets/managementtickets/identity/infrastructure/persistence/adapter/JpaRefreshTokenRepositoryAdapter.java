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

    public JpaRefreshTokenRepositoryAdapter(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        return toDomain(repository.save(toEntity(refreshToken)));
    }

    @Override
    public void deleteByExpiresAtBefore(Instant expiresAt) {
        repository.deleteByExpiresAtBefore(expiresAt);
    }

    private RefreshToken toDomain(RefreshTokenEntity entity) {
        return new RefreshToken(
            entity.getId(),
            entity.getUserId(),
            entity.getTokenHash(),
            entity.getExpiresAt(),
            entity.getRevokedAt(),
            entity.getReplacedByTokenId()
        );
    }

    private RefreshTokenEntity toEntity(RefreshToken refreshToken) {
        RefreshTokenEntity entity = refreshToken.id() == null
            ? new RefreshTokenEntity()
            : repository.findById(refreshToken.id()).orElseGet(RefreshTokenEntity::new);
        if (refreshToken.id() != null) {
            entity.setId(refreshToken.id());
        }
        entity.setUserId(refreshToken.userId());
        entity.setTokenHash(refreshToken.tokenHash());
        entity.setExpiresAt(refreshToken.expiresAt());
        entity.setRevokedAt(refreshToken.revokedAt());
        entity.setReplacedByTokenId(refreshToken.replacedByTokenId());
        return entity;
    }
}
