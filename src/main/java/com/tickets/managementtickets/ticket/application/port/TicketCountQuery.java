package com.tickets.managementtickets.ticket.application.port;

import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;

import java.time.Instant;

public record TicketCountQuery(
    TicketVisibility visibility,
    String currentUserId,
    TicketStatus status,
    TicketPriority priority,
    boolean excludeTerminalStatuses,
    Instant createdAtFrom,
    boolean onlyUnassigned,
    boolean onlyBreachedSla,
    Instant resolutionDueAfter,
    Instant resolutionDueAtOrBefore,
    String assignedAgentId
) {

    public static TicketCountQuery visibleTo(TicketVisibility visibility, String currentUserId) {
        return new TicketCountQuery(visibility, currentUserId, null, null, false, null, false, false, null, null, null);
    }

    public TicketCountQuery withStatus(TicketStatus status) {
        return new TicketCountQuery(visibility, currentUserId, status, priority, excludeTerminalStatuses, createdAtFrom, onlyUnassigned, onlyBreachedSla, resolutionDueAfter, resolutionDueAtOrBefore, assignedAgentId);
    }

    public TicketCountQuery withPriority(TicketPriority priority) {
        return new TicketCountQuery(visibility, currentUserId, status, priority, excludeTerminalStatuses, createdAtFrom, onlyUnassigned, onlyBreachedSla, resolutionDueAfter, resolutionDueAtOrBefore, assignedAgentId);
    }

    public TicketCountQuery withoutTerminalStatuses() {
        return new TicketCountQuery(visibility, currentUserId, status, priority, true, createdAtFrom, onlyUnassigned, onlyBreachedSla, resolutionDueAfter, resolutionDueAtOrBefore, assignedAgentId);
    }

    public TicketCountQuery createdAtFrom(Instant createdAtFrom) {
        return new TicketCountQuery(visibility, currentUserId, status, priority, excludeTerminalStatuses, createdAtFrom, onlyUnassigned, onlyBreachedSla, resolutionDueAfter, resolutionDueAtOrBefore, assignedAgentId);
    }

    public TicketCountQuery unassignedOnly() {
        return new TicketCountQuery(visibility, currentUserId, status, priority, excludeTerminalStatuses, createdAtFrom, true, onlyBreachedSla, resolutionDueAfter, resolutionDueAtOrBefore, assignedAgentId);
    }

    public TicketCountQuery breachedSlaOnly() {
        return new TicketCountQuery(visibility, currentUserId, status, priority, excludeTerminalStatuses, createdAtFrom, onlyUnassigned, true, resolutionDueAfter, resolutionDueAtOrBefore, assignedAgentId);
    }

    public TicketCountQuery resolutionDueBetween(Instant after, Instant atOrBefore) {
        return new TicketCountQuery(visibility, currentUserId, status, priority, excludeTerminalStatuses, createdAtFrom, onlyUnassigned, onlyBreachedSla, after, atOrBefore, assignedAgentId);
    }

    public TicketCountQuery assignedTo(String userId) {
        return new TicketCountQuery(visibility, currentUserId, status, priority, excludeTerminalStatuses, createdAtFrom, onlyUnassigned, onlyBreachedSla, resolutionDueAfter, resolutionDueAtOrBefore, userId);
    }
}
