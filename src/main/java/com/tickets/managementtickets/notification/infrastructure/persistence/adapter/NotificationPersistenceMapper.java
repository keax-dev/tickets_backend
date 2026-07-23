package com.tickets.managementtickets.notification.infrastructure.persistence.adapter;

import com.tickets.managementtickets.notification.domain.model.Notification;
import com.tickets.managementtickets.notification.infrastructure.persistence.entity.NotificationEntity;

final class NotificationPersistenceMapper {

    Notification toDomain(NotificationEntity entity) {
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

    NotificationEntity toEntity(Notification notification, NotificationEntity entity) {
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
