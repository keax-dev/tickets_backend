package com.tickets.managementtickets.ticket.infrastructure.web.dto;

import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "TicketSummaryResponse", description = "Condensed ticket representation used in list screens.")
public record TicketSummaryResponse(
    @Schema(description = "Ticket identifier.", example = "30000000-0000-0000-0000-000000000001")
    String id,
    @Schema(description = "Human-readable ticket code.", example = "TCK-2026-900001")
    String code,
    @Schema(description = "Ticket title.", example = "VPN access for new analyst")
    String title,
    @Schema(description = "Current workflow status.", example = "CREATED")
    TicketStatus status,
    @Schema(description = "Assigned priority.", example = "HIGH")
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
