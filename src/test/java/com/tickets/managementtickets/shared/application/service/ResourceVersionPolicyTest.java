package com.tickets.managementtickets.shared.application.service;

import com.tickets.managementtickets.shared.application.exception.ConflictException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// This test suite verifies the shared optimistic concurrency policy used by application validators.
class ResourceVersionPolicyTest {

    @Test
    void shouldAllowMatchingVersions() {
        // Arrange: define matching current and requested versions.
        long currentVersion = 3;
        long requestedVersion = 3;

        // Act and assert: verify matching versions do not throw.
        assertDoesNotThrow(() -> ResourceVersionPolicy.ensureCurrent(currentVersion, requestedVersion, "message"));
    }

    @Test
    void shouldRejectStaleVersion() {
        // Arrange: define a newer current version and a stale requested version.
        long currentVersion = 3;
        long requestedVersion = 2;

        // Act: validate the stale requested version.
        ConflictException exception = assertThrows(
            ConflictException.class,
            () -> ResourceVersionPolicy.ensureCurrent(currentVersion, requestedVersion, "The resource changed.")
        );

        // Assert: verify the policy preserves the shared conflict code and caller message.
        assertEquals("RESOURCE_VERSION_CONFLICT", exception.getCode());
        assertEquals("The resource changed.", exception.getMessage());
    }
}
