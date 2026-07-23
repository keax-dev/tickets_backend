package com.tickets.managementtickets.ticket.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ResolveTicketRequest(
    @PositiveOrZero long version,
    @NotBlank @Size(max = 5000) String resolutionSummary
) {
}
