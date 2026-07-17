package com.tickets.managementtickets.ticket.infrastructure.persistence.repository;

import com.tickets.managementtickets.ticket.infrastructure.persistence.entity.TicketCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketCommentRepository extends JpaRepository<TicketCommentEntity, String> {

    List<TicketCommentEntity> findAllByTicketIdOrderByCreatedAtAsc(String ticketId);
}
