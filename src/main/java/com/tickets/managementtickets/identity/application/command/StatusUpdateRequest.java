package com.tickets.managementtickets.identity.application.command;

public record StatusUpdateRequest(long version, boolean active) {
}
