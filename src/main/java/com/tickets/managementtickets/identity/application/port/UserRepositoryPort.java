package com.tickets.managementtickets.identity.application.port;

import com.tickets.managementtickets.identity.domain.model.Role;
import com.tickets.managementtickets.identity.domain.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {

    List<User> findAll();

    List<User> findAllById(Iterable<String> ids);

    Optional<User> findById(String id);

    Optional<User> findByEmail(String email);

    List<User> findAllByRoleInAndActiveTrue(Collection<Role> roles);

    User save(User user);
}
