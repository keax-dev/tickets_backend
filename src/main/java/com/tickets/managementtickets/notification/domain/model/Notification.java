package com.tickets.managementtickets.notification.domain.model;

import java.time.Instant;

public record Notification(
    String id,
    String recipientId,
    NotificationType type,
    String title,
    String message,
    String relatedTicketId,
    boolean read,
    Instant createdAt,
    Instant readAt
) {

    public static Notification create(
        String recipientId,
        NotificationType type,
        String title,
        String message,
        String relatedTicketId
    ) {
        return new Notification(null, recipientId, type, title, message, relatedTicketId, false, null, null);
    }

    public Notification markAsRead(Instant readAt) {
        return new Notification(id, recipientId, type, title, message, relatedTicketId, true, createdAt, readAt);
    }
}
