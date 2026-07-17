package com.tickets.managementtickets.ticket.infrastructure.persistence.repository;

import com.tickets.managementtickets.ticket.infrastructure.persistence.entity.TicketSequenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TicketSequenceRepository extends JpaRepository<TicketSequenceEntity, Integer> {

    @Query(
        value = "select sequence_year, current_value from ticket_sequences where sequence_year = :sequenceYear for update",
        nativeQuery = true
    )
    Optional<TicketSequenceEntity> findByYearForUpdate(@Param("sequenceYear") Integer sequenceYear);
}
