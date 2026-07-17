package com.tickets.managementtickets.ticket.infrastructure.web.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record VersionedRequest(@PositiveOrZero long version) {
}
