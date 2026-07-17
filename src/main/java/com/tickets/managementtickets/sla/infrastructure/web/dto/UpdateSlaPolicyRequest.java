package com.tickets.managementtickets.sla.infrastructure.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateSlaPolicyRequest(
    @PositiveOrZero long version,
    @Min(1) int firstResponseHours,
    @Min(1) int resolutionHours,
    boolean active
) {
}
