package com.tickets.managementtickets.ticket.application.service;

import com.tickets.managementtickets.shared.application.exception.BadRequestException;
import com.tickets.managementtickets.ticket.application.query.TicketFilterRequest;

import java.util.Set;

final class TicketFilterValidator {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "createdAt",
        "updatedAt",
        "code",
        "title",
        "status",
        "priority",
        "resolutionDueAt"
    );

    void validate(TicketFilterRequest filterRequest) {
        if (filterRequest.page() < 0) {
            throw new BadRequestException("INVALID_PAGE", "The page number must be greater than or equal to zero.");
        }
        if (filterRequest.size() < 1 || filterRequest.size() > MAX_PAGE_SIZE) {
            throw new BadRequestException("INVALID_PAGE_SIZE", "The page size must be between 1 and " + MAX_PAGE_SIZE + ".");
        }
        if (filterRequest.sortBy() == null || !ALLOWED_SORT_FIELDS.contains(filterRequest.sortBy())) {
            throw new BadRequestException("INVALID_SORT_FIELD", "The sort field is not supported.");
        }
        if (filterRequest.createdFrom() != null
            && filterRequest.createdTo() != null
            && filterRequest.createdFrom().isAfter(filterRequest.createdTo())) {
            throw new BadRequestException("INVALID_DATE_RANGE", "The createdFrom value cannot be after createdTo.");
        }
    }
}
