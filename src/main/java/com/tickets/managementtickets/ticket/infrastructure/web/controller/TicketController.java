package com.tickets.managementtickets.ticket.infrastructure.web.controller;

import com.tickets.managementtickets.identity.application.port.CurrentAuthenticatedUserProvider;
import com.tickets.managementtickets.shared.application.model.PageResponse;
import com.tickets.managementtickets.shared.application.model.SortDirection;
import com.tickets.managementtickets.ticket.application.service.TicketService;
import com.tickets.managementtickets.ticket.infrastructure.web.dto.AddCommentRequest;
import com.tickets.managementtickets.ticket.infrastructure.web.dto.AssignTicketRequest;
import com.tickets.managementtickets.ticket.infrastructure.web.dto.CancelTicketRequest;
import com.tickets.managementtickets.ticket.infrastructure.web.dto.CreateTicketRequest;
import com.tickets.managementtickets.ticket.infrastructure.web.dto.ReopenTicketRequest;
import com.tickets.managementtickets.ticket.infrastructure.web.dto.RequestInformationRequest;
import com.tickets.managementtickets.ticket.infrastructure.web.dto.ResolveTicketRequest;
import com.tickets.managementtickets.ticket.infrastructure.web.dto.TicketCommentResponse;
import com.tickets.managementtickets.ticket.infrastructure.web.dto.TicketDetailResponse;
import com.tickets.managementtickets.ticket.infrastructure.web.dto.TicketHistoryResponse;
import com.tickets.managementtickets.ticket.infrastructure.web.dto.TicketSummaryResponse;
import com.tickets.managementtickets.ticket.infrastructure.web.dto.UpdateTicketRequest;
import com.tickets.managementtickets.ticket.infrastructure.web.dto.VersionedRequest;
import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final CurrentAuthenticatedUserProvider currentUserProvider;

    public TicketController(TicketService ticketService, CurrentAuthenticatedUserProvider currentUserProvider) {
        this.ticketService = ticketService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public PageResponse<TicketSummaryResponse> list(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) TicketStatus status,
        @RequestParam(required = false) TicketPriority priority,
        @RequestParam(required = false) String categoryId,
        @RequestParam(required = false) String assignedAgentId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
        @RequestParam(defaultValue = "0") @PositiveOrZero int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        return ticketService
            .list(
                currentUserProvider.requireCurrentUser(),
                new TicketService.TicketFilterRequest(search, status, priority, categoryId, assignedAgentId, createdFrom, createdTo, page, size, sortBy, toSortDirection(direction))
            )
            .map(TicketSummaryResponse::from);
    }

    @PostMapping
    public TicketDetailResponse create(
        @Valid @RequestBody CreateTicketRequest request,
        @RequestHeader(name = "Idempotency-Key", required = false) @Size(max = 120) String idempotencyKey
    ) {
        return TicketDetailResponse.from(
            ticketService.create(
                currentUserProvider.requireCurrentUser(),
                new TicketService.CreateTicketRequest(request.title(), request.description(), request.categoryId(), request.priority()),
                idempotencyKey
            )
        );
    }

    @GetMapping("/{ticketId}")
    public TicketDetailResponse getById(@PathVariable String ticketId) {
        return TicketDetailResponse.from(ticketService.getById(currentUserProvider.requireCurrentUser(), ticketId));
    }

    @PatchMapping("/{ticketId}")
    public TicketDetailResponse update(
        @PathVariable String ticketId,
        @Valid @RequestBody UpdateTicketRequest request
    ) {
        return TicketDetailResponse.from(
            ticketService.update(
                currentUserProvider.requireCurrentUser(),
                ticketId,
                new TicketService.UpdateTicketRequest(request.version(), request.title(), request.description(), request.categoryId(), request.priority())
            )
        );
    }

    @PostMapping("/{ticketId}/assign")
    public TicketDetailResponse assign(
        @PathVariable String ticketId,
        @Valid @RequestBody AssignTicketRequest request
    ) {
        return TicketDetailResponse.from(
            ticketService.assign(
                currentUserProvider.requireCurrentUser(),
                ticketId,
                new TicketService.AssignTicketRequest(request.version(), request.agentId())
            )
        );
    }

    @PostMapping("/{ticketId}/start")
    public TicketDetailResponse start(
        @PathVariable String ticketId,
        @Valid @RequestBody VersionedRequest request
    ) {
        return TicketDetailResponse.from(
            ticketService.start(
                currentUserProvider.requireCurrentUser(),
                ticketId,
                new TicketService.VersionedRequest(request.version())
            )
        );
    }

    @PostMapping("/{ticketId}/request-information")
    public TicketDetailResponse requestInformation(
        @PathVariable String ticketId,
        @Valid @RequestBody RequestInformationRequest request
    ) {
        return TicketDetailResponse.from(
            ticketService.requestInformation(
                currentUserProvider.requireCurrentUser(),
                ticketId,
                new TicketService.RequestInformationRequest(request.version(), request.content())
            )
        );
    }

    @PostMapping("/{ticketId}/resolve")
    public TicketDetailResponse resolve(
        @PathVariable String ticketId,
        @Valid @RequestBody ResolveTicketRequest request
    ) {
        return TicketDetailResponse.from(
            ticketService.resolve(
                currentUserProvider.requireCurrentUser(),
                ticketId,
                new TicketService.ResolveTicketRequest(request.version(), request.resolutionSummary())
            )
        );
    }

    @PostMapping("/{ticketId}/close")
    public TicketDetailResponse close(
        @PathVariable String ticketId,
        @Valid @RequestBody VersionedRequest request
    ) {
        return TicketDetailResponse.from(
            ticketService.close(
                currentUserProvider.requireCurrentUser(),
                ticketId,
                new TicketService.VersionedRequest(request.version())
            )
        );
    }

    @PostMapping("/{ticketId}/reopen")
    public TicketDetailResponse reopen(
        @PathVariable String ticketId,
        @Valid @RequestBody ReopenTicketRequest request
    ) {
        return TicketDetailResponse.from(
            ticketService.reopen(
                currentUserProvider.requireCurrentUser(),
                ticketId,
                new TicketService.ReopenTicketRequest(request.version(), request.reason())
            )
        );
    }

    @PostMapping("/{ticketId}/cancel")
    public TicketDetailResponse cancel(
        @PathVariable String ticketId,
        @Valid @RequestBody CancelTicketRequest request
    ) {
        return TicketDetailResponse.from(
            ticketService.cancel(
                currentUserProvider.requireCurrentUser(),
                ticketId,
                new TicketService.CancelTicketRequest(request.version(), request.reason())
            )
        );
    }

    @GetMapping("/{ticketId}/comments")
    public List<TicketCommentResponse> comments(@PathVariable String ticketId) {
        return ticketService.listComments(currentUserProvider.requireCurrentUser(), ticketId).stream()
            .map(TicketCommentResponse::from)
            .toList();
    }

    @PostMapping("/{ticketId}/comments")
    public TicketCommentResponse addComment(
        @PathVariable String ticketId,
        @Valid @RequestBody AddCommentRequest request
    ) {
        return TicketCommentResponse.from(
            ticketService.addComment(
                currentUserProvider.requireCurrentUser(),
                ticketId,
                new TicketService.AddCommentRequest(request.version(), request.content(), request.visibility())
            )
        );
    }

    @GetMapping("/{ticketId}/history")
    public List<TicketHistoryResponse> history(@PathVariable String ticketId) {
        return ticketService.listHistory(currentUserProvider.requireCurrentUser(), ticketId).stream()
            .map(TicketHistoryResponse::from)
            .toList();
    }

    private SortDirection toSortDirection(Sort.Direction direction) {
        return direction == Sort.Direction.ASC ? SortDirection.ASC : SortDirection.DESC;
    }
}
