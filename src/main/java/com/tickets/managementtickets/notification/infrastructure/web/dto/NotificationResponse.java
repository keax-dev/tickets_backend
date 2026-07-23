package com.tickets.managementtickets.notification.infrastructure.web.dto;

import com.tickets.managementtickets.notification.domain.model.NotificationType;

import java.time.Instant;

public record NotificationResponse(
    String id,
    NotificationType type,
    String title,
    String message,
    String relatedTicketId,
    boolean read,
    Instant createdAt,
    Instant readAt
) {

    public static NotificationResponse from(com.tickets.managementtickets.notification.application.result.NotificationResponse response) {
        return new NotificationResponse(
            response.id(),
            response.type(),
            response.title(),
            response.message(),
            response.relatedTicketId(),
            response.read(),
            response.createdAt(),
            response.readAt()
        );
    }
}
