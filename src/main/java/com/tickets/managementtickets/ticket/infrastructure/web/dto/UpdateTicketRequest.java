package com.tickets.managementtickets.ticket.infrastructure.web.dto;

import com.tickets.managementtickets.ticket.domain.model.TicketPriority;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateTicketRequest(
    @PositiveOrZero long version,
    String title,
    String description,
    String categoryId,
    TicketPriority priority
) {
}
