package com.tickets.managementtickets.category.application.service;

import com.tickets.managementtickets.category.application.command.StatusUpdateRequest;
import com.tickets.managementtickets.category.application.command.UpsertCategoryRequest;
import com.tickets.managementtickets.category.application.port.CategoryRepositoryPort;
import com.tickets.managementtickets.category.application.result.CategoryResponse;
import com.tickets.managementtickets.category.domain.model.Category;
import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.application.service.AuthorizationService;
import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.shared.application.exception.NotFoundException;
import com.tickets.managementtickets.shared.application.port.TransactionRunner;

import java.util.List;

public class CategoryService {

    private final CategoryRepositoryPort categoryRepository;
    private final AuthorizationService authorizationService;
    private final TransactionRunner transactionRunner;
    private final CategoryResponseMapper responseMapper;
    private final CategoryCommandValidator commandValidator;

    public CategoryService(
        CategoryRepositoryPort categoryRepository,
        AuthorizationService authorizationService,
        TransactionRunner transactionRunner
    ) {
        this.categoryRepository = categoryRepository;
        this.authorizationService = authorizationService;
        this.transactionRunner = transactionRunner;
        this.responseMapper = new CategoryResponseMapper();
        this.commandValidator = new CategoryCommandValidator(categoryRepository);
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
            commandValidator.ensureUniqueName(request.name(), null);

            Category category = Category.create(commandValidator.normalizeName(request.name()), request.description());
            return responseMapper.toResponse(categoryRepository.save(category));
        });
    }

    public CategoryResponse update(AuthenticatedUser currentUser, String categoryId, UpsertCategoryRequest request) {
        return transactionRunner.required(() -> {
            authorizationService.requirePermission(currentUser, Permission.CATEGORY_UPDATE);
            Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "The category could not be found."));
            commandValidator.ensureVersion(category, request.version());

            commandValidator.ensureUniqueName(request.name(), categoryId);
            return responseMapper.toResponse(categoryRepository.save(category.update(commandValidator.normalizeName(request.name()), request.description())));
        });
    }

    public CategoryResponse updateStatus(AuthenticatedUser currentUser, String categoryId, StatusUpdateRequest request) {
        return transactionRunner.required(() -> {
            authorizationService.requirePermission(currentUser, Permission.CATEGORY_DISABLE);
            Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "The category could not be found."));
            commandValidator.ensureVersion(category, request.version());
            return responseMapper.toResponse(categoryRepository.save(category.withActive(request.active())));
        });
    }

}
