package com.tickets.managementtickets.ticket.infrastructure.persistence.repository;

import com.tickets.managementtickets.ticket.infrastructure.persistence.entity.TicketHistoryEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketHistoryRepository extends JpaRepository<TicketHistoryEntity, String> {

    List<TicketHistoryEntity> findAllByTicketIdOrderByCreatedAtDesc(String ticketId);

    List<TicketHistoryEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
        select history
        from TicketHistoryEntity history
        where exists (
            select 1
            from TicketEntity ticket
            where ticket.id = history.ticketId
              and (ticket.assignedAgentId = :userId or ticket.assignedAgentId is null)
        )
        order by history.createdAt desc
        """)
    List<TicketHistoryEntity> findRecentVisibleForAssignedUser(@Param("userId") String userId, Pageable pageable);

    @Query("""
        select history
        from TicketHistoryEntity history
        where exists (
            select 1
            from TicketEntity ticket
            where ticket.id = history.ticketId
              and ticket.requesterId = :userId
        )
        order by history.createdAt desc
        """)
    List<TicketHistoryEntity> findRecentVisibleForRequester(@Param("userId") String userId, Pageable pageable);
}
