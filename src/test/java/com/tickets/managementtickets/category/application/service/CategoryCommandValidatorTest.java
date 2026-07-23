package com.tickets.managementtickets.category.application.service;

import com.tickets.managementtickets.category.application.port.CategoryRepositoryPort;
import com.tickets.managementtickets.category.domain.model.Category;
import com.tickets.managementtickets.shared.application.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// This test suite verifies category command validation rules before application services persist changes.
class CategoryCommandValidatorTest {

    @Mock
    private CategoryRepositoryPort categoryRepository;

    @Test
    void shouldNormalizeCategoryName() {
        // Arrange: create the validator with a mocked category repository.
        CategoryCommandValidator validator = new CategoryCommandValidator(categoryRepository);

        // Act and assert: verify null and whitespace normalization behavior.
        assertEquals("Hardware", validator.normalizeName(" Hardware "));
        assertEquals("", validator.normalizeName(null));
    }

    @Test
    void shouldRejectDuplicatedCategoryNameFromAnotherCategory() {
        // Arrange: mock an existing category with the same normalized name but a different id.
        CategoryCommandValidator validator = new CategoryCommandValidator(categoryRepository);
        when(categoryRepository.findByNameIgnoreCase("Hardware")).thenReturn(Optional.of(category("category-2")));

        // Act: validate the requested name for the current category.
        ConflictException exception = assertThrows(
            ConflictException.class,
            () -> validator.ensureUniqueName(" Hardware ", "category-1")
        );

        // Assert: verify the application error contract for duplicated category names.
        assertEquals("CATEGORY_NAME_ALREADY_EXISTS", exception.getCode());
    }

    @Test
    void shouldAllowSameCategoryNameForCurrentCategory() {
        // Arrange: mock an existing category that belongs to the same aggregate being updated.
        CategoryCommandValidator validator = new CategoryCommandValidator(categoryRepository);
        when(categoryRepository.findByNameIgnoreCase("Hardware")).thenReturn(Optional.of(category("category-1")));

        // Act and assert: verify keeping the same normalized name is accepted.
        assertDoesNotThrow(() -> validator.ensureUniqueName(" Hardware ", "category-1"));
    }

    @Test
    void shouldRejectStaleCategoryVersion() {
        // Arrange: create the validator and a category whose current version is newer than the request.
        CategoryCommandValidator validator = new CategoryCommandValidator(categoryRepository);

        // Act: validate a stale requested version.
        ConflictException exception = assertThrows(
            ConflictException.class,
            () -> validator.ensureVersion(new Category("category-1", "Hardware", null, true, 2, null, null), 1)
        );

        // Assert: verify optimistic concurrency failures use the shared conflict code.
        assertEquals("RESOURCE_VERSION_CONFLICT", exception.getCode());
    }

    private Category category(String id) {
        return new Category(id, "Hardware", null, true, 0, null, null);
    }
}
