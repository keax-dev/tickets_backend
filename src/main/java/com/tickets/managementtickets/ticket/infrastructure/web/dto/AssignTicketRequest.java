package com.tickets.managementtickets.ticket.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AssignTicketRequest(
    @PositiveOrZero long version,
    @NotBlank @Size(max = 36) String agentId
) {
}
