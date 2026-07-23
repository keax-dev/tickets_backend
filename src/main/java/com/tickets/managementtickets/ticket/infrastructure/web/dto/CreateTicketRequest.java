package com.tickets.managementtickets.ticket.infrastructure.web.dto;

import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
    @NotBlank @Size(max = 150) String title,
    @NotBlank @Size(max = 5000) String description,
    @NotBlank @Size(max = 36) String categoryId,
    @NotNull TicketPriority priority
) {
}
