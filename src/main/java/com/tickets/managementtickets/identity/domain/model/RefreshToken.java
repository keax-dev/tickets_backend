package com.tickets.managementtickets.identity.domain.model;

import java.time.Instant;

public record RefreshToken(
    String id,
    String userId,
    String tokenHash,
    Instant expiresAt,
    Instant revokedAt,
    String replacedByTokenId
) {

    public static RefreshToken issue(String userId, String tokenHash, Instant expiresAt) {
        return new RefreshToken(null, userId, tokenHash, expiresAt, null, null);
    }

    public RefreshToken revoke(Instant revokedAt) {
        return new RefreshToken(id, userId, tokenHash, expiresAt, revokedAt, replacedByTokenId);
    }

    public RefreshToken replaceBy(String replacementTokenId, Instant revokedAt) {
        return new RefreshToken(id, userId, tokenHash, expiresAt, revokedAt, replacementTokenId);
    }
}
