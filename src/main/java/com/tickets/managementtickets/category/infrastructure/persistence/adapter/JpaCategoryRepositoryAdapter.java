package com.tickets.managementtickets.category.infrastructure.persistence.adapter;

import com.tickets.managementtickets.category.application.port.CategoryRepositoryPort;
import com.tickets.managementtickets.category.domain.model.Category;
import com.tickets.managementtickets.category.infrastructure.persistence.entity.CategoryEntity;
import com.tickets.managementtickets.category.infrastructure.persistence.repository.CategoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaCategoryRepositoryAdapter implements CategoryRepositoryPort {

    private final CategoryRepository repository;
    private final CategoryPersistenceMapper mapper;

    public JpaCategoryRepositoryAdapter(CategoryRepository repository) {
        this.repository = repository;
        this.mapper = new CategoryPersistenceMapper();
    }

    @Override
    public List<Category> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Category> findAllById(Iterable<String> ids) {
        return repository.findAllById(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Category> findById(String id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Category> findByNameIgnoreCase(String name) {
        return repository.findByNameIgnoreCase(name).map(mapper::toDomain);
    }

    @Override
    public Category save(Category category) {
        CategoryEntity entity = category.id() == null
            ? new CategoryEntity()
            : repository.findById(category.id()).orElseGet(CategoryEntity::new);
        return mapper.toDomain(repository.save(mapper.toEntity(category, entity)));
    }
}
