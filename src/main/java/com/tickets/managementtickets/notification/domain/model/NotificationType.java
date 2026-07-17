package com.tickets.managementtickets.notification.domain.model;

public enum NotificationType {
    TICKET_CREATED,
    TICKET_ASSIGNED,
    TICKET_REASSIGNED,
    PUBLIC_COMMENT_ADDED,
    INFORMATION_REQUESTED,
    TICKET_RESOLVED,
    TICKET_REOPENED,
    TICKET_CLOSED,
    SLA_DUE_SOON,
    SLA_BREACHED
}
