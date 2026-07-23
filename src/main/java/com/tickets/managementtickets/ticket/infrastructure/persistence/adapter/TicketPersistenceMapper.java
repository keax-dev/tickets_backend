package com.tickets.managementtickets.ticket.infrastructure.persistence.adapter;

import com.tickets.managementtickets.ticket.domain.model.Ticket;
import com.tickets.managementtickets.ticket.infrastructure.persistence.entity.TicketEntity;

final class TicketPersistenceMapper {

    Ticket toDomain(TicketEntity entity) {
        return new Ticket(
            entity.getId(),
            entity.getCode(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getStatus(),
            entity.getPriority(),
            entity.getRequesterId(),
            entity.getAssignedAgentId(),
            entity.getCategoryId(),
            entity.getFirstResponseDueAt(),
            entity.getResolutionDueAt(),
            entity.getFirstRespondedAt(),
            entity.getResolvedAt(),
            entity.getClosedAt(),
            entity.getCancelledAt(),
            entity.getSlaPausedAt(),
            entity.getAccumulatedPausedSeconds(),
            entity.isSlaFirstResponseBreached(),
            entity.isSlaResolutionBreached(),
            entity.getResolutionSummary(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getVersion()
        );
    }

    TicketEntity toEntity(Ticket ticket, TicketEntity entity) {
        if (ticket.getId() != null) {
            entity.setId(ticket.getId());
        }
        entity.setCode(ticket.getCode());
        entity.setTitle(ticket.getTitle());
        entity.setDescription(ticket.getDescription());
        entity.setStatus(ticket.getStatus());
        entity.setPriority(ticket.getPriority());
        entity.setRequesterId(ticket.getRequesterId());
        entity.setAssignedAgentId(ticket.getAssignedAgentId());
        entity.setCategoryId(ticket.getCategoryId());
        entity.setFirstResponseDueAt(ticket.getFirstResponseDueAt());
        entity.setResolutionDueAt(ticket.getResolutionDueAt());
        entity.setFirstRespondedAt(ticket.getFirstRespondedAt());
        entity.setResolvedAt(ticket.getResolvedAt());
        entity.setClosedAt(ticket.getClosedAt());
        entity.setCancelledAt(ticket.getCancelledAt());
        entity.setSlaPausedAt(ticket.getSlaPausedAt());
        entity.setAccumulatedPausedSeconds(ticket.getAccumulatedPausedSeconds());
        entity.setSlaFirstResponseBreached(ticket.isSlaFirstResponseBreached());
        entity.setSlaResolutionBreached(ticket.isSlaResolutionBreached());
        entity.setResolutionSummary(ticket.getResolutionSummary());
        return entity;
    }
}
