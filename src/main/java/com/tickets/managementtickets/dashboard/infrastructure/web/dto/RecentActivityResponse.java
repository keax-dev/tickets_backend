package com.tickets.managementtickets.dashboard.infrastructure.web.dto;

import com.tickets.managementtickets.dashboard.application.service.DashboardService;
import com.tickets.managementtickets.ticket.domain.model.TicketHistoryAction;

import java.time.Instant;

public record RecentActivityResponse(
    String id,
    String ticketId,
    TicketHistoryAction action,
    String performedByName,
    Instant createdAt
) {

    public static RecentActivityResponse from(DashboardService.RecentActivityResponse response) {
        return new RecentActivityResponse(
            response.id(),
            response.ticketId(),
            response.action(),
            response.performedByName(),
            response.createdAt()
        );
    }
}
