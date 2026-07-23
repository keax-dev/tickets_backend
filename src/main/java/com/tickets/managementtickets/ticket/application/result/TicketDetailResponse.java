package com.tickets.managementtickets.ticket.application.result;

import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;

import java.time.Instant;
import java.util.List;

public record TicketDetailResponse(
    String id,
    String code,
    String title,
    String description,
    TicketStatus status,
    TicketPriority priority,
    String requesterId,
    String requesterName,
    String assignedAgentId,
    String assignedAgentName,
    String categoryId,
    String categoryName,
    Instant firstResponseDueAt,
    Instant resolutionDueAt,
    Instant firstRespondedAt,
    Instant resolvedAt,
    Instant closedAt,
    Instant cancelledAt,
    Instant slaPausedAt,
    long accumulatedPausedSeconds,
    boolean slaFirstResponseBreached,
    boolean slaResolutionBreached,
    String resolutionSummary,
    Instant createdAt,
    Instant updatedAt,
    long version,
    List<String> availableActions
) {
}
