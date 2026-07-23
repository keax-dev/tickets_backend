package com.tickets.managementtickets.identity.application.service;

import com.tickets.managementtickets.identity.application.command.CreateUserRequest;
import com.tickets.managementtickets.identity.application.command.StatusUpdateRequest;
import com.tickets.managementtickets.identity.application.command.UpdateUserRequest;
import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.application.port.UserRepositoryPort;
import com.tickets.managementtickets.identity.application.result.UserResponse;
import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.identity.domain.model.User;
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
    private final UserCommandValidator commandValidator;

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
        this.commandValidator = new UserCommandValidator(userRepository);
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
            commandValidator.ensureEmailIsUnique(request.email(), null);

            User user = User.create(
                request.firstName().trim(),
                request.lastName().trim(),
                commandValidator.normalizeEmail(request.email()),
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
            commandValidator.ensureVersion(user, request.version());

            commandValidator.ensureEmailIsUnique(request.email(), userId);
            return responseMapper.toUserResponse(userRepository.save(user.updateProfile(
                request.firstName().trim(),
                request.lastName().trim(),
                commandValidator.normalizeEmail(request.email()),
                request.role()
            )));
        });
    }

    public UserResponse updateStatus(AuthenticatedUser currentUser, String userId, StatusUpdateRequest request) {
        return transactionRunner.required(() -> {
            authorizationService.requirePermission(currentUser, Permission.USER_DISABLE);
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "The user could not be found."));
            commandValidator.ensureVersion(user, request.version());
            return responseMapper.toUserResponse(userRepository.save(user.withActive(request.active())));
        });
    }

}
