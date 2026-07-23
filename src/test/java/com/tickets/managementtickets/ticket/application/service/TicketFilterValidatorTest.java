package com.tickets.managementtickets.ticket.application.service;

import com.tickets.managementtickets.shared.application.exception.BadRequestException;
import com.tickets.managementtickets.shared.application.model.SortDirection;
import com.tickets.managementtickets.ticket.application.query.TicketFilterRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// This test suite verifies ticket list filters before they are converted into repository queries.
class TicketFilterValidatorTest {

    private final TicketFilterValidator validator = new TicketFilterValidator();

    @Test
    void shouldAllowValidFilter() {
        // Arrange: build a valid filter at the maximum allowed page size.
        TicketFilterRequest request = filter(0, 100, "createdAt", null, null);

        // Act and assert: verify valid pagination and sorting are accepted.
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void shouldRejectNegativePage() {
        // Arrange: build a filter with an invalid page number.
        TicketFilterRequest request = filter(-1, 10, "createdAt", null, null);

        // Act: validate the invalid filter.
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> validator.validate(request)
        );

        // Assert: verify the page validation error code.
        assertEquals("INVALID_PAGE", exception.getCode());
    }

    @Test
    void shouldRejectInvalidPageSize() {
        // Arrange: build filters below and above the accepted page-size range.
        TicketFilterRequest tooSmallRequest = filter(0, 0, "createdAt", null, null);
        TicketFilterRequest tooLargeRequest = filter(0, 101, "createdAt", null, null);

        // Act: validate both invalid filters.
        BadRequestException tooSmall = assertThrows(
            BadRequestException.class,
            () -> validator.validate(tooSmallRequest)
        );
        BadRequestException tooLarge = assertThrows(
            BadRequestException.class,
            () -> validator.validate(tooLargeRequest)
        );

        // Assert: verify both bounds use the same page-size error code.
        assertEquals("INVALID_PAGE_SIZE", tooSmall.getCode());
        assertEquals("INVALID_PAGE_SIZE", tooLarge.getCode());
    }

    @Test
    void shouldRejectUnsupportedSortField() {
        // Arrange: build a filter with a sort field not accepted by the API contract.
        TicketFilterRequest request = filter(0, 10, "unsupported", null, null);

        // Act: validate the unsupported sort field.
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> validator.validate(request)
        );

        // Assert: verify unsupported sort fields are rejected with the expected code.
        assertEquals("INVALID_SORT_FIELD", exception.getCode());
    }

    @Test
    void shouldRejectInvalidDateRange() {
        // Arrange: build a filter where the start date is after the end date.
        TicketFilterRequest request = filter(
            0,
            10,
            "createdAt",
            Instant.parse("2026-07-16T10:00:00Z"),
            Instant.parse("2026-07-16T09:00:00Z")
        );

        // Act: validate the invalid date range.
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> validator.validate(request)
        );

        // Assert: verify invalid ranges preserve their public error code.
        assertEquals("INVALID_DATE_RANGE", exception.getCode());
    }

    private TicketFilterRequest filter(int page, int size, String sortBy, Instant createdFrom, Instant createdTo) {
        return new TicketFilterRequest(
            null,
            null,
            null,
            null,
            null,
            createdFrom,
            createdTo,
            page,
            size,
            sortBy,
            SortDirection.DESC
        );
    }
}
