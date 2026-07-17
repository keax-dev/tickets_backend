package com.tickets.managementtickets.category.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record CategoryRequest(
    @PositiveOrZero long version,
    @NotBlank String name,
    String description
) {
}
