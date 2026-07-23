package com.tickets.managementtickets.ticket.application.command;

public record ResolveTicketRequest(long version, String resolutionSummary) {
}
