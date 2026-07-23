package com.tickets.managementtickets.category.infrastructure.persistence.adapter;

import com.tickets.managementtickets.category.domain.model.Category;
import com.tickets.managementtickets.category.infrastructure.persistence.entity.CategoryEntity;

final class CategoryPersistenceMapper {

    Category toDomain(CategoryEntity entity) {
        return new Category(
            entity.getId(),
            entity.getName(),
            entity.getDescription(),
            entity.isActive(),
            entity.getVersion(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    CategoryEntity toEntity(Category category, CategoryEntity entity) {
        if (category.id() != null) {
            entity.setId(category.id());
        }
        entity.setName(category.name());
        entity.setDescription(category.description());
        entity.setActive(category.active());
        return entity;
    }
}
