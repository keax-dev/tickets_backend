package com.tickets.managementtickets.category.application.command;

public record StatusUpdateRequest(long version, boolean active) {
}
