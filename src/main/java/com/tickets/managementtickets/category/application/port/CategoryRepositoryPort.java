package com.tickets.managementtickets.category.application.port;

import com.tickets.managementtickets.category.domain.model.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepositoryPort {

    List<Category> findAll();

    List<Category> findAllById(Iterable<String> ids);

    Optional<Category> findById(String id);

    Optional<Category> findByNameIgnoreCase(String name);

    Category save(Category category);
}
