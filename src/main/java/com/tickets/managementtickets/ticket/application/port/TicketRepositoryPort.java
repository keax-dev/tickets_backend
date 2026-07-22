package com.tickets.managementtickets.ticket.application.port;

import com.tickets.managementtickets.shared.application.model.PageResponse;
import com.tickets.managementtickets.ticket.domain.model.Ticket;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TicketRepositoryPort {

    PageResponse<Ticket> findAll(TicketQuery query);

    Optional<Ticket> findById(String id);

    Ticket save(Ticket ticket);

    long count(TicketCountQuery query);

    List<Ticket> findAllByStatusAndResolvedAtBefore(TicketStatus status, Instant resolvedAt);
}
