package com.tickets.managementtickets.ticket.application.service;

import com.tickets.managementtickets.category.domain.model.Category;
import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.domain.model.User;
import com.tickets.managementtickets.ticket.application.result.TicketCommentResponse;
import com.tickets.managementtickets.ticket.application.result.TicketDetailResponse;
import com.tickets.managementtickets.ticket.application.result.TicketHistoryResponse;
import com.tickets.managementtickets.ticket.application.result.TicketSummaryResponse;
import com.tickets.managementtickets.ticket.domain.model.Ticket;
import com.tickets.managementtickets.ticket.domain.model.TicketComment;
import com.tickets.managementtickets.ticket.domain.model.TicketHistory;

import java.util.Map;

final class TicketResponseMapper {

    private final TicketAccessPolicy accessPolicy;

    TicketResponseMapper(TicketAccessPolicy accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    TicketSummaryResponse toSummaryResponse(
        Ticket ticket,
        Map<String, User> usersById,
        Map<String, Category> categoriesById
    ) {
        return new TicketSummaryResponse(
            ticket.getId(),
            ticket.getCode(),
            ticket.getTitle(),
            ticket.getStatus(),
            ticket.getPriority(),
            ticket.getRequesterId(),
            displayName(usersById.get(ticket.getRequesterId())),
            ticket.getAssignedAgentId(),
            displayName(usersById.get(ticket.getAssignedAgentId())),
            ticket.getCategoryId(),
            categoryName(ticket, categoriesById),
            ticket.getResolutionDueAt(),
            ticket.isSlaFirstResponseBreached(),
            ticket.isSlaResolutionBreached(),
            ticket.getCreatedAt(),
            ticket.getUpdatedAt(),
            ticket.getVersion()
        );
    }

    TicketDetailResponse toDetailResponse(
        Ticket ticket,
        AuthenticatedUser currentUser,
        Map<String, User> usersById,
        Map<String, Category> categoriesById
    ) {
        return new TicketDetailResponse(
            ticket.getId(),
            ticket.getCode(),
            ticket.getTitle(),
            ticket.getDescription(),
            ticket.getStatus(),
            ticket.getPriority(),
            ticket.getRequesterId(),
            displayName(usersById.get(ticket.getRequesterId())),
            ticket.getAssignedAgentId(),
            displayName(usersById.get(ticket.getAssignedAgentId())),
            ticket.getCategoryId(),
            categoryName(ticket, categoriesById),
            ticket.getFirstResponseDueAt(),
            ticket.getResolutionDueAt(),
            ticket.getFirstRespondedAt(),
            ticket.getResolvedAt(),
            ticket.getClosedAt(),
            ticket.getCancelledAt(),
            ticket.getSlaPausedAt(),
            ticket.getAccumulatedPausedSeconds(),
            ticket.isSlaFirstResponseBreached(),
            ticket.isSlaResolutionBreached(),
            ticket.getResolutionSummary(),
            ticket.getCreatedAt(),
            ticket.getUpdatedAt(),
            ticket.getVersion(),
            accessPolicy.availableActions(currentUser, ticket)
        );
    }

    TicketCommentResponse toCommentResponse(TicketComment comment, Map<String, User> usersById) {
        return new TicketCommentResponse(
            comment.id(),
            comment.ticketId(),
            comment.authorId(),
            displayName(usersById.get(comment.authorId())),
            comment.content(),
            comment.visibility(),
            comment.createdAt(),
            comment.updatedAt()
        );
    }

    TicketHistoryResponse toHistoryResponse(TicketHistory entry, Map<String, User> usersById) {
        return new TicketHistoryResponse(
            entry.id(),
            entry.action(),
            entry.performedBy(),
            displayName(usersById.get(entry.performedBy())),
            entry.previousValue(),
            entry.newValue(),
            entry.metadataJson(),
            entry.createdAt()
        );
    }

    private String categoryName(Ticket ticket, Map<String, Category> categoriesById) {
        Category category = categoriesById.get(ticket.getCategoryId());
        return category == null ? null : category.name();
    }

    private String displayName(User user) {
        if (user == null) {
            return null;
        }
        return user.displayName();
    }
}
