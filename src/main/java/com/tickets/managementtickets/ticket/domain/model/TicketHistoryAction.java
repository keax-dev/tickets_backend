package com.tickets.managementtickets.ticket.domain.model;

public enum TicketHistoryAction {
    CREATED,
    UPDATED,
    ASSIGNED,
    REASSIGNED,
    STARTED,
    REQUESTED_INFORMATION,
    COMMENT_ADDED_PUBLIC,
    COMMENT_ADDED_INTERNAL,
    RESOLVED,
    REOPENED,
    CLOSED,
    CANCELLED,
    PRIORITY_CHANGED,
    CATEGORY_CHANGED,
    SLA_BREACHED
}
