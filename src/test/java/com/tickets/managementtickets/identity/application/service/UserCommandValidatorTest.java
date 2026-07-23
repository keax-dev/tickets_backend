package com.tickets.managementtickets.identity.application.service;

import com.tickets.managementtickets.identity.application.port.UserRepositoryPort;
import com.tickets.managementtickets.identity.domain.model.Role;
import com.tickets.managementtickets.identity.domain.model.User;
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
// This test suite verifies user command validation rules for email uniqueness, normalization, and version conflicts.
class UserCommandValidatorTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Test
    void shouldNormalizeEmail() {
        // Arrange: create the validator with a mocked user repository.
        UserCommandValidator validator = new UserCommandValidator(userRepository);

        // Act and assert: verify email values are trimmed, lowercased, and null-safe.
        assertEquals("user@test.com", validator.normalizeEmail(" User@Test.COM "));
        assertEquals("", validator.normalizeEmail(null));
    }

    @Test
    void shouldRejectDuplicatedEmailFromAnotherUser() {
        // Arrange: mock an existing user with the requested normalized email and a different id.
        UserCommandValidator validator = new UserCommandValidator(userRepository);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user("user-2")));

        // Act: validate the email for another current user.
        ConflictException exception = assertThrows(
            ConflictException.class,
            () -> validator.ensureEmailIsUnique(" User@Test.COM ", "user-1")
        );

        // Assert: verify duplicated emails keep the expected application error code.
        assertEquals("USER_EMAIL_ALREADY_EXISTS", exception.getCode());
    }

    @Test
    void shouldAllowSameEmailForCurrentUser() {
        // Arrange: mock an existing user that matches the same aggregate being updated.
        UserCommandValidator validator = new UserCommandValidator(userRepository);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user("user-1")));

        // Act and assert: verify keeping the same email for the same user is accepted.
        assertDoesNotThrow(() -> validator.ensureEmailIsUnique(" User@Test.COM ", "user-1"));
    }

    @Test
    void shouldRejectStaleUserVersion() {
        // Arrange: create the validator and a user with a newer current version.
        UserCommandValidator validator = new UserCommandValidator(userRepository);

        // Act: validate a stale requested version.
        ConflictException exception = assertThrows(
            ConflictException.class,
            () -> validator.ensureVersion(user("user-1", 2), 1)
        );

        // Assert: verify stale writes use the shared optimistic concurrency error code.
        assertEquals("RESOURCE_VERSION_CONFLICT", exception.getCode());
    }

    private User user(String id) {
        return user(id, 0);
    }

    private User user(String id, long version) {
        return new User(
            id,
            "User",
            "Test",
            "user@test.com",
            "encoded",
            Role.CUSTOMER,
            true,
            0,
            null,
            null,
            version
        );
    }
}
