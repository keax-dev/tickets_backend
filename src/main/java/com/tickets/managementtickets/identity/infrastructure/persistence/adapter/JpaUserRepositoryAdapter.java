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
    private final UserPersistenceMapper mapper;

    public JpaUserRepositoryAdapter(UserRepository repository) {
        this.repository = repository;
        this.mapper = new UserPersistenceMapper();
    }

    @Override
    public List<User> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<User> findAllById(Iterable<String> ids) {
        return repository.findAllById(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<User> findById(String id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public List<User> findAllByRoleInAndActiveTrue(Collection<Role> roles) {
        return repository.findAllByRoleInAndActiveTrue(roles).stream().map(mapper::toDomain).toList();
    }

    @Override
    public User save(User user) {
        UserEntity entity = user.id() == null
            ? new UserEntity()
            : repository.findById(user.id()).orElseGet(UserEntity::new);
        return mapper.toDomain(repository.save(mapper.toEntity(user, entity)));
    }
}
