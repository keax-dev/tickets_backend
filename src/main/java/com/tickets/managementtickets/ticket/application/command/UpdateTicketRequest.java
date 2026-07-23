package com.tickets.managementtickets.ticket.application.command;

import com.tickets.managementtickets.shared.domain.model.TicketPriority;

public record UpdateTicketRequest(long version, String title, String description, String categoryId, TicketPriority priority) {
}
