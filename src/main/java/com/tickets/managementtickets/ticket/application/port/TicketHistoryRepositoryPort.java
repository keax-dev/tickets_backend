package com.tickets.managementtickets.ticket.application.port;

import com.tickets.managementtickets.ticket.domain.model.TicketHistory;

import java.util.List;

public interface TicketHistoryRepositoryPort {

    List<TicketHistory> findAllByTicketIdOrderByCreatedAtDesc(String ticketId);

    List<TicketHistory> findRecent(TicketVisibility visibility, String currentUserId, int limit);

    TicketHistory save(TicketHistory history);
}
