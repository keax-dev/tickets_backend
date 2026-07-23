package com.tickets.managementtickets.identity.application.service;

import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.application.result.AuthResponse;
import com.tickets.managementtickets.identity.application.result.AuthenticatedUserResponse;
import com.tickets.managementtickets.identity.application.result.UserResponse;
import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.identity.domain.model.User;

import java.time.Instant;
import java.util.List;

final class IdentityResponseMapper {

    UserResponse toUserResponse(User user) {
        return new UserResponse(
            user.id(),
            user.firstName(),
            user.lastName(),
            user.email(),
            user.role(),
            user.active(),
            user.lastLoginAt(),
            user.version()
        );
    }

    AuthenticatedUserResponse toAuthenticatedUserResponse(AuthenticatedUser user) {
        List<String> permissions = user.permissions().stream().map(Permission::name).toList();
        return new AuthenticatedUserResponse(user.id(), user.firstName(), user.lastName(), user.email(), user.role(), permissions);
    }

    AuthResponse toAuthResponse(String accessToken, Instant accessTokenExpiresAt, AuthenticatedUser user) {
        return new AuthResponse(accessToken, accessTokenExpiresAt, toAuthenticatedUserResponse(user));
    }
}
