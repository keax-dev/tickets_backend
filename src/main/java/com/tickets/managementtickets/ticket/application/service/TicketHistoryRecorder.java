package com.tickets.managementtickets.ticket.application.service;

import com.tickets.managementtickets.shared.application.port.JsonCodec;
import com.tickets.managementtickets.ticket.application.port.TicketHistoryRepositoryPort;
import com.tickets.managementtickets.ticket.domain.model.TicketHistory;
import com.tickets.managementtickets.ticket.domain.model.TicketHistoryAction;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;

import java.util.Map;

final class TicketHistoryRecorder {

    private final TicketHistoryRepositoryPort ticketHistoryRepository;
    private final JsonCodec jsonCodec;

    TicketHistoryRecorder(TicketHistoryRepositoryPort ticketHistoryRepository, JsonCodec jsonCodec) {
        this.ticketHistoryRepository = ticketHistoryRepository;
        this.jsonCodec = jsonCodec;
    }

    void created(String ticketId, String performedBy, String code) {
        record(ticketId, TicketHistoryAction.CREATED, performedBy, null, null, metadata("code", code));
    }

    void updated(String ticketId, String performedBy, String previousTitle, String newTitle) {
        record(ticketId, TicketHistoryAction.UPDATED, performedBy, previousTitle, newTitle, null);
    }

    void categoryChanged(String ticketId, String performedBy, String previousCategoryId, String newCategoryId) {
        record(ticketId, TicketHistoryAction.CATEGORY_CHANGED, performedBy, previousCategoryId, newCategoryId, null);
    }

    void priorityChanged(String ticketId, String performedBy, String previousPriority, String newPriority) {
        record(ticketId, TicketHistoryAction.PRIORITY_CHANGED, performedBy, previousPriority, newPriority, null);
    }

    void assigned(String ticketId, String performedBy, String previousAgentId, String newAgentId) {
        TicketHistoryAction action = previousAgentId == null ? TicketHistoryAction.ASSIGNED : TicketHistoryAction.REASSIGNED;
        record(ticketId, action, performedBy, previousAgentId, newAgentId, null);
    }

    void started(String ticketId, String performedBy) {
        record(ticketId, TicketHistoryAction.STARTED, performedBy, TicketStatus.ASSIGNED.name(), TicketStatus.IN_PROGRESS.name(), null);
    }

    void requestedInformation(String ticketId, String performedBy) {
        record(ticketId, TicketHistoryAction.REQUESTED_INFORMATION, performedBy, TicketStatus.IN_PROGRESS.name(), TicketStatus.WAITING_FOR_CUSTOMER.name(), null);
    }

    void resolved(String ticketId, String performedBy, String resolutionSummary) {
        record(ticketId, TicketHistoryAction.RESOLVED, performedBy, null, resolutionSummary, null);
    }

    void closed(String ticketId, String performedBy) {
        record(ticketId, TicketHistoryAction.CLOSED, performedBy, null, null, null);
    }

    void reopened(String ticketId, String performedBy, String reason) {
        record(
            ticketId,
            TicketHistoryAction.REOPENED,
            performedBy,
            TicketStatus.RESOLVED.name(),
            TicketStatus.IN_PROGRESS.name(),
            metadata("reason", reason)
        );
    }

    void cancelled(String ticketId, String performedBy, String reason) {
        record(ticketId, TicketHistoryAction.CANCELLED, performedBy, null, null, metadata("reason", reason));
    }

    void commentAdded(String ticketId, String performedBy, String commentId, boolean publicComment) {
        TicketHistoryAction action = publicComment
            ? TicketHistoryAction.COMMENT_ADDED_PUBLIC
            : TicketHistoryAction.COMMENT_ADDED_INTERNAL;
        record(ticketId, action, performedBy, null, null, metadata("commentId", commentId));
    }

    void autoClosed(String ticketId, String performedBy) {
        record(
            ticketId,
            TicketHistoryAction.CLOSED,
            performedBy,
            TicketStatus.RESOLVED.name(),
            TicketStatus.CLOSED.name(),
            metadata("source", "auto-close")
        );
    }

    private void record(
        String ticketId,
        TicketHistoryAction action,
        String performedBy,
        String previousValue,
        String newValue,
        String metadataJson
    ) {
        ticketHistoryRepository.save(TicketHistory.create(ticketId, action, performedBy, previousValue, newValue, metadataJson));
    }

    private String metadata(String key, String value) {
        return jsonCodec.serialize(Map.of(key, normalize(value)));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
