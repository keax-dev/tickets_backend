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

    public JpaNotificationRepositoryAdapter(NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResponse<Notification> findAllByRecipientIdOrderByCreatedAtDesc(String recipientId, int page, int size) {
        Page<Notification> notificationPage = repository
            .findAllByRecipientIdOrderByCreatedAtDesc(recipientId, PageRequest.of(page, size))
            .map(this::toDomain);

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
        return repository.findByIdAndRecipientId(id, recipientId).map(this::toDomain);
    }

    @Override
    public List<Notification> findAllByRecipientIdAndReadFalse(String recipientId) {
        return repository.findAllByRecipientIdAndReadFalse(recipientId).stream().map(this::toDomain).toList();
    }

    @Override
    public Notification save(Notification notification) {
        return toDomain(repository.save(toEntity(notification)));
    }

    private Notification toDomain(NotificationEntity entity) {
        return new Notification(
            entity.getId(),
            entity.getRecipientId(),
            entity.getType(),
            entity.getTitle(),
            entity.getMessage(),
            entity.getRelatedTicketId(),
            entity.isRead(),
            entity.getCreatedAt(),
            entity.getReadAt()
        );
    }

    private NotificationEntity toEntity(Notification notification) {
        NotificationEntity entity = notification.id() == null
            ? new NotificationEntity()
            : repository.findById(notification.id()).orElseGet(NotificationEntity::new);
        if (notification.id() != null) {
            entity.setId(notification.id());
        }
        entity.setRecipientId(notification.recipientId());
        entity.setType(notification.type());
        entity.setTitle(notification.title());
        entity.setMessage(notification.message());
        entity.setRelatedTicketId(notification.relatedTicketId());
        entity.setRead(notification.read());
        entity.setReadAt(notification.readAt());
        return entity;
    }
}
