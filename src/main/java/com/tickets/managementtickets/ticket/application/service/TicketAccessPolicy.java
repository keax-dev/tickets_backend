package com.tickets.managementtickets.ticket.application.service;

import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.shared.application.exception.ForbiddenException;
import com.tickets.managementtickets.shared.application.exception.ValidationException;
import com.tickets.managementtickets.ticket.application.port.TicketVisibility;
import com.tickets.managementtickets.ticket.domain.model.Ticket;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class TicketAccessPolicy {

    private static final int REQUESTER_REOPEN_WINDOW_DAYS = 7;
    private final TicketVisibilityPolicy visibilityPolicy;

    TicketAccessPolicy() {
        this.visibilityPolicy = new TicketVisibilityPolicy();
    }

    TicketVisibility resolveVisibility(AuthenticatedUser currentUser) {
        return visibilityPolicy.resolveFor(currentUser);
    }

    void ensureCanViewTicket(AuthenticatedUser currentUser, Ticket ticket) {
        if (currentUser.hasPermission(Permission.TICKET_READ_ALL)) {
            return;
        }
        if (currentUser.hasPermission(Permission.TICKET_READ_ASSIGNED)
            && (currentUser.id().equals(ticket.getAssignedAgentId()) || ticket.getAssignedAgentId() == null)) {
            return;
        }
        if (currentUser.hasPermission(Permission.TICKET_READ_OWN) && currentUser.id().equals(ticket.getRequesterId())) {
            return;
        }
        throw new ForbiddenException("ACCESS_DENIED", "You do not have permission to view this ticket.");
    }

    void ensureCanUpdateTicket(AuthenticatedUser currentUser, Ticket ticket) {
        boolean privilegedCanUpdate = currentUser.hasPermission(Permission.TICKET_UPDATE)
            && currentUser.hasPermission(Permission.TICKET_READ_ALL);
        if (privilegedCanUpdate) {
            return;
        }

        boolean requesterCanUpdate = currentUser.hasPermission(Permission.TICKET_UPDATE)
            && currentUser.id().equals(ticket.getRequesterId())
            && ticket.getStatus() == TicketStatus.CREATED;
        if (requesterCanUpdate) {
            return;
        }

        throw new ForbiddenException("ACCESS_DENIED", "You do not have permission to update this ticket.");
    }

    void ensureCanOperateTicket(AuthenticatedUser currentUser, Ticket ticket) {
        if (currentUser.hasPermission(Permission.TICKET_READ_ALL)) {
            return;
        }
        if (currentUser.hasPermission(Permission.TICKET_CHANGE_STATUS)
            && currentUser.id().equals(ticket.getAssignedAgentId())) {
            return;
        }
        throw new ForbiddenException("ACCESS_DENIED", "You do not have permission to operate on this ticket.");
    }

    void ensureCanCloseTicket(AuthenticatedUser currentUser, Ticket ticket) {
        boolean canClose = currentUser.hasPermission(Permission.TICKET_CLOSE) && (
            currentUser.hasPermission(Permission.TICKET_READ_ALL) || ticket.getRequesterId().equals(currentUser.id())
        );
        if (!canClose) {
            throw new ForbiddenException("ACCESS_DENIED", "You do not have permission to close this ticket.");
        }
    }

    void ensureCanReopenTicket(AuthenticatedUser currentUser, Ticket ticket, Instant now) {
        boolean isRequester = currentUser.hasPermission(Permission.TICKET_REOPEN)
            && ticket.getRequesterId().equals(currentUser.id());
        boolean isPrivileged = currentUser.hasPermission(Permission.TICKET_REOPEN)
            && currentUser.hasPermission(Permission.TICKET_READ_ALL);
        if (!isRequester && !isPrivileged) {
            throw new ForbiddenException("ACCESS_DENIED", "You do not have permission to reopen this ticket.");
        }
        if (isRequester
            && ticket.getResolvedAt() != null
            && ticket.getResolvedAt().isBefore(now.minus(REQUESTER_REOPEN_WINDOW_DAYS, ChronoUnit.DAYS))) {
            throw new ValidationException("REOPEN_WINDOW_EXPIRED", "The ticket can no longer be reopened.");
        }
    }

    void ensureCanCancelTicket(AuthenticatedUser currentUser, Ticket ticket) {
        boolean requesterCanCancel = currentUser.hasPermission(Permission.TICKET_CANCEL)
            && ticket.getRequesterId().equals(currentUser.id())
            && ticket.getStatus() == TicketStatus.CREATED;
        boolean privilegedCanCancel = currentUser.hasPermission(Permission.TICKET_CANCEL)
            && currentUser.hasPermission(Permission.TICKET_READ_ALL)
            && Set.of(
                TicketStatus.CREATED,
                TicketStatus.ASSIGNED,
                TicketStatus.IN_PROGRESS,
                TicketStatus.WAITING_FOR_CUSTOMER
            ).contains(ticket.getStatus());

        if (!requesterCanCancel && !privilegedCanCancel) {
            throw new ForbiddenException("ACCESS_DENIED", "You do not have permission to cancel this ticket.");
        }
    }

    boolean canSeeInternalComments(AuthenticatedUser currentUser) {
        return currentUser.hasPermission(Permission.COMMENT_READ_INTERNAL);
    }

    List<String> availableActions(AuthenticatedUser currentUser, Ticket ticket) {
        List<String> actions = new ArrayList<>();
        if (currentUser.hasPermission(Permission.TICKET_READ_ALL) || currentUser.id().equals(ticket.getRequesterId())) {
            if (ticket.getStatus() == TicketStatus.CREATED) {
                actions.add("update");
                actions.add("cancel");
            }
            if (ticket.getStatus() == TicketStatus.RESOLVED) {
                actions.add("close");
                actions.add("reopen");
            }
        }
        if (!ticket.isTerminal() && (currentUser.hasPermission(Permission.TICKET_ASSIGN) || currentUser.hasPermission(Permission.TICKET_REASSIGN))) {
            actions.add("assign");
        }
        if (currentUser.hasPermission(Permission.TICKET_CHANGE_STATUS)
            && (currentUser.hasPermission(Permission.TICKET_READ_ALL) || currentUser.id().equals(ticket.getAssignedAgentId()))) {
            if (ticket.getStatus() == TicketStatus.ASSIGNED) {
                actions.add("start");
            }
            if (ticket.getStatus() == TicketStatus.IN_PROGRESS) {
                actions.add("request-information");
                actions.add("resolve");
            }
            if (ticket.getStatus() == TicketStatus.WAITING_FOR_CUSTOMER) {
                actions.add("resolve");
            }
        }
        if (!ticket.isTerminal() && (
            currentUser.hasPermission(Permission.COMMENT_CREATE_PUBLIC) ||
                currentUser.hasPermission(Permission.COMMENT_CREATE_INTERNAL)
        )) {
            actions.add("comment");
        }
        return actions;
    }
}
