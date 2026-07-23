package com.tickets.managementtickets.sla.application.result;

import com.tickets.managementtickets.shared.domain.model.TicketPriority;

public record SlaPolicyResponse(
    String id,
    TicketPriority priority,
    int firstResponseHours,
    int resolutionHours,
    boolean active,
    long version
) {
}
