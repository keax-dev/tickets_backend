package com.tickets.managementtickets.ticket.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ReopenTicketRequest(
    @PositiveOrZero long version,
    @NotBlank @Size(max = 4000) String reason
) {
}
