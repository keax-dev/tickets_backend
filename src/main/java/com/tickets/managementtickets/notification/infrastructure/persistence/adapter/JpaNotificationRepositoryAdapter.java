package com.tickets.managementtickets.notification.infrastructure.persistence.adapter;

import com.tickets.managementtickets.notification.application.port.NotificationRepositoryPort;
import com.tickets.managementtickets.notification.domain.model.Notification;
import com.tickets.managementtickets.notification.infrastructure.persistence.entity.NotificationEntity;
import com.tickets.managementtickets.notification.infrastructure.persistence.repository.NotificationRepository;
import com.tickets.managementtickets.shared.application.model.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaNotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final NotificationRepository repository;
    private final NotificationPersistenceMapper mapper;

    public JpaNotificationRepositoryAdapter(NotificationRepository repository) {
        this.repository = repository;
        this.mapper = new NotificationPersistenceMapper();
    }

    @Override
    public PageResponse<Notification> findAllByRecipientIdOrderByCreatedAtDesc(String recipientId, int page, int size) {
        Page<Notification> notificationPage = repository
            .findAllByRecipientIdOrderByCreatedAtDesc(recipientId, PageRequest.of(page, size))
            .map(mapper::toDomain);

        return PageResponse.of(
            notificationPage.getContent(),
            notificationPage.getNumber(),
            notificationPage.getSize(),
            notificationPage.getTotalElements(),
            notificationPage.getTotalPages(),
            List.of("createdAt,desc")
        );
    }

    @Override
    public Optional<Notification> findByIdAndRecipientId(String id, String recipientId) {
        return repository.findByIdAndRecipientId(id, recipientId).map(mapper::toDomain);
    }

    @Override
    public List<Notification> findAllByRecipientIdAndReadFalse(String recipientId) {
        return repository.findAllByRecipientIdAndReadFalse(recipientId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Notification save(Notification notification) {
        NotificationEntity entity = notification.id() == null
            ? new NotificationEntity()
            : repository.findById(notification.id()).orElseGet(NotificationEntity::new);
        return mapper.toDomain(repository.save(mapper.toEntity(notification, entity)));
    }
}
