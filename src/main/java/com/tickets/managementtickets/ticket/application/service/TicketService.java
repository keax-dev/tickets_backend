package com.tickets.managementtickets.ticket.application.service;

import com.tickets.managementtickets.category.application.port.CategoryRepositoryPort;
import com.tickets.managementtickets.category.domain.model.Category;
import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.application.port.UserRepositoryPort;
import com.tickets.managementtickets.identity.application.service.AuthorizationService;
import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.identity.domain.model.Role;
import com.tickets.managementtickets.identity.domain.model.User;
import com.tickets.managementtickets.notification.application.port.NotificationRepositoryPort;
import com.tickets.managementtickets.notification.domain.model.Notification;
import com.tickets.managementtickets.notification.domain.model.NotificationType;
import com.tickets.managementtickets.shared.application.exception.BadRequestException;
import com.tickets.managementtickets.shared.application.exception.ConflictException;
import com.tickets.managementtickets.shared.application.exception.ForbiddenException;
import com.tickets.managementtickets.shared.application.exception.NotFoundException;
import com.tickets.managementtickets.shared.application.exception.UnauthorizedException;
import com.tickets.managementtickets.shared.application.exception.ValidationException;
import com.tickets.managementtickets.shared.application.model.PageResponse;
import com.tickets.managementtickets.shared.application.model.SortDirection;
import com.tickets.managementtickets.shared.application.port.HashingService;
import com.tickets.managementtickets.shared.application.port.JsonCodec;
import com.tickets.managementtickets.shared.application.port.TransactionRunner;
import com.tickets.managementtickets.sla.application.port.SlaPolicyRepositoryPort;
import com.tickets.managementtickets.sla.domain.model.SlaPolicy;
import com.tickets.managementtickets.ticket.application.port.IdempotencyPolicy;
import com.tickets.managementtickets.ticket.application.port.IdempotencyRecordRepositoryPort;
import com.tickets.managementtickets.ticket.application.port.TicketCodeGenerator;
import com.tickets.managementtickets.ticket.application.port.TicketCommentRepositoryPort;
import com.tickets.managementtickets.ticket.application.port.TicketHistoryRepositoryPort;
import com.tickets.managementtickets.ticket.application.port.TicketLifecyclePolicy;
import com.tickets.managementtickets.ticket.application.port.TicketQuery;
import com.tickets.managementtickets.ticket.application.port.TicketRepositoryPort;
import com.tickets.managementtickets.ticket.application.port.TicketVisibility;
import com.tickets.managementtickets.ticket.domain.model.CommentVisibility;
import com.tickets.managementtickets.ticket.domain.model.IdempotencyRecord;
import com.tickets.managementtickets.ticket.domain.model.Ticket;
import com.tickets.managementtickets.ticket.domain.model.TicketComment;
import com.tickets.managementtickets.ticket.domain.model.TicketHistory;
import com.tickets.managementtickets.ticket.domain.model.TicketHistoryAction;
import com.tickets.managementtickets.ticket.domain.model.TicketPriority;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TicketService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String SYSTEM_ACTOR = "system-auto-close";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "createdAt",
        "updatedAt",
        "code",
        "title",
        "status",
        "priority",
        "resolutionDueAt"
    );

    private final TicketRepositoryPort ticketRepository;
    private final TicketCommentRepositoryPort ticketCommentRepository;
    private final TicketHistoryRepositoryPort ticketHistoryRepository;
    private final IdempotencyRecordRepositoryPort idempotencyRecordRepository;
    private final UserRepositoryPort userRepository;
    private final CategoryRepositoryPort categoryRepository;
    private final SlaPolicyRepositoryPort slaPolicyRepository;
    private final NotificationRepositoryPort notificationRepository;
    private final TicketCodeGenerator ticketCodeGenerator;
    private final AuthorizationService authorizationService;
    private final HashingService hashingService;
    private final JsonCodec jsonCodec;
    private final Clock clock;
    private final IdempotencyPolicy idempotencyPolicy;
    private final TicketLifecyclePolicy ticketLifecyclePolicy;
    private final TransactionRunner transactionRunner;

    public TicketService(
        TicketRepositoryPort ticketRepository,
        TicketCommentRepositoryPort ticketCommentRepository,
        TicketHistoryRepositoryPort ticketHistoryRepository,
        IdempotencyRecordRepositoryPort idempotencyRecordRepository,
        UserRepositoryPort userRepository,
        CategoryRepositoryPort categoryRepository,
        SlaPolicyRepositoryPort slaPolicyRepository,
        NotificationRepositoryPort notificationRepository,
        TicketCodeGenerator ticketCodeGenerator,
        AuthorizationService authorizationService,
        HashingService hashingService,
        JsonCodec jsonCodec,
        Clock clock,
        IdempotencyPolicy idempotencyPolicy,
        TicketLifecyclePolicy ticketLifecyclePolicy,
        TransactionRunner transactionRunner
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketCommentRepository = ticketCommentRepository;
        this.ticketHistoryRepository = ticketHistoryRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.slaPolicyRepository = slaPolicyRepository;
        this.notificationRepository = notificationRepository;
        this.ticketCodeGenerator = ticketCodeGenerator;
        this.authorizationService = authorizationService;
        this.hashingService = hashingService;
        this.jsonCodec = jsonCodec;
        this.clock = clock;
        this.idempotencyPolicy = idempotencyPolicy;
        this.ticketLifecyclePolicy = ticketLifecyclePolicy;
        this.transactionRunner = transactionRunner;
    }

    public PageResponse<TicketSummaryResponse> list(AuthenticatedUser currentUser, TicketFilterRequest filterRequest) {
        return transactionRunner.readOnly(() -> {
            validateTicketFilter(filterRequest);

            PageResponse<Ticket> page = ticketRepository.findAll(new TicketQuery(
                filterRequest.search(),
                filterRequest.status(),
                filterRequest.priority(),
                filterRequest.categoryId(),
                filterRequest.assignedAgentId(),
                filterRequest.createdFrom(),
                filterRequest.createdTo(),
                filterRequest.page(),
                filterRequest.size(),
                filterRequest.sortBy(),
                filterRequest.direction(),
                resolveVisibility(currentUser),
                currentUser.id()
            ));
            Map<String, User> usersById = loadUsersById(
                page.content().stream()
                    .flatMap(ticket -> Stream.of(ticket.getRequesterId(), ticket.getAssignedAgentId()))
                    .filter(Objects::nonNull)
                    .toList()
            );
            Map<String, Category> categoriesById = loadCategoriesById(page.content().stream().map(Ticket::getCategoryId).toList());

            return page.map(ticket -> toSummaryResponse(ticket, usersById, categoriesById));
        });
    }

    public TicketDetailResponse getById(AuthenticatedUser currentUser, String ticketId) {
        return transactionRunner.readOnly(() -> {
            Ticket ticket = findTicket(ticketId);
            ensureCanViewTicket(currentUser, ticket);
            return toDetailResponse(ticket, currentUser);
        });
    }

    public TicketDetailResponse create(AuthenticatedUser currentUser, CreateTicketRequest request, String idempotencyKey) {
        return transactionRunner.required(() -> {
            authorizationService.requirePermission(currentUser, Permission.TICKET_CREATE);
            if (idempotencyKey == null || idempotencyKey.isBlank()) {
                throw new ValidationException("IDEMPOTENCY_KEY_REQUIRED", "The idempotency key is required.");
            }

            User requester = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new UnauthorizedException(
                    "AUTHENTICATED_USER_NOT_FOUND",
                    "The authenticated user no longer exists. Please sign in again."
                ));

            String requestHash = hashingService.hash(currentUser.id() + "|" + normalize(request.title()) + "|" + normalize(request.description()) + "|" + request.categoryId() + "|" + request.priority().name());
            Optional<IdempotencyRecord> existingRecord = idempotencyRecordRepository.findByIdempotencyKeyAndUserId(idempotencyKey, currentUser.id());
            if (existingRecord.isPresent()) {
                IdempotencyRecord record = existingRecord.get();
                if (!record.requestHash().equals(requestHash)) {
                    throw new ConflictException("IDEMPOTENCY_KEY_CONFLICT", "The idempotency key was already used with a different payload.");
                }
                return deserializeStoredResponse(record.responseBody());
            }

            Category category = findActiveCategory(request.categoryId());
            SlaPolicy slaPolicy = findActiveSlaPolicy(request.priority());

            Instant now = clock.instant();
            Ticket ticket = Ticket.create(
                ticketCodeGenerator.nextCode(now),
                normalize(request.title()),
                normalize(request.description()),
                request.priority(),
                requester.id(),
                category.id(),
                slaPolicy,
                now
            );
            ticket = ticketRepository.save(ticket);

            addHistory(ticket.getId(), TicketHistoryAction.CREATED, currentUser.id(), null, null, "{\"code\":\"" + ticket.getCode() + "\"}");
            notifySupportUsers(
                NotificationType.TICKET_CREATED,
                "Nuevo ticket creado",
                "Se creo el ticket " + ticket.getCode() + ".",
                ticket.getId()
            );

            TicketDetailResponse response = toDetailResponse(ticket, currentUser);
            storeIdempotencyRecord(idempotencyKey, currentUser.id(), requestHash, ticket.getId(), response);
            return response;
        });
    }

    public TicketDetailResponse update(AuthenticatedUser currentUser, String ticketId, UpdateTicketRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            ensureCanUpdateTicket(currentUser, ticket);
            ensureVersion(ticket, request.version());

            String previousTitle = ticket.getTitle();
            String previousDescription = ticket.getDescription();
            String previousCategoryId = ticket.getCategoryId();
            TicketPriority previousPriority = ticket.getPriority();

            String requestedCategoryId = null;
            if (request.categoryId() != null && !request.categoryId().isBlank()) {
                requestedCategoryId = findActiveCategory(request.categoryId()).id();
            }
            ticket.updateDetails(normalize(request.title()), normalize(request.description()), requestedCategoryId);
            if (request.priority() != null && request.priority() != ticket.getPriority()) {
                ticket.changePriority(request.priority(), findActiveSlaPolicy(request.priority()), clock.instant());
            }

            ticket = ticketRepository.save(ticket);

            if (!Objects.equals(previousTitle, ticket.getTitle()) || !Objects.equals(previousDescription, ticket.getDescription())) {
                addHistory(ticket.getId(), TicketHistoryAction.UPDATED, currentUser.id(), previousTitle, ticket.getTitle(), null);
            }
            if (!Objects.equals(previousCategoryId, ticket.getCategoryId())) {
                addHistory(ticket.getId(), TicketHistoryAction.CATEGORY_CHANGED, currentUser.id(), previousCategoryId, ticket.getCategoryId(), null);
            }
            if (previousPriority != ticket.getPriority()) {
                addHistory(ticket.getId(), TicketHistoryAction.PRIORITY_CHANGED, currentUser.id(), previousPriority.name(), ticket.getPriority().name(), null);
            }

            return toDetailResponse(ticket, currentUser);
        });
    }

    public TicketDetailResponse assign(AuthenticatedUser currentUser, String ticketId, AssignTicketRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            authorizationService.requirePermission(currentUser, ticket.getAssignedAgentId() == null ? Permission.TICKET_ASSIGN : Permission.TICKET_REASSIGN);
            ensureVersion(ticket, request.version());
            ensureNotTerminal(ticket);

            User assignee = userRepository.findById(request.agentId())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "The assignee could not be found."));
            if (!assignee.active()) {
                throw new ValidationException("ASSIGNEE_INACTIVE", "The assignee must be active.");
            }
            if (assignee.role() != Role.SUPPORT_AGENT && assignee.role() != Role.SUPPORT_MANAGER) {
                throw new ValidationException("INVALID_ASSIGNEE_ROLE", "The assignee must be a support user.");
            }

            String previousAgentId = ticket.getAssignedAgentId();
            ticket.assign(assignee.id());
            ticket = ticketRepository.save(ticket);

            addHistory(ticket.getId(), previousAgentId == null ? TicketHistoryAction.ASSIGNED : TicketHistoryAction.REASSIGNED, currentUser.id(), previousAgentId, assignee.id(), null);
            notifyUser(assignee.id(), NotificationType.TICKET_ASSIGNED, "Ticket asignado", "Se te asigno el ticket " + ticket.getCode() + ".", ticket.getId());
            return toDetailResponse(ticket, currentUser);
        });
    }

    public TicketDetailResponse start(AuthenticatedUser currentUser, String ticketId, VersionedRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            ensureCanOperateTicket(currentUser, ticket);
            ensureVersion(ticket, request.version());
            if (ticket.getStatus() != TicketStatus.ASSIGNED) {
                throw new ValidationException("INVALID_TICKET_TRANSITION", "The ticket must be in ASSIGNED status.");
            }

            ticket.start(clock.instant());
            ticket = ticketRepository.save(ticket);
            addHistory(ticket.getId(), TicketHistoryAction.STARTED, currentUser.id(), TicketStatus.ASSIGNED.name(), TicketStatus.IN_PROGRESS.name(), null);
            return toDetailResponse(ticket, currentUser);
        });
    }

    public TicketDetailResponse requestInformation(AuthenticatedUser currentUser, String ticketId, RequestInformationRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            ensureCanOperateTicket(currentUser, ticket);
            ensureVersion(ticket, request.version());
            if (ticket.getStatus() != TicketStatus.IN_PROGRESS) {
                throw new ValidationException("INVALID_TICKET_TRANSITION", "The ticket must be in IN_PROGRESS status.");
            }
            addCommentInternal(currentUser, ticket, request.content(), CommentVisibility.PUBLIC);
            ticket.requestInformation(clock.instant());
            ticket = ticketRepository.save(ticket);
            addHistory(ticket.getId(), TicketHistoryAction.REQUESTED_INFORMATION, currentUser.id(), TicketStatus.IN_PROGRESS.name(), TicketStatus.WAITING_FOR_CUSTOMER.name(), null);
            notifyUser(ticket.getRequesterId(), NotificationType.INFORMATION_REQUESTED, "Se requiere informacion", "Hay una solicitud de informacion en el ticket " + ticket.getCode() + ".", ticket.getId());
            return toDetailResponse(ticket, currentUser);
        });
    }

    public TicketDetailResponse resolve(AuthenticatedUser currentUser, String ticketId, ResolveTicketRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            ensureCanOperateTicket(currentUser, ticket);
            ensureVersion(ticket, request.version());
            if (ticket.getStatus() != TicketStatus.IN_PROGRESS && ticket.getStatus() != TicketStatus.WAITING_FOR_CUSTOMER) {
                throw new ValidationException("INVALID_TICKET_TRANSITION", "The ticket cannot be resolved from its current status.");
            }
            if (request.resolutionSummary() == null || request.resolutionSummary().isBlank()) {
                throw new ValidationException("RESOLUTION_SUMMARY_REQUIRED", "The resolution summary is required.");
            }

            ticket.resolve(normalize(request.resolutionSummary()), clock.instant());
            ticket = ticketRepository.save(ticket);
            addHistory(ticket.getId(), TicketHistoryAction.RESOLVED, currentUser.id(), null, ticket.getResolutionSummary(), null);
            notifyUser(ticket.getRequesterId(), NotificationType.TICKET_RESOLVED, "Ticket resuelto", "El ticket " + ticket.getCode() + " fue resuelto.", ticket.getId());
            return toDetailResponse(ticket, currentUser);
        });
    }

    public TicketDetailResponse close(AuthenticatedUser currentUser, String ticketId, VersionedRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            ensureVersion(ticket, request.version());
            boolean canClose = currentUser.hasPermission(Permission.TICKET_CLOSE) && (
                currentUser.hasPermission(Permission.TICKET_READ_ALL) || ticket.getRequesterId().equals(currentUser.id())
            );
            if (!canClose) {
                throw new ForbiddenException("ACCESS_DENIED", "You do not have permission to close this ticket.");
            }
            if (ticket.getStatus() != TicketStatus.RESOLVED) {
                throw new ValidationException("INVALID_TICKET_TRANSITION", "The ticket must be resolved before closing.");
            }

            ticket.close(clock.instant());
            ticket = ticketRepository.save(ticket);
            addHistory(ticket.getId(), TicketHistoryAction.CLOSED, currentUser.id(), null, null, null);
            notifyUser(ticket.getRequesterId(), NotificationType.TICKET_CLOSED, "Ticket cerrado", "El ticket " + ticket.getCode() + " fue cerrado.", ticket.getId());
            return toDetailResponse(ticket, currentUser);
        });
    }

    public TicketDetailResponse reopen(AuthenticatedUser currentUser, String ticketId, ReopenTicketRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            ensureVersion(ticket, request.version());
            if (ticket.getStatus() != TicketStatus.RESOLVED) {
                throw new ValidationException("INVALID_TICKET_TRANSITION", "Only resolved tickets can be reopened.");
            }
            if (request.reason() == null || request.reason().isBlank()) {
                throw new ValidationException("REOPEN_REASON_REQUIRED", "The reopen reason is required.");
            }

            boolean isRequester = ticket.getRequesterId().equals(currentUser.id());
            boolean isPrivileged = currentUser.hasPermission(Permission.TICKET_REOPEN) && currentUser.hasPermission(Permission.TICKET_READ_ALL);
            if (!isRequester && !isPrivileged) {
                throw new ForbiddenException("ACCESS_DENIED", "You do not have permission to reopen this ticket.");
            }
            if (isRequester && ticket.getResolvedAt() != null && ticket.getResolvedAt().isBefore(clock.instant().minus(7, ChronoUnit.DAYS))) {
                throw new ValidationException("REOPEN_WINDOW_EXPIRED", "The ticket can no longer be reopened.");
            }

            ticket.reopen(clock.instant(), findActiveSlaPolicy(ticket.getPriority()));
            ticket = ticketRepository.save(ticket);
            addHistory(ticket.getId(), TicketHistoryAction.REOPENED, currentUser.id(), TicketStatus.RESOLVED.name(), TicketStatus.IN_PROGRESS.name(), "{\"reason\":\"" + escapeJson(request.reason()) + "\"}");
            if (ticket.getAssignedAgentId() != null) {
                notifyUser(ticket.getAssignedAgentId(), NotificationType.TICKET_REOPENED, "Ticket reabierto", "El ticket " + ticket.getCode() + " fue reabierto.", ticket.getId());
            }
            return toDetailResponse(ticket, currentUser);
        });
    }

    public TicketDetailResponse cancel(AuthenticatedUser currentUser, String ticketId, CancelTicketRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            ensureVersion(ticket, request.version());
            if (request.reason() == null || request.reason().isBlank()) {
                throw new ValidationException("CANCEL_REASON_REQUIRED", "The cancel reason is required.");
            }

            boolean requesterCanCancel = ticket.getRequesterId().equals(currentUser.id()) && ticket.getStatus() == TicketStatus.CREATED;
            boolean privilegedCanCancel = currentUser.hasPermission(Permission.TICKET_CANCEL)
                && currentUser.hasPermission(Permission.TICKET_READ_ALL)
                && Set.of(TicketStatus.CREATED, TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS, TicketStatus.WAITING_FOR_CUSTOMER).contains(ticket.getStatus());

            if (!requesterCanCancel && !privilegedCanCancel) {
                throw new ForbiddenException("ACCESS_DENIED", "You do not have permission to cancel this ticket.");
            }

            ticket.cancel(clock.instant());
            ticket = ticketRepository.save(ticket);
            addHistory(ticket.getId(), TicketHistoryAction.CANCELLED, currentUser.id(), null, null, "{\"reason\":\"" + escapeJson(request.reason()) + "\"}");
            return toDetailResponse(ticket, currentUser);
        });
    }

    public List<TicketCommentResponse> listComments(AuthenticatedUser currentUser, String ticketId) {
        return transactionRunner.readOnly(() -> {
            Ticket ticket = findTicket(ticketId);
            ensureCanViewTicket(currentUser, ticket);
            List<TicketComment> comments = ticketCommentRepository.findAllByTicketIdOrderByCreatedAtAsc(ticketId);
            Map<String, User> usersById = loadUsersById(comments.stream().map(TicketComment::authorId).toList());

            return comments.stream()
                .filter(comment -> comment.visibility() == CommentVisibility.PUBLIC || canSeeInternalComments(currentUser))
                .map(comment -> toCommentResponse(comment, usersById))
                .toList();
        });
    }

    public TicketCommentResponse addComment(AuthenticatedUser currentUser, String ticketId, AddCommentRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            ensureCanViewTicket(currentUser, ticket);
            ensureVersion(ticket, request.version());
            ensureNotTerminal(ticket);
            if (request.content() == null || request.content().isBlank()) {
                throw new ValidationException("COMMENT_CONTENT_REQUIRED", "The comment content is required.");
            }

            CommentVisibility visibility = request.visibility();
            if (visibility == CommentVisibility.INTERNAL) {
                authorizationService.requirePermission(currentUser, Permission.COMMENT_CREATE_INTERNAL);
            } else {
                authorizationService.requirePermission(currentUser, Permission.COMMENT_CREATE_PUBLIC);
            }
            if (currentUser.role() == Role.CUSTOMER && visibility == CommentVisibility.INTERNAL) {
                throw new ForbiddenException("ACCESS_DENIED", "Customers cannot create internal comments.");
            }

            TicketComment comment = addCommentInternal(currentUser, ticket, request.content(), visibility);
            if (currentUser.role() == Role.CUSTOMER && ticket.getStatus() == TicketStatus.WAITING_FOR_CUSTOMER && visibility == CommentVisibility.PUBLIC) {
                ticket.continueAfterCustomerResponse(clock.instant());
            }
            ticket = ticketRepository.save(ticket);

            if (currentUser.role() != Role.CUSTOMER && visibility == CommentVisibility.PUBLIC && ticket.getRequesterId() != null) {
                notifyUser(ticket.getRequesterId(), NotificationType.PUBLIC_COMMENT_ADDED, "Nuevo comentario", "Hay un nuevo comentario en el ticket " + ticket.getCode() + ".", ticket.getId());
            }
            if (currentUser.role() == Role.CUSTOMER && ticket.getAssignedAgentId() != null) {
                notifyUser(ticket.getAssignedAgentId(), NotificationType.PUBLIC_COMMENT_ADDED, "Respuesta del cliente", "El cliente respondio en el ticket " + ticket.getCode() + ".", ticket.getId());
            }

            Map<String, User> usersById = loadUsersById(List.of(comment.authorId()));
            return toCommentResponse(comment, usersById);
        });
    }

    public List<TicketHistoryResponse> listHistory(AuthenticatedUser currentUser, String ticketId) {
        return transactionRunner.readOnly(() -> {
            Ticket ticket = findTicket(ticketId);
            ensureCanViewTicket(currentUser, ticket);
            authorizationService.requirePermission(currentUser, Permission.AUDIT_READ);
            List<TicketHistory> historyEntries = ticketHistoryRepository.findAllByTicketIdOrderByCreatedAtDesc(ticketId);
            Map<String, User> usersById = loadUsersById(historyEntries.stream().map(TicketHistory::performedBy).toList());

            return historyEntries.stream()
                .map(entry -> new TicketHistoryResponse(
                    entry.id(),
                    entry.action(),
                    entry.performedBy(),
                    displayName(usersById.get(entry.performedBy())),
                    entry.previousValue(),
                    entry.newValue(),
                    entry.metadataJson(),
                    entry.createdAt()
                ))
                .toList();
        });
    }

    public void purgeExpiredIdempotencyRecords() {
        transactionRunner.required(() -> idempotencyRecordRepository.deleteByExpiresAtBefore(clock.instant()));
    }

    public void autoCloseResolvedTickets() {
        transactionRunner.required(() -> {
            Instant resolvedBefore = clock.instant().minus(ticketLifecyclePolicy.getAutoCloseDays(), ChronoUnit.DAYS);
            ticketRepository.findAllByStatusAndResolvedAtBefore(TicketStatus.RESOLVED, resolvedBefore)
                .forEach(ticket -> {
                    ticket.close(clock.instant());
                    Ticket savedTicket = ticketRepository.save(ticket);
                    addHistory(
                        savedTicket.getId(),
                        TicketHistoryAction.CLOSED,
                        SYSTEM_ACTOR,
                        TicketStatus.RESOLVED.name(),
                        TicketStatus.CLOSED.name(),
                        "{\"source\":\"auto-close\"}"
                    );
                    notifyUser(
                        savedTicket.getRequesterId(),
                        NotificationType.TICKET_CLOSED,
                        "Ticket cerrado automaticamente",
                        "El ticket " + savedTicket.getCode() + " fue cerrado automaticamente por inactividad.",
                        savedTicket.getId()
                    );
                });
        });
    }

    private void validateTicketFilter(TicketFilterRequest filterRequest) {
        if (filterRequest.page() < 0) {
            throw new BadRequestException("INVALID_PAGE", "The page number must be greater than or equal to zero.");
        }
        if (filterRequest.size() < 1 || filterRequest.size() > MAX_PAGE_SIZE) {
            throw new BadRequestException("INVALID_PAGE_SIZE", "The page size must be between 1 and " + MAX_PAGE_SIZE + ".");
        }
        if (filterRequest.sortBy() == null || !ALLOWED_SORT_FIELDS.contains(filterRequest.sortBy())) {
            throw new BadRequestException("INVALID_SORT_FIELD", "The sort field is not supported.");
        }
        if (filterRequest.createdFrom() != null && filterRequest.createdTo() != null && filterRequest.createdFrom().isAfter(filterRequest.createdTo())) {
            throw new BadRequestException("INVALID_DATE_RANGE", "The createdFrom value cannot be after createdTo.");
        }
    }

    private TicketVisibility resolveVisibility(AuthenticatedUser currentUser) {
        if (currentUser.hasPermission(Permission.TICKET_READ_ALL)) {
            return TicketVisibility.ALL;
        }
        if (currentUser.hasPermission(Permission.TICKET_READ_ASSIGNED)) {
            return TicketVisibility.ASSIGNED_OR_UNASSIGNED;
        }
        return TicketVisibility.REQUESTER;
    }

    private Ticket findTicket(String ticketId) {
        return ticketRepository.findById(ticketId)
            .orElseThrow(() -> new NotFoundException("TICKET_NOT_FOUND", "The ticket could not be found."));
    }

    private Category findActiveCategory(String categoryId) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "The category could not be found."));
        if (!category.active()) {
            throw new ValidationException("CATEGORY_INACTIVE", "The category is inactive.");
        }
        return category;
    }

    private SlaPolicy findActiveSlaPolicy(TicketPriority priority) {
        SlaPolicy policy = slaPolicyRepository.findByPriority(priority)
            .orElseThrow(() -> new NotFoundException("SLA_POLICY_NOT_FOUND", "The SLA policy could not be found."));
        if (!policy.active()) {
            throw new ValidationException("SLA_POLICY_INACTIVE", "The SLA policy is inactive.");
        }
        return policy;
    }

    private void ensureCanViewTicket(AuthenticatedUser currentUser, Ticket ticket) {
        if (currentUser.hasPermission(Permission.TICKET_READ_ALL)) {
            return;
        }
        if (currentUser.hasPermission(Permission.TICKET_READ_ASSIGNED) && (currentUser.id().equals(ticket.getAssignedAgentId()) || ticket.getAssignedAgentId() == null)) {
            return;
        }
        if (currentUser.hasPermission(Permission.TICKET_READ_OWN) && currentUser.id().equals(ticket.getRequesterId())) {
            return;
        }
        throw new ForbiddenException("ACCESS_DENIED", "You do not have permission to view this ticket.");
    }

    private void ensureCanUpdateTicket(AuthenticatedUser currentUser, Ticket ticket) {
        if (ticket.isTerminal()) {
            throw new ValidationException("TICKET_TERMINAL", "Terminal tickets cannot be modified.");
        }
        if (currentUser.hasPermission(Permission.TICKET_READ_ALL)) {
            return;
        }
        if (currentUser.id().equals(ticket.getRequesterId()) && ticket.getStatus() == TicketStatus.CREATED) {
            return;
        }
        throw new ForbiddenException("ACCESS_DENIED", "You do not have permission to update this ticket.");
    }

    private void ensureCanOperateTicket(AuthenticatedUser currentUser, Ticket ticket) {
        if (currentUser.hasPermission(Permission.TICKET_READ_ALL)) {
            return;
        }
        if (currentUser.hasPermission(Permission.TICKET_CHANGE_STATUS) && currentUser.id().equals(ticket.getAssignedAgentId())) {
            return;
        }
        throw new ForbiddenException("ACCESS_DENIED", "You do not have permission to operate on this ticket.");
    }

    private boolean canSeeInternalComments(AuthenticatedUser currentUser) {
        return currentUser.hasPermission(Permission.COMMENT_READ_INTERNAL);
    }

    private void ensureVersion(Ticket ticket, long version) {
        if (ticket.getVersion() != version) {
            throw new ConflictException("RESOURCE_VERSION_CONFLICT", "The ticket was modified by another request.");
        }
    }

    private void ensureNotTerminal(Ticket ticket) {
        if (ticket.isTerminal()) {
            throw new ValidationException("TICKET_TERMINAL", "Terminal tickets cannot be modified.");
        }
    }

    private TicketComment addCommentInternal(AuthenticatedUser currentUser, Ticket ticket, String content, CommentVisibility visibility) {
        TicketComment savedComment = ticketCommentRepository.save(TicketComment.create(
            ticket.getId(),
            currentUser.id(),
            normalize(content),
            visibility
        ));

        if (visibility == CommentVisibility.PUBLIC && currentUser.role() != Role.CUSTOMER) {
            ticket.applyFirstResponseIfMissing(clock.instant());
        }

        addHistory(
            ticket.getId(),
            visibility == CommentVisibility.PUBLIC ? TicketHistoryAction.COMMENT_ADDED_PUBLIC : TicketHistoryAction.COMMENT_ADDED_INTERNAL,
            currentUser.id(),
            null,
            null,
            "{\"commentId\":\"" + savedComment.id() + "\"}"
        );
        return savedComment;
    }

    private void addHistory(String ticketId, TicketHistoryAction action, String performedBy, String previousValue, String newValue, String metadataJson) {
        ticketHistoryRepository.save(TicketHistory.create(ticketId, action, performedBy, previousValue, newValue, metadataJson));
    }

    private void notifySupportUsers(NotificationType type, String title, String message, String ticketId) {
        List<User> recipients = userRepository.findAllByRoleInAndActiveTrue(List.of(Role.ADMIN, Role.SUPPORT_MANAGER));
        recipients.forEach(user -> notifyUser(user.id(), type, title, message, ticketId));
    }

    private void notifyUser(String recipientId, NotificationType type, String title, String message, String ticketId) {
        notificationRepository.save(Notification.create(recipientId, type, title, message, ticketId));
    }

    private void storeIdempotencyRecord(String idempotencyKey, String userId, String requestHash, String resourceId, TicketDetailResponse response) {
        idempotencyRecordRepository.save(IdempotencyRecord.create(
            idempotencyKey,
            userId,
            requestHash,
            201,
            jsonCodec.serialize(response),
            resourceId,
            clock.instant().plus(idempotencyPolicy.getRecordTtlHours(), ChronoUnit.HOURS)
        ));
    }

    private TicketDetailResponse deserializeStoredResponse(String responseBody) {
        return jsonCodec.deserialize(responseBody, TicketDetailResponse.class);
    }

    private Map<String, User> loadUsersById(Collection<String> userIds) {
        Set<String> uniqueIds = userIds.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (uniqueIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(uniqueIds)
            .stream()
            .collect(Collectors.toMap(User::id, Function.identity()));
    }

    private Map<String, Category> loadCategoriesById(Collection<String> categoryIds) {
        Set<String> uniqueIds = categoryIds.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (uniqueIds.isEmpty()) {
            return Map.of();
        }
        return categoryRepository.findAllById(uniqueIds)
            .stream()
            .collect(Collectors.toMap(Category::id, Function.identity()));
    }

    private TicketSummaryResponse toSummaryResponse(
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
            categoriesById.containsKey(ticket.getCategoryId()) ? categoriesById.get(ticket.getCategoryId()).name() : null,
            ticket.getResolutionDueAt(),
            ticket.isSlaFirstResponseBreached(),
            ticket.isSlaResolutionBreached(),
            ticket.getCreatedAt(),
            ticket.getUpdatedAt(),
            ticket.getVersion()
        );
    }

    private TicketDetailResponse toDetailResponse(Ticket ticket, AuthenticatedUser currentUser) {
        Map<String, User> usersById = loadUsersById(
            Stream.of(ticket.getRequesterId(), ticket.getAssignedAgentId())
                .filter(Objects::nonNull)
                .toList()
        );
        Map<String, Category> categoriesById = loadCategoriesById(List.of(ticket.getCategoryId()));
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
            categoriesById.containsKey(ticket.getCategoryId()) ? categoriesById.get(ticket.getCategoryId()).name() : null,
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
            availableActions(currentUser, ticket)
        );
    }

    private TicketCommentResponse toCommentResponse(TicketComment comment, Map<String, User> usersById) {
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

    private List<String> availableActions(AuthenticatedUser currentUser, Ticket ticket) {
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
        if (currentUser.hasPermission(Permission.TICKET_CHANGE_STATUS) && (currentUser.hasPermission(Permission.TICKET_READ_ALL) || currentUser.id().equals(ticket.getAssignedAgentId()))) {
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

    private String displayName(User user) {
        if (user == null) {
            return null;
        }
        return user.displayName();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String escapeJson(String value) {
        return normalize(value).replace("\"", "\\\"");
    }

    public record TicketFilterRequest(
        String search,
        TicketStatus status,
        TicketPriority priority,
        String categoryId,
        String assignedAgentId,
        Instant createdFrom,
        Instant createdTo,
        int page,
        int size,
        String sortBy,
        SortDirection direction
    ) {
    }

    public record CreateTicketRequest(String title, String description, String categoryId, TicketPriority priority) {
    }

    public record UpdateTicketRequest(long version, String title, String description, String categoryId, TicketPriority priority) {
    }

    public record AssignTicketRequest(long version, String agentId) {
    }

    public record VersionedRequest(long version) {
    }

    public record RequestInformationRequest(long version, String content) {
    }

    public record ResolveTicketRequest(long version, String resolutionSummary) {
    }

    public record ReopenTicketRequest(long version, String reason) {
    }

    public record CancelTicketRequest(long version, String reason) {
    }

    public record AddCommentRequest(long version, String content, CommentVisibility visibility) {
    }

    public record TicketSummaryResponse(
        String id,
        String code,
        String title,
        TicketStatus status,
        TicketPriority priority,
        String requesterId,
        String requesterName,
        String assignedAgentId,
        String assignedAgentName,
        String categoryId,
        String categoryName,
        Instant resolutionDueAt,
        boolean slaFirstResponseBreached,
        boolean slaResolutionBreached,
        Instant createdAt,
        Instant updatedAt,
        long version
    ) {
    }

    public record TicketDetailResponse(
        String id,
        String code,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        String requesterId,
        String requesterName,
        String assignedAgentId,
        String assignedAgentName,
        String categoryId,
        String categoryName,
        Instant firstResponseDueAt,
        Instant resolutionDueAt,
        Instant firstRespondedAt,
        Instant resolvedAt,
        Instant closedAt,
        Instant cancelledAt,
        Instant slaPausedAt,
        long accumulatedPausedSeconds,
        boolean slaFirstResponseBreached,
        boolean slaResolutionBreached,
        String resolutionSummary,
        Instant createdAt,
        Instant updatedAt,
        long version,
        List<String> availableActions
    ) {
    }

    public record TicketCommentResponse(
        String id,
        String ticketId,
        String authorId,
        String authorName,
        String content,
        CommentVisibility visibility,
        Instant createdAt,
        Instant updatedAt
    ) {
    }

    public record TicketHistoryResponse(
        String id,
        TicketHistoryAction action,
        String performedBy,
        String performedByName,
        String previousValue,
        String newValue,
        String metadataJson,
        Instant createdAt
    ) {
    }
}
