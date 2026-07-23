package com.tickets.managementtickets.category.application.service;

import com.tickets.managementtickets.category.application.result.CategoryResponse;
import com.tickets.managementtickets.category.domain.model.Category;

final class CategoryResponseMapper {

    CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
            category.id(),
            category.name(),
            category.description(),
            category.active(),
            category.version(),
            category.createdAt(),
            category.updatedAt()
        );
    }
}
