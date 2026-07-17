package com.tickets.managementtickets.identity.infrastructure.persistence.repository;

import com.tickets.managementtickets.identity.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, String> {

    Optional<UserEntity> findByEmail(String email);

    List<UserEntity> findAllByRoleInAndActiveTrue(Collection<com.tickets.managementtickets.identity.domain.model.Role> roles);
}
