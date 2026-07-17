package com.tickets.managementtickets.identity.infrastructure.web.controller;

import com.tickets.managementtickets.identity.application.service.UserManagementService;
import com.tickets.managementtickets.identity.infrastructure.security.CurrentUserService;
import com.tickets.managementtickets.identity.infrastructure.web.dto.CreateUserRequest;
import com.tickets.managementtickets.identity.infrastructure.web.dto.StatusRequest;
import com.tickets.managementtickets.identity.infrastructure.web.dto.UpdateUserRequest;
import com.tickets.managementtickets.identity.infrastructure.web.dto.UserResponse;
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
public class UserController {

    private final UserManagementService userManagementService;
    private final CurrentUserService currentUserService;

    public UserController(UserManagementService userManagementService, CurrentUserService currentUserService) {
        this.userManagementService = userManagementService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userManagementService.list(currentUserService.requireCurrentUser()).stream()
            .map(UserResponse::from)
            .toList();
    }

    @GetMapping("/{userId}")
    public UserResponse getById(@PathVariable String userId) {
        return UserResponse.from(userManagementService.getById(currentUserService.requireCurrentUser(), userId));
    }

    @PostMapping
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return UserResponse.from(
            userManagementService.create(
                currentUserService.requireCurrentUser(),
                new UserManagementService.CreateUserRequest(
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
    public UserResponse update(
        @PathVariable String userId,
        @Valid @RequestBody UpdateUserRequest request
    ) {
        return UserResponse.from(
            userManagementService.update(
                currentUserService.requireCurrentUser(),
                userId,
                new UserManagementService.UpdateUserRequest(
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
    public UserResponse updateStatus(
        @PathVariable String userId,
        @Valid @RequestBody StatusRequest request
    ) {
        return UserResponse.from(
            userManagementService.updateStatus(
                currentUserService.requireCurrentUser(),
                userId,
                new UserManagementService.StatusUpdateRequest(request.version(), request.active())
            )
        );
    }
}
