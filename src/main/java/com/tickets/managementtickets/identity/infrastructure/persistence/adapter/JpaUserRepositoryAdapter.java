package com.tickets.managementtickets.identity.infrastructure.persistence.adapter;

import com.tickets.managementtickets.identity.application.port.UserRepositoryPort;
import com.tickets.managementtickets.identity.domain.model.Role;
import com.tickets.managementtickets.identity.domain.model.User;
import com.tickets.managementtickets.identity.infrastructure.persistence.entity.UserEntity;
import com.tickets.managementtickets.identity.infrastructure.persistence.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public class JpaUserRepositoryAdapter implements UserRepositoryPort {

    private final UserRepository repository;

    public JpaUserRepositoryAdapter(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<User> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<User> findAllById(Iterable<String> ids) {
        return repository.findAllById(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<User> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public List<User> findAllByRoleInAndActiveTrue(Collection<Role> roles) {
        return repository.findAllByRoleInAndActiveTrue(roles).stream().map(this::toDomain).toList();
    }

    @Override
    public User save(User user) {
        return toDomain(repository.save(toEntity(user)));
    }

    private User toDomain(UserEntity entity) {
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

    private UserEntity toEntity(User user) {
        UserEntity entity = user.id() == null
            ? new UserEntity()
            : repository.findById(user.id()).orElseGet(UserEntity::new);
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
