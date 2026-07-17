package com.tickets.managementtickets.ticket.infrastructure.web.dto;

import com.tickets.managementtickets.ticket.application.service.TicketService;
import com.tickets.managementtickets.ticket.domain.model.TicketPriority;
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

    public static TicketDetailResponse from(TicketService.TicketDetailResponse response) {
        return new TicketDetailResponse(
            response.id(),
            response.code(),
            response.title(),
            response.description(),
            response.status(),
            response.priority(),
            response.requesterId(),
            response.requesterName(),
            response.assignedAgentId(),
            response.assignedAgentName(),
            response.categoryId(),
            response.categoryName(),
            response.firstResponseDueAt(),
            response.resolutionDueAt(),
            response.firstRespondedAt(),
            response.resolvedAt(),
            response.closedAt(),
            response.cancelledAt(),
            response.slaPausedAt(),
            response.accumulatedPausedSeconds(),
            response.slaFirstResponseBreached(),
            response.slaResolutionBreached(),
            response.resolutionSummary(),
            response.createdAt(),
            response.updatedAt(),
            response.version(),
            response.availableActions()
        );
    }
}
