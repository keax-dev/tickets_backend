package com.tickets.managementtickets.ticket.infrastructure.persistence.repository;

import com.tickets.managementtickets.ticket.infrastructure.persistence.entity.TicketSequenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TicketSequenceRepository extends JpaRepository<TicketSequenceEntity, Integer> {

    @Modifying
    @Query(
        value = """
            insert into ticket_sequences (sequence_year, current_value)
            values (:sequenceYear, 0)
            on duplicate key update current_value = current_value
            """,
        nativeQuery = true
    )
    void ensureSequenceExists(@Param("sequenceYear") Integer sequenceYear);

    @Query(
        value = "select sequence_year, current_value from ticket_sequences where sequence_year = :sequenceYear for update",
        nativeQuery = true
    )
    Optional<TicketSequenceEntity> findByYearForUpdate(@Param("sequenceYear") Integer sequenceYear);
}
