package com.tickets.managementtickets.sla.application.command;

public record UpdateSlaPolicyRequest(long version, int firstResponseHours, int resolutionHours, boolean active) {
}
