package com.tickets.managementtickets.sla.infrastructure.persistence.repository;

import com.tickets.managementtickets.sla.infrastructure.persistence.entity.SlaPolicyEntity;
import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicyEntity, String> {

    Optional<SlaPolicyEntity> findByPriority(TicketPriority priority);
}
