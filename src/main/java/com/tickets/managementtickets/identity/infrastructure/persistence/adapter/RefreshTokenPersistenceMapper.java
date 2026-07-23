package com.tickets.managementtickets.identity.infrastructure.persistence.adapter;

import com.tickets.managementtickets.identity.domain.model.RefreshToken;
import com.tickets.managementtickets.identity.infrastructure.persistence.entity.RefreshTokenEntity;

final class RefreshTokenPersistenceMapper {

    RefreshToken toDomain(RefreshTokenEntity entity) {
        return new RefreshToken(
            entity.getId(),
            entity.getUserId(),
            entity.getTokenHash(),
            entity.getExpiresAt(),
            entity.getRevokedAt(),
            entity.getReplacedByTokenId()
        );
    }

    RefreshTokenEntity toEntity(RefreshToken refreshToken, RefreshTokenEntity entity) {
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
