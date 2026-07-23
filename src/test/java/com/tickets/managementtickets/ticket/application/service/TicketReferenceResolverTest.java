package com.tickets.managementtickets.ticket.application.service;

import com.tickets.managementtickets.category.application.port.CategoryRepositoryPort;
import com.tickets.managementtickets.category.domain.model.Category;
import com.tickets.managementtickets.shared.application.exception.NotFoundException;
import com.tickets.managementtickets.shared.application.exception.ValidationException;
import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import com.tickets.managementtickets.sla.application.port.SlaPolicyRepositoryPort;
import com.tickets.managementtickets.sla.domain.model.SlaPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// This test suite verifies ticket reference lookup rules for active categories and SLA policies.
class TicketReferenceResolverTest {

    // Mock the category port because the resolver should depend on the abstraction, not on a database.
    @Mock
    private CategoryRepositoryPort categoryRepository;

    // Mock the SLA port for the same reason: we only want to exercise resolver logic here.
    @Mock
    private SlaPolicyRepositoryPort slaPolicyRepository;

    @Test
    void shouldResolveActiveCategory() {
        // Arrange: mock an active category returned by the category port.
        Category category = category(true);
        TicketReferenceResolver resolver = new TicketReferenceResolver(categoryRepository, slaPolicyRepository);
        when(categoryRepository.findById("category-1")).thenReturn(Optional.of(category));

        // Act and assert: verify the active category is returned unchanged.
        assertSame(category, resolver.findActiveCategory("category-1"));
    }

    @Test
    void shouldRejectMissingCategory() {
        // Arrange: mock an empty category lookup.
        TicketReferenceResolver resolver = new TicketReferenceResolver(categoryRepository, slaPolicyRepository);
        when(categoryRepository.findById("category-1")).thenReturn(Optional.empty());

        // Act: resolve a missing category.
        NotFoundException exception = assertThrows(
            NotFoundException.class,
            () -> resolver.findActiveCategory("category-1")
        );

        // Assert: verify the missing category error code.
        assertEquals("CATEGORY_NOT_FOUND", exception.getCode());
    }

    @Test
    void shouldRejectInactiveCategory() {
        // Arrange: mock an inactive category returned by the category port.
        TicketReferenceResolver resolver = new TicketReferenceResolver(categoryRepository, slaPolicyRepository);
        when(categoryRepository.findById("category-1")).thenReturn(Optional.of(category(false)));

        // Act: resolve an inactive category.
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> resolver.findActiveCategory("category-1")
        );

        // Assert: verify inactive categories cannot be used by ticket commands.
        assertEquals("CATEGORY_INACTIVE", exception.getCode());
    }

    @Test
    void shouldResolveActiveSlaPolicy() {
        // Arrange: mock an active SLA policy for the requested priority.
        SlaPolicy policy = slaPolicy(true);
        TicketReferenceResolver resolver = new TicketReferenceResolver(categoryRepository, slaPolicyRepository);
        when(slaPolicyRepository.findByPriority(TicketPriority.HIGH)).thenReturn(Optional.of(policy));

        // Act and assert: verify the active SLA policy is returned unchanged.
        assertSame(policy, resolver.findActiveSlaPolicy(TicketPriority.HIGH));
    }

    @Test
    void shouldRejectMissingSlaPolicy() {
        // Arrange: mock an empty SLA policy lookup.
        TicketReferenceResolver resolver = new TicketReferenceResolver(categoryRepository, slaPolicyRepository);
        when(slaPolicyRepository.findByPriority(TicketPriority.HIGH)).thenReturn(Optional.empty());

        // Act: resolve a missing SLA policy.
        NotFoundException exception = assertThrows(
            NotFoundException.class,
            () -> resolver.findActiveSlaPolicy(TicketPriority.HIGH)
        );

        // Assert: verify the missing SLA policy error code.
        assertEquals("SLA_POLICY_NOT_FOUND", exception.getCode());
    }

    @Test
    void shouldRejectInactiveSlaPolicy() {
        // Arrange: mock an inactive SLA policy for the requested priority.
        TicketReferenceResolver resolver = new TicketReferenceResolver(categoryRepository, slaPolicyRepository);
        when(slaPolicyRepository.findByPriority(TicketPriority.HIGH)).thenReturn(Optional.of(slaPolicy(false)));

        // Act: resolve an inactive SLA policy.
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> resolver.findActiveSlaPolicy(TicketPriority.HIGH)
        );

        // Assert: verify inactive SLA policies cannot be used by ticket commands.
        assertEquals("SLA_POLICY_INACTIVE", exception.getCode());
    }

    private Category category(boolean active) {
        // Create a compact category fixture where only the active flag changes between scenarios.
        return new Category("category-1", "Hardware", null, active, 0, null, null);
    }

    private SlaPolicy slaPolicy(boolean active) {
        // Create a compact SLA fixture where only the active flag changes between scenarios.
        return new SlaPolicy("sla-1", TicketPriority.HIGH, 1, 4, active, 0);
    }
}
