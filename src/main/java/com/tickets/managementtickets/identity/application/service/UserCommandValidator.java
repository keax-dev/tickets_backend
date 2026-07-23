package com.tickets.managementtickets.identity.application.service;

import com.tickets.managementtickets.identity.application.port.UserRepositoryPort;
import com.tickets.managementtickets.identity.domain.model.User;
import com.tickets.managementtickets.shared.application.exception.ConflictException;
import com.tickets.managementtickets.shared.application.service.ResourceVersionPolicy;

final class UserCommandValidator {

    private final UserRepositoryPort userRepository;

    UserCommandValidator(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    void ensureEmailIsUnique(String email, String currentUserId) {
        userRepository.findByEmail(normalizeEmail(email))
            .filter(existing -> !existing.id().equals(currentUserId))
            .ifPresent(existing -> {
                throw new ConflictException("USER_EMAIL_ALREADY_EXISTS", "A user with the same email already exists.");
            });
    }

    void ensureVersion(User user, long requestedVersion) {
        ResourceVersionPolicy.ensureCurrent(user.version(), requestedVersion, "The user was modified by another request.");
    }

    String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
