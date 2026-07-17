package com.tickets.managementtickets.sla.infrastructure.web.dto;

import com.tickets.managementtickets.sla.application.service.SlaPolicyService;
import com.tickets.managementtickets.ticket.domain.model.TicketPriority;

public record SlaPolicyResponse(
    String id,
    TicketPriority priority,
    int firstResponseHours,
    int resolutionHours,
    boolean active,
    long version
) {

    public static SlaPolicyResponse from(SlaPolicyService.SlaPolicyResponse response) {
        return new SlaPolicyResponse(
            response.id(),
            response.priority(),
            response.firstResponseHours(),
            response.resolutionHours(),
            response.active(),
            response.version()
        );
    }
}
