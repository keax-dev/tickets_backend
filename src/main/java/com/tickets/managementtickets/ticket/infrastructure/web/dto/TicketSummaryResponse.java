package com.tickets.managementtickets.ticket.infrastructure.web.dto;

import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;

import java.time.Instant;

public record TicketSummaryResponse(
    String id,
    String code,
    String title,
    TicketStatus status,
    TicketPriority priority,
    String requesterId,
    String requesterName,
    String assignedAgentId,
    String assignedAgentName,
    String categoryId,
    String categoryName,
    Instant resolutionDueAt,
    boolean slaFirstResponseBreached,
    boolean slaResolutionBreached,
    Instant createdAt,
    Instant updatedAt,
    long version
) {

    public static TicketSummaryResponse from(com.tickets.managementtickets.ticket.application.result.TicketSummaryResponse response) {
        return new TicketSummaryResponse(
            response.id(),
            response.code(),
            response.title(),
            response.status(),
            response.priority(),
            response.requesterId(),
            response.requesterName(),
            response.assignedAgentId(),
            response.assignedAgentName(),
            response.categoryId(),
            response.categoryName(),
            response.resolutionDueAt(),
            response.slaFirstResponseBreached(),
            response.slaResolutionBreached(),
            response.createdAt(),
            response.updatedAt(),
            response.version()
        );
    }
}
