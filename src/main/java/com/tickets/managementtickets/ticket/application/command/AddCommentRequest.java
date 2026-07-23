package com.tickets.managementtickets.ticket.application.command;

import com.tickets.managementtickets.ticket.domain.model.CommentVisibility;

public record AddCommentRequest(long version, String content, CommentVisibility visibility) {
}
