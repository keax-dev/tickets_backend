package com.tickets.managementtickets.ticket.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ticket_sequences")
public class TicketSequenceEntity {

    @Id
    @Column(name = "sequence_year", nullable = false)
    private Integer sequenceYear;

    @Column(name = "current_value", nullable = false)
    private long currentValue;

    public Integer getSequenceYear() {
        return sequenceYear;
    }

    public void setSequenceYear(Integer sequenceYear) {
        this.sequenceYear = sequenceYear;
    }

    public long getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(long currentValue) {
        this.currentValue = currentValue;
    }
}
