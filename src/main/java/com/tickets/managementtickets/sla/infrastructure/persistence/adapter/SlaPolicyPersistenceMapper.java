package com.tickets.managementtickets.sla.infrastructure.persistence.adapter;

import com.tickets.managementtickets.sla.domain.model.SlaPolicy;
import com.tickets.managementtickets.sla.infrastructure.persistence.entity.SlaPolicyEntity;

final class SlaPolicyPersistenceMapper {

    SlaPolicy toDomain(SlaPolicyEntity entity) {
        return new SlaPolicy(
            entity.getId(),
            entity.getPriority(),
            entity.getFirstResponseHours(),
            entity.getResolutionHours(),
            entity.isActive(),
            entity.getVersion()
        );
    }

    SlaPolicyEntity toEntity(SlaPolicy policy, SlaPolicyEntity entity) {
        if (policy.id() != null) {
            entity.setId(policy.id());
        }
        entity.setPriority(policy.priority());
        entity.setFirstResponseHours(policy.firstResponseHours());
        entity.setResolutionHours(policy.resolutionHours());
        entity.setActive(policy.active());
        return entity;
    }
}
