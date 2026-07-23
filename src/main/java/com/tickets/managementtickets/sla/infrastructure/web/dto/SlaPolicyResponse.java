package com.tickets.managementtickets.sla.infrastructure.web.dto;

import com.tickets.managementtickets.shared.domain.model.TicketPriority;

public record SlaPolicyResponse(
    String id,
    TicketPriority priority,
    int firstResponseHours,
    int resolutionHours,
    boolean active,
    long version
) {

    public static SlaPolicyResponse from(com.tickets.managementtickets.sla.application.result.SlaPolicyResponse response) {
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
