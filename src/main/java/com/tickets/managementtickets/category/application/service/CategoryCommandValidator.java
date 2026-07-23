package com.tickets.managementtickets.category.application.service;

import com.tickets.managementtickets.category.application.port.CategoryRepositoryPort;
import com.tickets.managementtickets.category.domain.model.Category;
import com.tickets.managementtickets.shared.application.exception.ConflictException;
import com.tickets.managementtickets.shared.application.service.ResourceVersionPolicy;

final class CategoryCommandValidator {

    private final CategoryRepositoryPort categoryRepository;

    CategoryCommandValidator(CategoryRepositoryPort categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    void ensureUniqueName(String name, String currentCategoryId) {
        categoryRepository.findByNameIgnoreCase(normalizeName(name))
            .filter(category -> !category.id().equals(currentCategoryId))
            .ifPresent(existing -> {
                throw new ConflictException("CATEGORY_NAME_ALREADY_EXISTS", "A category with the same name already exists.");
            });
    }

    void ensureVersion(Category category, long requestedVersion) {
        ResourceVersionPolicy.ensureCurrent(category.version(), requestedVersion, "The category was modified by another request.");
    }

    String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }
}
