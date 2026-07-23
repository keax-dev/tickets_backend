package com.tickets.managementtickets.identity.infrastructure.persistence.adapter;

import com.tickets.managementtickets.identity.domain.model.User;
import com.tickets.managementtickets.identity.infrastructure.persistence.entity.UserEntity;

final class UserPersistenceMapper {

    User toDomain(UserEntity entity) {
        return new User(
            entity.getId(),
            entity.getFirstName(),
            entity.getLastName(),
            entity.getEmail(),
            entity.getPasswordHash(),
            entity.getRole(),
            entity.isActive(),
            entity.getFailedLoginAttempts(),
            entity.getLastLoginAt(),
            entity.getLockedUntil(),
            entity.getVersion()
        );
    }

    UserEntity toEntity(User user, UserEntity entity) {
        if (user.id() != null) {
            entity.setId(user.id());
        }
        entity.setFirstName(user.firstName());
        entity.setLastName(user.lastName());
        entity.setEmail(user.email());
        entity.setPasswordHash(user.passwordHash());
        entity.setRole(user.role());
        entity.setActive(user.active());
        entity.setFailedLoginAttempts(user.failedLoginAttempts());
        entity.setLastLoginAt(user.lastLoginAt());
        entity.setLockedUntil(user.lockedUntil());
        return entity;
    }
}
