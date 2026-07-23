package com.tickets.managementtickets.ticket.application.service;

import com.tickets.managementtickets.category.application.port.CategoryRepositoryPort;
import com.tickets.managementtickets.category.domain.model.Category;
import com.tickets.managementtickets.shared.application.exception.NotFoundException;
import com.tickets.managementtickets.shared.application.exception.ValidationException;
import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import com.tickets.managementtickets.sla.application.port.SlaPolicyRepositoryPort;
import com.tickets.managementtickets.sla.domain.model.SlaPolicy;

final class TicketReferenceResolver {

    private final CategoryRepositoryPort categoryRepository;
    private final SlaPolicyRepositoryPort slaPolicyRepository;

    TicketReferenceResolver(CategoryRepositoryPort categoryRepository, SlaPolicyRepositoryPort slaPolicyRepository) {
        this.categoryRepository = categoryRepository;
        this.slaPolicyRepository = slaPolicyRepository;
    }

    Category findActiveCategory(String categoryId) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "The category could not be found."));
        if (!category.active()) {
            throw new ValidationException("CATEGORY_INACTIVE", "The category is inactive.");
        }
        return category;
    }

    SlaPolicy findActiveSlaPolicy(TicketPriority priority) {
        SlaPolicy policy = slaPolicyRepository.findByPriority(priority)
            .orElseThrow(() -> new NotFoundException("SLA_POLICY_NOT_FOUND", "The SLA policy could not be found."));
        if (!policy.active()) {
            throw new ValidationException("SLA_POLICY_INACTIVE", "The SLA policy is inactive.");
        }
        return policy;
    }
}
