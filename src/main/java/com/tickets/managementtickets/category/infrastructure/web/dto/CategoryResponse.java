package com.tickets.managementtickets.category.infrastructure.web.dto;

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

    public static CategoryResponse from(com.tickets.managementtickets.category.application.result.CategoryResponse response) {
        return new CategoryResponse(
            response.id(),
            response.name(),
            response.description(),
            response.active(),
            response.version(),
            response.createdAt(),
            response.updatedAt()
        );
    }
}
