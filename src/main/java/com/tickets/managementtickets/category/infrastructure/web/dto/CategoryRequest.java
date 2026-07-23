package com.tickets.managementtickets.category.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
    @PositiveOrZero long version,
    @NotBlank @Size(max = 120) String name,
    @Size(max = 500) String description
) {
}
