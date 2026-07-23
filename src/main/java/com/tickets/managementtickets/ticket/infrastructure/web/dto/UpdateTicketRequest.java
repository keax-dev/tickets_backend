package com.tickets.managementtickets.ticket.infrastructure.web.dto;

import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateTicketRequest(
    @PositiveOrZero long version,
    @Size(max = 150) String title,
    @Size(max = 5000) String description,
    @Size(max = 36) String categoryId,
    TicketPriority priority
) {
}
