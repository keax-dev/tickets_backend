package com.tickets.managementtickets.category.application.service;

import com.tickets.managementtickets.category.infrastructure.persistence.entity.CategoryEntity;
import com.tickets.managementtickets.category.infrastructure.persistence.repository.CategoryRepository;
import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.application.service.AuthorizationService;
import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.shared.application.exception.ConflictException;
import com.tickets.managementtickets.shared.application.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AuthorizationService authorizationService;

    public CategoryService(CategoryRepository categoryRepository, AuthorizationService authorizationService) {
        this.categoryRepository = categoryRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(AuthenticatedUser currentUser) {
        return categoryRepository.findAll()
            .stream()
            .filter(category -> currentUser.hasPermission(Permission.CATEGORY_UPDATE) || currentUser.hasPermission(Permission.CATEGORY_DISABLE) || category.isActive())
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public CategoryResponse create(AuthenticatedUser currentUser, UpsertCategoryRequest request) {
        authorizationService.requirePermission(currentUser, Permission.CATEGORY_CREATE);
        ensureUniqueName(request.name(), null);

        CategoryEntity category = new CategoryEntity();
        category.setName(normalizeName(request.name()));
        category.setDescription(request.description());
        category.setActive(true);
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(AuthenticatedUser currentUser, String categoryId, UpsertCategoryRequest request) {
        authorizationService.requirePermission(currentUser, Permission.CATEGORY_UPDATE);
        CategoryEntity category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "The category could not be found."));
        ensureVersion(category.getVersion(), request.version(), "The category was modified by another request.");

        ensureUniqueName(request.name(), categoryId);
        category.setName(normalizeName(request.name()));
        category.setDescription(request.description());
        return toResponse(category);
    }

    @Transactional
    public CategoryResponse updateStatus(AuthenticatedUser currentUser, String categoryId, StatusUpdateRequest request) {
        authorizationService.requirePermission(currentUser, Permission.CATEGORY_DISABLE);
        CategoryEntity category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "The category could not be found."));
        ensureVersion(category.getVersion(), request.version(), "The category was modified by another request.");
        category.setActive(request.active());
        return toResponse(category);
    }

    private void ensureUniqueName(String name, String currentCategoryId) {
        categoryRepository.findByNameIgnoreCase(normalizeName(name))
            .filter(category -> !category.getId().equals(currentCategoryId))
            .ifPresent(existing -> {
                throw new ConflictException("CATEGORY_NAME_ALREADY_EXISTS", "A category with the same name already exists.");
            });
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }

    private void ensureVersion(long currentVersion, long requestedVersion, String message) {
        if (currentVersion != requestedVersion) {
            throw new ConflictException("RESOURCE_VERSION_CONFLICT", message);
        }
    }

    private CategoryResponse toResponse(CategoryEntity category) {
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getDescription(),
            category.isActive(),
            category.getVersion(),
            category.getCreatedAt(),
            category.getUpdatedAt()
        );
    }

    public record UpsertCategoryRequest(long version, String name, String description) {
    }

    public record StatusUpdateRequest(long version, boolean active) {
    }

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
}
