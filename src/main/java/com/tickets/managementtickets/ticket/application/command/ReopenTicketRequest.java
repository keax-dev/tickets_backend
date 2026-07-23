package com.tickets.managementtickets.ticket.application.command;

public record ReopenTicketRequest(long version, String reason) {
}
