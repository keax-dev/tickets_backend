package com.tickets.managementtickets.category.domain.model;

import java.time.Instant;

public record Category(
    String id,
    String name,
    String description,
    boolean active,
    long version,
    Instant createdAt,
    Instant updatedAt
) {

    public static Category create(String name, String description) {
        return new Category(null, name, description, true, 0, null, null);
    }

    public Category update(String name, String description) {
        return new Category(id, name, description, active, version, createdAt, updatedAt);
    }

    public Category withActive(boolean active) {
        return new Category(id, name, description, active, version, createdAt, updatedAt);
    }
}
