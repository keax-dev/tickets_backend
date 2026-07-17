package com.tickets.managementtickets.sla.infrastructure.persistence.entity;

import com.tickets.managementtickets.shared.infrastructure.persistence.BaseUuidEntity;
import com.tickets.managementtickets.ticket.domain.model.TicketPriority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "sla_policies")
public class SlaPolicyEntity extends BaseUuidEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, unique = true, length = 16)
    private TicketPriority priority;

    @Column(name = "first_response_hours", nullable = false)
    private int firstResponseHours;

    @Column(name = "resolution_hours", nullable = false)
    private int resolutionHours;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public int getFirstResponseHours() {
        return firstResponseHours;
    }

    public void setFirstResponseHours(int firstResponseHours) {
        this.firstResponseHours = firstResponseHours;
    }

    public int getResolutionHours() {
        return resolutionHours;
    }

    public void setResolutionHours(int resolutionHours) {
        this.resolutionHours = resolutionHours;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
