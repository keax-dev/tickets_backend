package com.tickets.managementtickets.ticket.application.port;

import com.tickets.managementtickets.ticket.domain.model.TicketComment;

import java.util.List;

public interface TicketCommentRepositoryPort {

    List<TicketComment> findAllByTicketIdOrderByCreatedAtAsc(String ticketId);

    TicketComment save(TicketComment comment);
}
