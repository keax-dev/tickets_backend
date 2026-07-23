package com.tickets.managementtickets.ticket.application.command;

import com.tickets.managementtickets.shared.domain.model.TicketPriority;

public record CreateTicketRequest(String title, String description, String categoryId, TicketPriority priority) {
}
