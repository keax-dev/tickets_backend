package com.tickets.managementtickets.identity.infrastructure.web.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record StatusRequest(@PositiveOrZero long version, boolean active) {
}
