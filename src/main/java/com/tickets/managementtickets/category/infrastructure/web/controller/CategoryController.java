package com.tickets.managementtickets.category.infrastructure.web.controller;

import com.tickets.managementtickets.category.application.command.StatusUpdateRequest;
import com.tickets.managementtickets.category.application.command.UpsertCategoryRequest;
import com.tickets.managementtickets.category.application.service.CategoryService;
import com.tickets.managementtickets.category.infrastructure.web.dto.CategoryRequest;
import com.tickets.managementtickets.category.infrastructure.web.dto.CategoryResponse;
import com.tickets.managementtickets.category.infrastructure.web.dto.StatusRequest;
import com.tickets.managementtickets.identity.application.port.CurrentAuthenticatedUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Category catalog administration endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;
    private final CurrentAuthenticatedUserProvider currentUserProvider;

    public CategoryController(CategoryService categoryService, CurrentAuthenticatedUserProvider currentUserProvider) {
        this.categoryService = categoryService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    @Operation(summary = "List categories", description = "Returns the category catalog visible to the authenticated user.")
    public List<CategoryResponse> list() {
        return categoryService.list(currentUserProvider.requireCurrentUser()).stream()
            .map(CategoryResponse::from)
            .toList();
    }

    @PostMapping
    @Operation(summary = "Create a category", description = "Creates a new ticket category.")
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        return CategoryResponse.from(
            categoryService.create(
                currentUserProvider.requireCurrentUser(),
                new UpsertCategoryRequest(0, request.name(), request.description())
            )
        );
    }

    @PutMapping("/{categoryId}")
    @Operation(summary = "Update a category", description = "Updates category name or description.")
    public CategoryResponse update(
        @PathVariable String categoryId,
        @Valid @RequestBody CategoryRequest request
    ) {
        return CategoryResponse.from(
            categoryService.update(
                currentUserProvider.requireCurrentUser(),
                categoryId,
                new UpsertCategoryRequest(request.version(), request.name(), request.description())
            )
        );
    }

    @PatchMapping("/{categoryId}/status")
    @Operation(summary = "Update category status", description = "Activates or deactivates an existing category.")
    public CategoryResponse updateStatus(
        @PathVariable String categoryId,
        @Valid @RequestBody StatusRequest request
    ) {
        return CategoryResponse.from(
            categoryService.updateStatus(
                currentUserProvider.requireCurrentUser(),
                categoryId,
                new StatusUpdateRequest(request.version(), request.active())
            )
        );
    }
}
