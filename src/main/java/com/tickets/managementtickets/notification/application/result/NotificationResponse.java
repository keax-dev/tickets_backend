package com.tickets.managementtickets.notification.application.result;

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
}
