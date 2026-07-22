package com.tickets.managementtickets.ticket.domain.model;

import java.time.Instant;

public record IdempotencyRecord(
    String id,
    String idempotencyKey,
    String userId,
    String requestHash,
    int responseStatus,
    String responseBody,
    String resourceId,
    Instant createdAt,
    Instant expiresAt
) {

    public static IdempotencyRecord create(
        String idempotencyKey,
        String userId,
        String requestHash,
        int responseStatus,
        String responseBody,
        String resourceId,
        Instant expiresAt
    ) {
        return new IdempotencyRecord(null, idempotencyKey, userId, requestHash, responseStatus, responseBody, resourceId, null, expiresAt);
    }
}
