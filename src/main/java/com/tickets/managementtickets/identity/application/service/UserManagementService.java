package com.tickets.managementtickets.identity.application.service;

import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.identity.domain.model.Role;
import com.tickets.managementtickets.identity.infrastructure.persistence.entity.UserEntity;
import com.tickets.managementtickets.identity.infrastructure.persistence.repository.UserRepository;
import com.tickets.managementtickets.shared.application.exception.ConflictException;
import com.tickets.managementtickets.shared.application.exception.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(
        UserRepository userRepository,
        AuthorizationService authorizationService,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list(AuthenticatedUser currentUser) {
        authorizationService.requirePermission(currentUser, Permission.USER_READ);
        return userRepository.findAll()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getById(AuthenticatedUser currentUser, String userId) {
        authorizationService.requirePermission(currentUser, Permission.USER_READ);
        return toResponse(userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "The user could not be found.")));
    }

    @Transactional
    public UserResponse create(AuthenticatedUser currentUser, CreateUserRequest request) {
        authorizationService.requirePermission(currentUser, Permission.USER_CREATE);
        ensureEmailIsUnique(request.email(), null);

        UserEntity user = new UserEntity();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(normalizeEmail(request.email()));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setActive(true);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(AuthenticatedUser currentUser, String userId, UpdateUserRequest request) {
        authorizationService.requirePermission(currentUser, Permission.USER_UPDATE);
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "The user could not be found."));
        ensureVersion(user.getVersion(), request.version(), "The user was modified by another request.");

        ensureEmailIsUnique(request.email(), userId);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(normalizeEmail(request.email()));
        user.setRole(request.role());
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateStatus(AuthenticatedUser currentUser, String userId, StatusUpdateRequest request) {
        authorizationService.requirePermission(currentUser, Permission.USER_DISABLE);
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "The user could not be found."));
        ensureVersion(user.getVersion(), request.version(), "The user was modified by another request.");
        user.setActive(request.active());
        if (!request.active()) {
            user.setLastLoginAt((Instant) null);
        }
        return toResponse(user);
    }

    private void ensureEmailIsUnique(String email, String currentUserId) {
        userRepository.findByEmail(normalizeEmail(email))
            .filter(existing -> !existing.getId().equals(currentUserId))
            .ifPresent(existing -> {
                throw new ConflictException("USER_EMAIL_ALREADY_EXISTS", "A user with the same email already exists.");
            });
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private void ensureVersion(long currentVersion, long requestedVersion, String message) {
        if (currentVersion != requestedVersion) {
            throw new ConflictException("RESOURCE_VERSION_CONFLICT", message);
        }
    }

    private UserResponse toResponse(UserEntity user) {
        return new UserResponse(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getRole(),
            user.isActive(),
            user.getLastLoginAt(),
            user.getVersion()
        );
    }

    public record CreateUserRequest(String firstName, String lastName, String email, String password, Role role) {
    }

    public record UpdateUserRequest(long version, String firstName, String lastName, String email, Role role) {
    }

    public record StatusUpdateRequest(long version, boolean active) {
    }

    public record UserResponse(
        String id,
        String firstName,
        String lastName,
        String email,
        Role role,
        boolean active,
        Instant lastLoginAt,
        long version
    ) {
    }
}
