package com.tickets.managementtickets.category.application.result;

import java.time.Instant;

public record CategoryResponse(
    String id,
    String name,
    String description,
    boolean active,
    long version,
    Instant createdAt,
    Instant updatedAt
) {
}
