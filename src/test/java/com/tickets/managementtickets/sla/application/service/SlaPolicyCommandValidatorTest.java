package com.tickets.managementtickets.sla.application.service;

import com.tickets.managementtickets.shared.application.exception.ConflictException;
import com.tickets.managementtickets.shared.application.exception.ValidationException;
import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import com.tickets.managementtickets.sla.application.command.UpdateSlaPolicyRequest;
import com.tickets.managementtickets.sla.domain.model.SlaPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// This test suite verifies SLA policy command validation before SLA settings are persisted.
class SlaPolicyCommandValidatorTest {

    private final SlaPolicyCommandValidator validator = new SlaPolicyCommandValidator();

    @Test
    void shouldAllowPositiveSlaValues() {
        // Arrange: build an update request with positive first-response and resolution hours.
        UpdateSlaPolicyRequest request = new UpdateSlaPolicyRequest(0, 1, 4, true);

        // Act and assert: verify valid SLA values are accepted.
        assertDoesNotThrow(() -> validator.ensureValid(request));
    }

    @Test
    void shouldRejectInvalidSlaValues() {
        // Arrange: build requests with invalid first-response and resolution values.
        UpdateSlaPolicyRequest invalidFirstResponse = new UpdateSlaPolicyRequest(0, 0, 4, true);
        UpdateSlaPolicyRequest invalidResolution = new UpdateSlaPolicyRequest(0, 1, 0, true);

        // Act: validate each invalid request.
        ValidationException firstResponseException = assertThrows(
            ValidationException.class,
            () -> validator.ensureValid(invalidFirstResponse)
        );
        ValidationException resolutionException = assertThrows(
            ValidationException.class,
            () -> validator.ensureValid(invalidResolution)
        );

        // Assert: verify both invalid numeric cases use the same SLA validation code.
        assertEquals("INVALID_SLA_POLICY", firstResponseException.getCode());
        assertEquals("INVALID_SLA_POLICY", resolutionException.getCode());
    }

    @Test
    void shouldRejectStaleSlaPolicyVersion() {
        // Arrange: create a policy whose current version is newer than the request.
        SlaPolicy policy = new SlaPolicy("sla-1", TicketPriority.HIGH, 1, 4, true, 2);

        // Act: validate a stale requested version.
        ConflictException exception = assertThrows(
            ConflictException.class,
            () -> validator.ensureVersion(policy, 1)
        );

        // Assert: verify version conflicts use the shared optimistic concurrency code.
        assertEquals("RESOURCE_VERSION_CONFLICT", exception.getCode());
    }
}
