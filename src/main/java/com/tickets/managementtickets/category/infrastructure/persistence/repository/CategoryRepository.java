package com.tickets.managementtickets.category.infrastructure.persistence.repository;

import com.tickets.managementtickets.category.infrastructure.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CategoryEntity, String> {

    Optional<CategoryEntity> findByNameIgnoreCase(String name);
}
