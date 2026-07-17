package com.tickets.managementtickets.category.infrastructure.web.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record StatusRequest(@PositiveOrZero long version, boolean active) {
}
