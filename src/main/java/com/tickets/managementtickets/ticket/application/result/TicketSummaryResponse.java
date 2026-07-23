package com.tickets.managementtickets.ticket.application.result;

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
}
