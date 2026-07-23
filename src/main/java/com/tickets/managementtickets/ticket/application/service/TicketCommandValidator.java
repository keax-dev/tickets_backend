package com.tickets.managementtickets.ticket.application.service;

import com.tickets.managementtickets.identity.domain.model.Role;
import com.tickets.managementtickets.identity.domain.model.User;
import com.tickets.managementtickets.shared.application.exception.ValidationException;
import com.tickets.managementtickets.shared.application.service.ResourceVersionPolicy;
import com.tickets.managementtickets.ticket.domain.model.Ticket;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;

final class TicketCommandValidator {

    void ensureVersion(Ticket ticket, long requestedVersion) {
        ResourceVersionPolicy.ensureCurrent(ticket.getVersion(), requestedVersion, "The ticket was modified by another request.");
    }

    void ensureNotTerminal(Ticket ticket) {
        if (ticket.isTerminal()) {
            throw new ValidationException("TICKET_TERMINAL", "Terminal tickets cannot be modified.");
        }
    }

    void ensureAssigneeCanHandleTickets(User assignee) {
        if (!assignee.active()) {
            throw new ValidationException("ASSIGNEE_INACTIVE", "The assignee must be active.");
        }
        if (assignee.role() != Role.SUPPORT_AGENT && assignee.role() != Role.SUPPORT_MANAGER) {
            throw new ValidationException("INVALID_ASSIGNEE_ROLE", "The assignee must be a support user.");
        }
    }

    void ensureStartAllowed(Ticket ticket) {
        if (ticket.getStatus() != TicketStatus.ASSIGNED) {
            throw new ValidationException("INVALID_TICKET_TRANSITION", "The ticket must be in ASSIGNED status.");
        }
    }

    void ensureInformationRequestAllowed(Ticket ticket) {
        if (ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            throw new ValidationException("INVALID_TICKET_TRANSITION", "The ticket must be in IN_PROGRESS status.");
        }
    }

    void ensureResolveAllowed(Ticket ticket, String resolutionSummary) {
        if (ticket.getStatus() != TicketStatus.IN_PROGRESS && ticket.getStatus() != TicketStatus.WAITING_FOR_CUSTOMER) {
            throw new ValidationException("INVALID_TICKET_TRANSITION", "The ticket cannot be resolved from its current status.");
        }
        if (resolutionSummary == null || resolutionSummary.isBlank()) {
            throw new ValidationException("RESOLUTION_SUMMARY_REQUIRED", "The resolution summary is required.");
        }
    }

    void ensureCloseAllowed(Ticket ticket) {
        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new ValidationException("INVALID_TICKET_TRANSITION", "The ticket must be resolved before closing.");
        }
    }

    void ensureReopenRequestAllowed(Ticket ticket, String reason) {
        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new ValidationException("INVALID_TICKET_TRANSITION", "Only resolved tickets can be reopened.");
        }
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("REOPEN_REASON_REQUIRED", "The reopen reason is required.");
        }
    }

    void ensureCancelReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("CANCEL_REASON_REQUIRED", "The cancel reason is required.");
        }
    }

    void ensureCommentContent(String content) {
        if (content == null || content.isBlank()) {
            throw new ValidationException("COMMENT_CONTENT_REQUIRED", "The comment content is required.");
        }
    }

    String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
