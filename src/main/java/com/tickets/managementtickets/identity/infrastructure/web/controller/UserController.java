package com.tickets.managementtickets.identity.infrastructure.web.controller;

import com.tickets.managementtickets.identity.application.port.CurrentAuthenticatedUserProvider;
import com.tickets.managementtickets.identity.application.service.UserManagementService;
import com.tickets.managementtickets.identity.infrastructure.web.dto.CreateUserRequest;
import com.tickets.managementtickets.identity.infrastructure.web.dto.StatusRequest;
import com.tickets.managementtickets.identity.infrastructure.web.dto.UpdateUserRequest;
import com.tickets.managementtickets.identity.infrastructure.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User administration endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserManagementService userManagementService;
    private final CurrentAuthenticatedUserProvider currentUserProvider;

    public UserController(UserManagementService userManagementService, CurrentAuthenticatedUserProvider currentUserProvider) {
        this.userManagementService = userManagementService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    @Operation(summary = "List users", description = "Returns the user catalog available to the authenticated administrator or manager.")
    public List<UserResponse> list() {
        return userManagementService.list(currentUserProvider.requireCurrentUser()).stream()
            .map(UserResponse::from)
            .toList();
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user detail", description = "Returns a single user by identifier.")
    public UserResponse getById(@PathVariable String userId) {
        return UserResponse.from(userManagementService.getById(currentUserProvider.requireCurrentUser(), userId));
    }

    @PostMapping
    @Operation(summary = "Create a user", description = "Creates a new platform user with the requested role.")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return UserResponse.from(
            userManagementService.create(
                currentUserProvider.requireCurrentUser(),
                new com.tickets.managementtickets.identity.application.command.CreateUserRequest(
                    request.firstName(),
                    request.lastName(),
                    request.email(),
                    request.password(),
                    request.role()
                )
            )
        );
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update a user", description = "Updates user profile data and role.")
    public UserResponse update(
        @PathVariable String userId,
        @Valid @RequestBody UpdateUserRequest request
    ) {
        return UserResponse.from(
            userManagementService.update(
                currentUserProvider.requireCurrentUser(),
                userId,
                new com.tickets.managementtickets.identity.application.command.UpdateUserRequest(
                    request.version(),
                    request.firstName(),
                    request.lastName(),
                    request.email(),
                    request.role()
                )
            )
        );
    }

    @PatchMapping("/{userId}/status")
    @Operation(summary = "Update user status", description = "Activates or deactivates an existing user.")
    public UserResponse updateStatus(
        @PathVariable String userId,
        @Valid @RequestBody StatusRequest request
    ) {
        return UserResponse.from(
            userManagementService.updateStatus(
                currentUserProvider.requireCurrentUser(),
                userId,
                new com.tickets.managementtickets.identity.application.command.StatusUpdateRequest(request.version(), request.active())
            )
        );
    }
}
