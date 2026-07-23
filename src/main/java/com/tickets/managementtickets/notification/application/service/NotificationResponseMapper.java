package com.tickets.managementtickets.notification.application.service;

import com.tickets.managementtickets.notification.application.result.NotificationResponse;
import com.tickets.managementtickets.notification.domain.model.Notification;

final class NotificationResponseMapper {

    NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
            notification.id(),
            notification.type(),
            notification.title(),
            notification.message(),
            notification.relatedTicketId(),
            notification.read(),
            notification.createdAt(),
            notification.readAt()
        );
    }
}
