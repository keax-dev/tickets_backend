package com.tickets.managementtickets.category.application.service;

import com.tickets.managementtickets.category.application.command.StatusUpdateRequest;
import com.tickets.managementtickets.category.application.command.UpsertCategoryRequest;
import com.tickets.managementtickets.category.application.port.CategoryRepositoryPort;
import com.tickets.managementtickets.category.application.result.CategoryResponse;
import com.tickets.managementtickets.category.domain.model.Category;
import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.application.service.AuthorizationService;
import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.shared.application.exception.ConflictException;
import com.tickets.managementtickets.shared.application.exception.NotFoundException;
import com.tickets.managementtickets.shared.application.port.TransactionRunner;

import java.util.List;

public class CategoryService {

    private final CategoryRepositoryPort categoryRepository;
    private final AuthorizationService authorizationService;
    private final TransactionRunner transactionRunner;
    private final CategoryResponseMapper responseMapper;

    public CategoryService(
        CategoryRepositoryPort categoryRepository,
        AuthorizationService authorizationService,
        TransactionRunner transactionRunner
    ) {
        this.categoryRepository = categoryRepository;
        this.authorizationService = authorizationService;
        this.transactionRunner = transactionRunner;
        this.responseMapper = new CategoryResponseMapper();
    }

    public List<CategoryResponse> list(AuthenticatedUser currentUser) {
        return transactionRunner.readOnly(() -> {
            authorizationService.requirePermission(currentUser, Permission.CATEGORY_READ);
            return categoryRepository.findAll()
                .stream()
                .filter(category -> currentUser.hasPermission(Permission.CATEGORY_UPDATE) || currentUser.hasPermission(Permission.CATEGORY_DISABLE) || category.active())
                .map(responseMapper::toResponse)
                .toList();
        });
    }

    public CategoryResponse create(AuthenticatedUser currentUser, UpsertCategoryRequest request) {
        return transactionRunner.required(() -> {
            authorizationService.requirePermission(currentUser, Permission.CATEGORY_CREATE);
            ensureUniqueName(request.name(), null);

            Category category = Category.create(normalizeName(request.name()), request.description());
            return responseMapper.toResponse(categoryRepository.save(category));
        });
    }

    public CategoryResponse update(AuthenticatedUser currentUser, String categoryId, UpsertCategoryRequest request) {
        return transactionRunner.required(() -> {
            authorizationService.requirePermission(currentUser, Permission.CATEGORY_UPDATE);
            Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "The category could not be found."));
            ensureVersion(category.version(), request.version(), "The category was modified by another request.");

            ensureUniqueName(request.name(), categoryId);
            return responseMapper.toResponse(categoryRepository.save(category.update(normalizeName(request.name()), request.description())));
        });
    }

    public CategoryResponse updateStatus(AuthenticatedUser currentUser, String categoryId, StatusUpdateRequest request) {
        return transactionRunner.required(() -> {
            authorizationService.requirePermission(currentUser, Permission.CATEGORY_DISABLE);
            Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "The category could not be found."));
            ensureVersion(category.version(), request.version(), "The category was modified by another request.");
            return responseMapper.toResponse(categoryRepository.save(category.withActive(request.active())));
        });
    }

    private void ensureUniqueName(String name, String currentCategoryId) {
        categoryRepository.findByNameIgnoreCase(normalizeName(name))
            .filter(category -> !category.id().equals(currentCategoryId))
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

}
