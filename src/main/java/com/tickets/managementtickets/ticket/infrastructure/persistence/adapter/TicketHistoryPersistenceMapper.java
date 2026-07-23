package com.tickets.managementtickets.ticket.infrastructure.persistence.adapter;

import com.tickets.managementtickets.ticket.domain.model.TicketHistory;
import com.tickets.managementtickets.ticket.infrastructure.persistence.entity.TicketHistoryEntity;

final class TicketHistoryPersistenceMapper {

    TicketHistory toDomain(TicketHistoryEntity entity) {
        return new TicketHistory(
            entity.getId(),
            entity.getTicketId(),
            entity.getAction(),
            entity.getPerformedBy(),
            entity.getPreviousValue(),
            entity.getNewValue(),
            entity.getMetadataJson(),
            entity.getCreatedAt()
        );
    }

    TicketHistoryEntity toEntity(TicketHistory history) {
        TicketHistoryEntity entity = new TicketHistoryEntity();
        entity.setTicketId(history.ticketId());
        entity.setAction(history.action());
        entity.setPerformedBy(history.performedBy());
        entity.setPreviousValue(history.previousValue());
        entity.setNewValue(history.newValue());
        entity.setMetadataJson(history.metadataJson());
        return entity;
    }
}
