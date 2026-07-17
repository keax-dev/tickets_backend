package com.tickets.managementtickets.ticket.infrastructure.persistence.repository;

import com.tickets.managementtickets.ticket.infrastructure.persistence.entity.TicketEntity;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;

public interface TicketRepository extends JpaRepository<TicketEntity, String>, JpaSpecificationExecutor<TicketEntity> {

    List<TicketEntity> findAllByStatusAndResolvedAtBefore(TicketStatus status, Instant resolvedAt);
}
