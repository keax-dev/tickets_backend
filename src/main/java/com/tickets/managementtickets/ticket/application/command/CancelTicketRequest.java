package com.tickets.managementtickets.ticket.application.command;

public record CancelTicketRequest(long version, String reason) {
}
