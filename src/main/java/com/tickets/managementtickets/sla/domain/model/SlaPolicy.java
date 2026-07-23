package com.tickets.managementtickets.sla.domain.model;

import com.tickets.managementtickets.shared.domain.model.TicketPriority;

public record SlaPolicy(
    String id,
    TicketPriority priority,
    int firstResponseHours,
    int resolutionHours,
    boolean active,
    long version
) {

    public SlaPolicy update(int firstResponseHours, int resolutionHours, boolean active) {
        return new SlaPolicy(id, priority, firstResponseHours, resolutionHours, active, version);
    }
}
