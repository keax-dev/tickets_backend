package com.tickets.managementtickets.ticket.infrastructure.web.dto;

import com.tickets.managementtickets.ticket.domain.model.CommentVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AddCommentRequest(
    @PositiveOrZero long version,
    @NotBlank @Size(max = 4000) String content,
    @NotNull CommentVisibility visibility
) {
}
