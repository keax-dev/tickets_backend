package com.tickets.managementtickets.ticket.infrastructure.web.controller;

import com.tickets.managementtickets.identity.application.port.CurrentAuthenticatedUserProvider;
import com.tickets.managementtickets.shared.application.model.PageResponse;
import com.tickets.managementtickets.shared.application.model.SortDirection;
import com.tickets.managementtickets.ticket.application.query.TicketFilterRequest;
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
import com.tickets.managementtickets.shared.infrastructure.web.ApiProblemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Tickets", description = "Ticket query and command endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class TicketController {

    private final TicketService ticketService;
    private final CurrentAuthenticatedUserProvider currentUserProvider;

    public TicketController(TicketService ticketService, CurrentAuthenticatedUserProvider currentUserProvider) {
        this.ticketService = ticketService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    @Operation(summary = "List visible tickets", description = "Returns the page of tickets visible to the authenticated user after applying optional filters, pagination, and sorting.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ticket page returned successfully."),
        @ApiResponse(responseCode = "400", description = "Filter, pagination, or sort inputs are invalid.", content = @Content(schema = @Schema(implementation = ApiProblemResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiProblemResponse.class)))
    })
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
                new TicketFilterRequest(search, status, priority, categoryId, assignedAgentId, createdFrom, createdTo, page, size, sortBy, toSortDirection(direction))
            )
            .map(TicketSummaryResponse::from);
    }

    @PostMapping
    @Operation(summary = "Create a ticket", description = "Creates a new ticket for the authenticated requester and optionally uses an idempotency key to prevent duplicate creation.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ticket created successfully.", content = @Content(schema = @Schema(implementation = TicketDetailResponse.class))),
        @ApiResponse(responseCode = "400", description = "Request payload is invalid.", content = @Content(schema = @Schema(implementation = ApiProblemResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiProblemResponse.class))),
        @ApiResponse(responseCode = "404", description = "Referenced category or SLA policy was not found.", content = @Content(schema = @Schema(implementation = ApiProblemResponse.class))),
        @ApiResponse(responseCode = "409", description = "Idempotency or version conflict detected.", content = @Content(schema = @Schema(implementation = ApiProblemResponse.class))),
        @ApiResponse(responseCode = "422", description = "Business rule validation failed.", content = @Content(schema = @Schema(implementation = ApiProblemResponse.class)))
    })
    public TicketDetailResponse create(
        @Valid @RequestBody CreateTicketRequest request,
        @RequestHeader(name = "Idempotency-Key", required = false) @Size(max = 120) String idempotencyKey
    ) {
        return TicketDetailResponse.from(
            ticketService.create(
                currentUserProvider.requireCurrentUser(),
                new com.tickets.managementtickets.ticket.application.command.CreateTicketRequest(request.title(), request.description(), request.categoryId(), request.priority()),
                idempotencyKey
            )
        );
    }

    @GetMapping("/{ticketId}")
    @Operation(summary = "Get ticket detail", description = "Returns the full ticket detail visible to the authenticated user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ticket detail returned successfully.", content = @Content(schema = @Schema(implementation = TicketDetailResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiProblemResponse.class))),
        @ApiResponse(responseCode = "403", description = "The user cannot view this ticket.", content = @Content(schema = @Schema(implementation = ApiProblemResponse.class))),
        @ApiResponse(responseCode = "404", description = "Ticket not found.", content = @Content(schema = @Schema(implementation = ApiProblemResponse.class)))
    })
    public TicketDetailResponse getById(@PathVariable String ticketId) {
        return TicketDetailResponse.from(ticketService.getById(currentUserProvider.requireCurrentUser(), ticketId));
    }

    @PatchMapping("/{ticketId}")
    @Operation(summary = "Update ticket details", description = "Updates mutable ticket fields such as title, description, category, and optionally priority.")
    public TicketDetailResponse update(
        @PathVariable String ticketId,
        @Valid @RequestBody UpdateTicketRequest request
    ) {
        return TicketDetailResponse.from(
            ticketService.update(
                currentUserProvider.requireCurrentUser(),
                ticketId,
                new com.tickets.managementtickets.ticket.application.command.UpdateTicketRequest(request.version(), request.title(), request.description(), request.categoryId(), request.priority())
            )
        );
    }

    @PostMapping("/{ticketId}/assign")
    @Operation(summary = "Assign or reassign a ticket", description = "Assigns the ticket to a support user or changes its current assignee.")
    public TicketDetailResponse assign(
        @PathVariable String ticketId,
        @Valid @RequestBody AssignTicketRequest request
    ) {
        return TicketDetailResponse.from(
            ticketService.assign(
                currentUserProvider.requireCurrentUser(),
                ticketId,
                new com.tickets.managementtickets.ticket.application.command.AssignTicketRequest(request.version(), request.agentId())
            )
        );
    }

    @PostMapping("/{ticketId}/start")
    @Operation(summary = "Start working on a ticket", description = "Moves an assigned ticket into the in-progress state and records the first response if needed.")
    public TicketDetailResponse start(
        @PathVariable String ticketId,
        @Valid @RequestBody VersionedRequest request
    ) {
        return TicketDetailResponse.from(
            ticketService.start(
                currentUserProvider.requireCurrentUser(),
                ticketId,
                new com.tickets.managementtickets.ticket.application.command.VersionedRequest(request.version())
            )
        );
    }

    @PostMapping("/{ticketId}/request-information")
    @Operation(summary = "Request more information", description = "Adds a public request-for-information comment and pauses the resolution SLA while waiting for the requester.")
    public TicketDetailResponse requestInformation(
        @PathVariable String ticketId,
        @Valid @RequestBody RequestInformationRequest request
    ) {
        return TicketDetailResponse.from(
            ticketService.requestInformation(
                currentUserProvider.requireCurrentUser(),
                ticketId,
                new com.tickets.managementtickets.ticket.application.command.RequestInformationRequest(request.version(), request.content())
            )
        );
    }

    @PostMapping("/{ticketId}/resolve")
    @Operation(summary = "Resolve a ticket", description = "Marks a ticket as resolved and stores the resolution summary.")
    public TicketDetailResponse resolve(
        @PathVariable String ticketId,
        @Valid @RequestBody ResolveTicketRequest request
    ) {
        return TicketDetailResponse.from(
            ticketService.resolve(
                currentUserProvider.requireCurrentUser(),
                ticketId,
                new com.tickets.managementtickets.ticket.application.command.ResolveTicketRequest(request.version(), request.resolutionSummary())
            )
        );
    }

    @PostMapping("/{ticketId}/close")
    @Operation(summary = "Close a ticket", description = "Closes a resolved ticket once the requester confirms the solution.")
    public TicketDetailResponse close(
        @PathVariable String ticketId,
        @Valid @RequestBody VersionedRequest request
    ) {
        return TicketDetailResponse.from(
            ticketService.close(
                currentUserProvider.requireCurrentUser(),
                ticketId,
                new com.tickets.managementtickets.ticket.application.command.VersionedRequest(request.version())
            )
        );
    }

    @PostMapping("/{ticketId}/reopen")
    @Operation(summary = "Reopen a ticket", description = "Reopens a resolved ticket and recalculates its active resolution deadline.")
    public TicketDetailResponse reopen(
        @PathVariable String ticketId,
        @Valid @RequestBody ReopenTicketRequest request
    ) {
        return TicketDetailResponse.from(
            ticketService.reopen(
                currentUserProvider.requireCurrentUser(),
                ticketId,
                new com.tickets.managementtickets.ticket.application.command.ReopenTicketRequest(request.version(), request.reason())
            )
        );
    }

    @PostMapping("/{ticketId}/cancel")
    @Operation(summary = "Cancel a ticket", description = "Cancels a ticket that should no longer be worked on.")
    public TicketDetailResponse cancel(
        @PathVariable String ticketId,
        @Valid @RequestBody CancelTicketRequest request
    ) {
        return TicketDetailResponse.from(
            ticketService.cancel(
                currentUserProvider.requireCurrentUser(),
                ticketId,
                new com.tickets.managementtickets.ticket.application.command.CancelTicketRequest(request.version(), request.reason())
            )
        );
    }

    @GetMapping("/{ticketId}/comments")
    @Operation(summary = "List ticket comments", description = "Returns public comments for customers and both public/internal comments for authorized support users.")
    @ApiResponse(responseCode = "200", description = "Ticket comments returned successfully.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TicketCommentResponse.class))))
    public List<TicketCommentResponse> comments(@PathVariable String ticketId) {
        return ticketService.listComments(currentUserProvider.requireCurrentUser(), ticketId).stream()
            .map(TicketCommentResponse::from)
            .toList();
    }

    @PostMapping("/{ticketId}/comments")
    @Operation(summary = "Add a ticket comment", description = "Adds a public or internal comment to the ticket, subject to the caller permissions and ticket state.")
    public TicketCommentResponse addComment(
        @PathVariable String ticketId,
        @Valid @RequestBody AddCommentRequest request
    ) {
        return TicketCommentResponse.from(
            ticketService.addComment(
                currentUserProvider.requireCurrentUser(),
                ticketId,
                new com.tickets.managementtickets.ticket.application.command.AddCommentRequest(request.version(), request.content(), request.visibility())
            )
        );
    }

    @GetMapping("/{ticketId}/history")
    @Operation(summary = "List ticket history", description = "Returns the audit trail of ticket changes for users allowed to read audit history.")
    @ApiResponse(responseCode = "200", description = "Ticket history returned successfully.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TicketHistoryResponse.class))))
    public List<TicketHistoryResponse> history(@PathVariable String ticketId) {
        return ticketService.listHistory(currentUserProvider.requireCurrentUser(), ticketId).stream()
            .map(TicketHistoryResponse::from)
            .toList();
    }

    private SortDirection toSortDirection(Sort.Direction direction) {
        return direction == Sort.Direction.ASC ? SortDirection.ASC : SortDirection.DESC;
    }
}
