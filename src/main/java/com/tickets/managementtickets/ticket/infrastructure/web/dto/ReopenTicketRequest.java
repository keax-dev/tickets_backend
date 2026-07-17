package com.tickets.managementtickets.ticket.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record ReopenTicketRequest(
    @PositiveOrZero long version,
    @NotBlank String reason
) {
}
