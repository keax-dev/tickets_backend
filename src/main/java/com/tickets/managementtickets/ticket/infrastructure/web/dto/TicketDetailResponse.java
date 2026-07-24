package com.tickets.managementtickets.ticket.infrastructure.web.dto;

import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(name = "TicketDetailResponse", description = "Detailed ticket representation used by ticket detail and command endpoints.")
public record TicketDetailResponse(
    @Schema(description = "Ticket identifier.", example = "30000000-0000-0000-0000-000000000001")
    String id,
    @Schema(description = "Human-readable ticket code.", example = "TCK-2026-900001")
    String code,
    @Schema(description = "Ticket title.", example = "VPN access for new analyst")
    String title,
    @Schema(description = "Ticket description.", example = "A new finance analyst needs VPN access and MFA enrollment.")
    String description,
    @Schema(description = "Current workflow status.", example = "IN_PROGRESS")
    TicketStatus status,
    @Schema(description = "Assigned priority.", example = "HIGH")
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
    @ArraySchema(schema = @Schema(example = "update"), arraySchema = @Schema(description = "Actions currently available to the authenticated user for this ticket."))
    List<String> availableActions
) {

    public static TicketDetailResponse from(com.tickets.managementtickets.ticket.application.result.TicketDetailResponse response) {
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
