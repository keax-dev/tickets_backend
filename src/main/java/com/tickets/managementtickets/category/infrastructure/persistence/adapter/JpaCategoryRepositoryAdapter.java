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

    public JpaCategoryRepositoryAdapter(CategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Category> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Category> findAllById(Iterable<String> ids) {
        return repository.findAllById(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Category> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Category> findByNameIgnoreCase(String name) {
        return repository.findByNameIgnoreCase(name).map(this::toDomain);
    }

    @Override
    public Category save(Category category) {
        return toDomain(repository.save(toEntity(category)));
    }

    private Category toDomain(CategoryEntity entity) {
        return new Category(
            entity.getId(),
            entity.getName(),
            entity.getDescription(),
            entity.isActive(),
            entity.getVersion(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private CategoryEntity toEntity(Category category) {
        CategoryEntity entity = category.id() == null
            ? new CategoryEntity()
            : repository.findById(category.id()).orElseGet(CategoryEntity::new);
        if (category.id() != null) {
            entity.setId(category.id());
        }
        entity.setName(category.name());
        entity.setDescription(category.description());
        entity.setActive(category.active());
        return entity;
    }
}
