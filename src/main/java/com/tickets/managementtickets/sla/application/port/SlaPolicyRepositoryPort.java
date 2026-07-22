package com.tickets.managementtickets.sla.application.port;

import com.tickets.managementtickets.sla.domain.model.SlaPolicy;
import com.tickets.managementtickets.ticket.domain.model.TicketPriority;

import java.util.List;
import java.util.Optional;

public interface SlaPolicyRepositoryPort {

    List<SlaPolicy> findAll();

    Optional<SlaPolicy> findByPriority(TicketPriority priority);

    SlaPolicy save(SlaPolicy policy);
}
