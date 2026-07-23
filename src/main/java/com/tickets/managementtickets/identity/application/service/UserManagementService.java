package com.tickets.managementtickets.identity.application.service;

import com.tickets.managementtickets.identity.application.command.CreateUserRequest;
import com.tickets.managementtickets.identity.application.command.StatusUpdateRequest;
import com.tickets.managementtickets.identity.application.command.UpdateUserRequest;
import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.application.port.UserRepositoryPort;
import com.tickets.managementtickets.identity.application.result.UserResponse;
import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.identity.domain.model.User;
import com.tickets.managementtickets.shared.application.exception.ConflictException;
import com.tickets.managementtickets.shared.application.exception.NotFoundException;
import com.tickets.managementtickets.shared.application.port.PasswordHashingService;
import com.tickets.managementtickets.shared.application.port.TransactionRunner;

import java.util.List;

public class UserManagementService {

    private final UserRepositoryPort userRepository;
    private final AuthorizationService authorizationService;
    private final PasswordHashingService passwordHashingService;
    private final TransactionRunner transactionRunner;
    private final IdentityResponseMapper responseMapper;

    public UserManagementService(
        UserRepositoryPort userRepository,
        AuthorizationService authorizationService,
        PasswordHashingService passwordHashingService,
        TransactionRunner transactionRunner
    ) {
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
        this.passwordHashingService = passwordHashingService;
        this.transactionRunner = transactionRunner;
        this.responseMapper = new IdentityResponseMapper();
    }

    public List<UserResponse> list(AuthenticatedUser currentUser) {
        return transactionRunner.readOnly(() -> {
            authorizationService.requirePermission(currentUser, Permission.USER_READ);
            return userRepository.findAll()
                .stream()
                .map(responseMapper::toUserResponse)
                .toList();
        });
    }

    public UserResponse getById(AuthenticatedUser currentUser, String userId) {
        return transactionRunner.readOnly(() -> {
            authorizationService.requirePermission(currentUser, Permission.USER_READ);
            return responseMapper.toUserResponse(userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "The user could not be found.")));
        });
    }

    public UserResponse create(AuthenticatedUser currentUser, CreateUserRequest request) {
        return transactionRunner.required(() -> {
            authorizationService.requirePermission(currentUser, Permission.USER_CREATE);
            ensureEmailIsUnique(request.email(), null);

            User user = User.create(
                request.firstName().trim(),
                request.lastName().trim(),
                normalizeEmail(request.email()),
                passwordHashingService.encode(request.password()),
                request.role()
            );
            return responseMapper.toUserResponse(userRepository.save(user));
        });
    }

    public UserResponse update(AuthenticatedUser currentUser, String userId, UpdateUserRequest request) {
        return transactionRunner.required(() -> {
            authorizationService.requirePermission(currentUser, Permission.USER_UPDATE);
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "The user could not be found."));
            ensureVersion(user.version(), request.version(), "The user was modified by another request.");

            ensureEmailIsUnique(request.email(), userId);
            return responseMapper.toUserResponse(userRepository.save(user.updateProfile(
                request.firstName().trim(),
                request.lastName().trim(),
                normalizeEmail(request.email()),
                request.role()
            )));
        });
    }

    public UserResponse updateStatus(AuthenticatedUser currentUser, String userId, StatusUpdateRequest request) {
        return transactionRunner.required(() -> {
            authorizationService.requirePermission(currentUser, Permission.USER_DISABLE);
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "The user could not be found."));
            ensureVersion(user.version(), request.version(), "The user was modified by another request.");
            return responseMapper.toUserResponse(userRepository.save(user.withActive(request.active())));
        });
    }

    private void ensureEmailIsUnique(String email, String currentUserId) {
        userRepository.findByEmail(normalizeEmail(email))
            .filter(existing -> !existing.id().equals(currentUserId))
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

}
