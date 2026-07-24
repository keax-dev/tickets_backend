package com.tickets.managementtickets.ticket.infrastructure.web.dto;

import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
    @Schema(description = "Short ticket title.", example = "Accounting app crashes on export")
    @NotBlank @Size(max = 150) String title,
    @Schema(description = "Detailed description of the issue or request.", example = "When exporting the general ledger to Excel, the application closes unexpectedly.")
    @NotBlank @Size(max = 5000) String description,
    @Schema(description = "Category identifier selected by the requester.", example = "20000000-0000-0000-0000-000000000003")
    @NotBlank @Size(max = 36) String categoryId,
    @Schema(description = "Business priority assigned to the ticket.", example = "HIGH")
    @NotNull TicketPriority priority
) {
}
