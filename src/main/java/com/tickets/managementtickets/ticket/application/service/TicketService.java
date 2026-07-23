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
import com.tickets.managementtickets.shared.application.exception.ForbiddenException;
import com.tickets.managementtickets.shared.application.exception.NotFoundException;
import com.tickets.managementtickets.shared.application.exception.UnauthorizedException;
import com.tickets.managementtickets.shared.application.exception.ValidationException;
import com.tickets.managementtickets.shared.application.model.PageResponse;
import com.tickets.managementtickets.shared.application.port.HashingService;
import com.tickets.managementtickets.shared.application.port.JsonCodec;
import com.tickets.managementtickets.shared.application.port.TransactionRunner;
import com.tickets.managementtickets.sla.application.port.SlaPolicyRepositoryPort;
import com.tickets.managementtickets.sla.domain.model.SlaPolicy;
import com.tickets.managementtickets.ticket.application.command.AddCommentRequest;
import com.tickets.managementtickets.ticket.application.command.AssignTicketRequest;
import com.tickets.managementtickets.ticket.application.command.CancelTicketRequest;
import com.tickets.managementtickets.ticket.application.command.CreateTicketRequest;
import com.tickets.managementtickets.ticket.application.command.ReopenTicketRequest;
import com.tickets.managementtickets.ticket.application.command.RequestInformationRequest;
import com.tickets.managementtickets.ticket.application.command.ResolveTicketRequest;
import com.tickets.managementtickets.ticket.application.command.UpdateTicketRequest;
import com.tickets.managementtickets.ticket.application.command.VersionedRequest;
import com.tickets.managementtickets.ticket.application.port.IdempotencyPolicy;
import com.tickets.managementtickets.ticket.application.port.IdempotencyRecordRepositoryPort;
import com.tickets.managementtickets.ticket.application.port.TicketCodeGenerator;
import com.tickets.managementtickets.ticket.application.port.TicketCommentRepositoryPort;
import com.tickets.managementtickets.ticket.application.port.TicketHistoryRepositoryPort;
import com.tickets.managementtickets.ticket.application.port.TicketLifecyclePolicy;
import com.tickets.managementtickets.ticket.application.port.TicketQuery;
import com.tickets.managementtickets.ticket.application.port.TicketRepositoryPort;
import com.tickets.managementtickets.ticket.application.query.TicketFilterRequest;
import com.tickets.managementtickets.ticket.application.result.TicketCommentResponse;
import com.tickets.managementtickets.ticket.application.result.TicketDetailResponse;
import com.tickets.managementtickets.ticket.application.result.TicketHistoryResponse;
import com.tickets.managementtickets.ticket.application.result.TicketSummaryResponse;
import com.tickets.managementtickets.ticket.domain.model.CommentVisibility;
import com.tickets.managementtickets.ticket.domain.model.Ticket;
import com.tickets.managementtickets.ticket.domain.model.TicketComment;
import com.tickets.managementtickets.ticket.domain.model.TicketHistory;
import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

    private static final String SYSTEM_ACTOR = "00000000-0000-0000-0000-000000000000";

    private final TicketRepositoryPort ticketRepository;
    private final TicketCommentRepositoryPort ticketCommentRepository;
    private final TicketHistoryRepositoryPort ticketHistoryRepository;
    private final UserRepositoryPort userRepository;
    private final CategoryRepositoryPort categoryRepository;
    private final SlaPolicyRepositoryPort slaPolicyRepository;
    private final TicketCodeGenerator ticketCodeGenerator;
    private final AuthorizationService authorizationService;
    private final Clock clock;
    private final TicketLifecyclePolicy ticketLifecyclePolicy;
    private final TransactionRunner transactionRunner;
    private final TicketAccessPolicy accessPolicy;
    private final TicketResponseMapper responseMapper;
    private final TicketHistoryRecorder historyRecorder;
    private final TicketNotificationDispatcher notificationDispatcher;
    private final TicketIdempotencyHandler idempotencyHandler;

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
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.slaPolicyRepository = slaPolicyRepository;
        this.ticketCodeGenerator = ticketCodeGenerator;
        this.authorizationService = authorizationService;
        this.clock = clock;
        this.ticketLifecyclePolicy = ticketLifecyclePolicy;
        this.transactionRunner = transactionRunner;
        this.accessPolicy = new TicketAccessPolicy();
        this.responseMapper = new TicketResponseMapper(accessPolicy);
        this.historyRecorder = new TicketHistoryRecorder(ticketHistoryRepository, jsonCodec);
        this.notificationDispatcher = new TicketNotificationDispatcher(notificationRepository, userRepository);
        this.idempotencyHandler = new TicketIdempotencyHandler(
            idempotencyRecordRepository,
            hashingService,
            jsonCodec,
            idempotencyPolicy,
            clock
        );
    }

    public PageResponse<TicketSummaryResponse> list(AuthenticatedUser currentUser, TicketFilterRequest filterRequest) {
        return transactionRunner.readOnly(() -> {
            accessPolicy.validateFilter(filterRequest);

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
                accessPolicy.resolveVisibility(currentUser),
                currentUser.id()
            ));
            Map<String, User> usersById = loadUsersById(
                page.content().stream()
                    .flatMap(ticket -> Stream.of(ticket.getRequesterId(), ticket.getAssignedAgentId()))
                    .filter(Objects::nonNull)
                    .toList()
            );
            Map<String, Category> categoriesById = loadCategoriesById(page.content().stream().map(Ticket::getCategoryId).toList());

            return page.map(ticket -> responseMapper.toSummaryResponse(ticket, usersById, categoriesById));
        });
    }

    public TicketDetailResponse getById(AuthenticatedUser currentUser, String ticketId) {
        return transactionRunner.readOnly(() -> {
            Ticket ticket = findTicket(ticketId);
            accessPolicy.ensureCanViewTicket(currentUser, ticket);
            return toDetailResponse(ticket, currentUser);
        });
    }

    public TicketDetailResponse create(AuthenticatedUser currentUser, CreateTicketRequest request, String idempotencyKey) {
        return transactionRunner.required(() -> {
            authorizationService.requirePermission(currentUser, Permission.TICKET_CREATE);
            idempotencyHandler.requireKey(idempotencyKey);

            User requester = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new UnauthorizedException(
                    "AUTHENTICATED_USER_NOT_FOUND",
                    "The authenticated user no longer exists. Please sign in again."
                ));

            String requestHash = idempotencyHandler.hashCreateRequest(currentUser.id(), request);
            Optional<TicketDetailResponse> storedResponse = idempotencyHandler.findStoredCreateResponse(idempotencyKey, currentUser.id(), requestHash);
            if (storedResponse.isPresent()) {
                return storedResponse.get();
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

            historyRecorder.created(ticket.getId(), currentUser.id(), ticket.getCode());
            notificationDispatcher.ticketCreated(ticket);

            TicketDetailResponse response = toDetailResponse(ticket, currentUser);
            idempotencyHandler.storeCreateResponse(idempotencyKey, currentUser.id(), requestHash, ticket.getId(), response);
            return response;
        });
    }

    public TicketDetailResponse update(AuthenticatedUser currentUser, String ticketId, UpdateTicketRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            accessPolicy.ensureCanUpdateTicket(currentUser, ticket);
            accessPolicy.ensureVersion(ticket, request.version());

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
                authorizationService.requirePermission(currentUser, Permission.TICKET_CHANGE_PRIORITY);
                ticket.changePriority(request.priority(), findActiveSlaPolicy(request.priority()), clock.instant());
            }

            ticket = ticketRepository.save(ticket);

            if (!Objects.equals(previousTitle, ticket.getTitle()) || !Objects.equals(previousDescription, ticket.getDescription())) {
                historyRecorder.updated(ticket.getId(), currentUser.id(), previousTitle, ticket.getTitle());
            }
            if (!Objects.equals(previousCategoryId, ticket.getCategoryId())) {
                historyRecorder.categoryChanged(ticket.getId(), currentUser.id(), previousCategoryId, ticket.getCategoryId());
            }
            if (previousPriority != ticket.getPriority()) {
                historyRecorder.priorityChanged(ticket.getId(), currentUser.id(), previousPriority.name(), ticket.getPriority().name());
            }

            return toDetailResponse(ticket, currentUser);
        });
    }

    public TicketDetailResponse assign(AuthenticatedUser currentUser, String ticketId, AssignTicketRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            authorizationService.requirePermission(currentUser, ticket.getAssignedAgentId() == null ? Permission.TICKET_ASSIGN : Permission.TICKET_REASSIGN);
            accessPolicy.ensureVersion(ticket, request.version());
            accessPolicy.ensureNotTerminal(ticket);

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

            historyRecorder.assigned(ticket.getId(), currentUser.id(), previousAgentId, assignee.id());
            notificationDispatcher.ticketAssigned(ticket, assignee.id());
            return toDetailResponse(ticket, currentUser);
        });
    }

    public TicketDetailResponse start(AuthenticatedUser currentUser, String ticketId, VersionedRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            accessPolicy.ensureCanOperateTicket(currentUser, ticket);
            authorizationService.requirePermission(currentUser, Permission.TICKET_RESOLVE);
            accessPolicy.ensureVersion(ticket, request.version());
            if (ticket.getStatus() != TicketStatus.ASSIGNED) {
                throw new ValidationException("INVALID_TICKET_TRANSITION", "The ticket must be in ASSIGNED status.");
            }

            ticket.start(clock.instant());
            ticket = ticketRepository.save(ticket);
            historyRecorder.started(ticket.getId(), currentUser.id());
            return toDetailResponse(ticket, currentUser);
        });
    }

    public TicketDetailResponse requestInformation(AuthenticatedUser currentUser, String ticketId, RequestInformationRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            accessPolicy.ensureCanOperateTicket(currentUser, ticket);
            accessPolicy.ensureVersion(ticket, request.version());
            if (ticket.getStatus() != TicketStatus.IN_PROGRESS) {
                throw new ValidationException("INVALID_TICKET_TRANSITION", "The ticket must be in IN_PROGRESS status.");
            }
            addCommentInternal(currentUser, ticket, request.content(), CommentVisibility.PUBLIC);
            ticket.requestInformation(clock.instant());
            ticket = ticketRepository.save(ticket);
            historyRecorder.requestedInformation(ticket.getId(), currentUser.id());
            notificationDispatcher.informationRequested(ticket);
            return toDetailResponse(ticket, currentUser);
        });
    }

    public TicketDetailResponse resolve(AuthenticatedUser currentUser, String ticketId, ResolveTicketRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            accessPolicy.ensureCanOperateTicket(currentUser, ticket);
            accessPolicy.ensureVersion(ticket, request.version());
            if (ticket.getStatus() != TicketStatus.IN_PROGRESS && ticket.getStatus() != TicketStatus.WAITING_FOR_CUSTOMER) {
                throw new ValidationException("INVALID_TICKET_TRANSITION", "The ticket cannot be resolved from its current status.");
            }
            if (request.resolutionSummary() == null || request.resolutionSummary().isBlank()) {
                throw new ValidationException("RESOLUTION_SUMMARY_REQUIRED", "The resolution summary is required.");
            }

            ticket.resolve(normalize(request.resolutionSummary()), clock.instant());
            ticket = ticketRepository.save(ticket);
            historyRecorder.resolved(ticket.getId(), currentUser.id(), ticket.getResolutionSummary());
            notificationDispatcher.ticketResolved(ticket);
            return toDetailResponse(ticket, currentUser);
        });
    }

    public TicketDetailResponse close(AuthenticatedUser currentUser, String ticketId, VersionedRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            accessPolicy.ensureVersion(ticket, request.version());
            accessPolicy.ensureCanCloseTicket(currentUser, ticket);
            if (ticket.getStatus() != TicketStatus.RESOLVED) {
                throw new ValidationException("INVALID_TICKET_TRANSITION", "The ticket must be resolved before closing.");
            }

            ticket.close(clock.instant());
            ticket = ticketRepository.save(ticket);
            historyRecorder.closed(ticket.getId(), currentUser.id());
            notificationDispatcher.ticketClosed(ticket);
            return toDetailResponse(ticket, currentUser);
        });
    }

    public TicketDetailResponse reopen(AuthenticatedUser currentUser, String ticketId, ReopenTicketRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            accessPolicy.ensureVersion(ticket, request.version());
            if (ticket.getStatus() != TicketStatus.RESOLVED) {
                throw new ValidationException("INVALID_TICKET_TRANSITION", "Only resolved tickets can be reopened.");
            }
            if (request.reason() == null || request.reason().isBlank()) {
                throw new ValidationException("REOPEN_REASON_REQUIRED", "The reopen reason is required.");
            }

            accessPolicy.ensureCanReopenTicket(currentUser, ticket, clock.instant());
            ticket.reopen(clock.instant(), findActiveSlaPolicy(ticket.getPriority()));
            ticket = ticketRepository.save(ticket);
            historyRecorder.reopened(ticket.getId(), currentUser.id(), request.reason());
            notificationDispatcher.ticketReopened(ticket);
            return toDetailResponse(ticket, currentUser);
        });
    }

    public TicketDetailResponse cancel(AuthenticatedUser currentUser, String ticketId, CancelTicketRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            accessPolicy.ensureVersion(ticket, request.version());
            if (request.reason() == null || request.reason().isBlank()) {
                throw new ValidationException("CANCEL_REASON_REQUIRED", "The cancel reason is required.");
            }

            accessPolicy.ensureCanCancelTicket(currentUser, ticket);
            ticket.cancel(clock.instant());
            ticket = ticketRepository.save(ticket);
            historyRecorder.cancelled(ticket.getId(), currentUser.id(), request.reason());
            return toDetailResponse(ticket, currentUser);
        });
    }

    public List<TicketCommentResponse> listComments(AuthenticatedUser currentUser, String ticketId) {
        return transactionRunner.readOnly(() -> {
            Ticket ticket = findTicket(ticketId);
            accessPolicy.ensureCanViewTicket(currentUser, ticket);
            List<TicketComment> comments = ticketCommentRepository.findAllByTicketIdOrderByCreatedAtAsc(ticketId);
            Map<String, User> usersById = loadUsersById(comments.stream().map(TicketComment::authorId).toList());

            return comments.stream()
                .filter(comment -> comment.visibility() == CommentVisibility.PUBLIC || accessPolicy.canSeeInternalComments(currentUser))
                .map(comment -> toCommentResponse(comment, usersById))
                .toList();
        });
    }

    public TicketCommentResponse addComment(AuthenticatedUser currentUser, String ticketId, AddCommentRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            accessPolicy.ensureCanViewTicket(currentUser, ticket);
            accessPolicy.ensureVersion(ticket, request.version());
            accessPolicy.ensureNotTerminal(ticket);
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
                notificationDispatcher.publicCommentAddedForRequester(ticket);
            }
            if (currentUser.role() == Role.CUSTOMER && ticket.getAssignedAgentId() != null) {
                notificationDispatcher.customerReplied(ticket);
            }

            Map<String, User> usersById = loadUsersById(List.of(comment.authorId()));
            return toCommentResponse(comment, usersById);
        });
    }

    public List<TicketHistoryResponse> listHistory(AuthenticatedUser currentUser, String ticketId) {
        return transactionRunner.readOnly(() -> {
            Ticket ticket = findTicket(ticketId);
            accessPolicy.ensureCanViewTicket(currentUser, ticket);
            authorizationService.requirePermission(currentUser, Permission.AUDIT_READ);
            List<TicketHistory> historyEntries = ticketHistoryRepository.findAllByTicketIdOrderByCreatedAtDesc(ticketId);
            Map<String, User> usersById = loadUsersById(historyEntries.stream().map(TicketHistory::performedBy).toList());

            return historyEntries.stream()
                .map(entry -> responseMapper.toHistoryResponse(entry, usersById))
                .toList();
        });
    }

    public void purgeExpiredIdempotencyRecords() {
        transactionRunner.required(idempotencyHandler::purgeExpiredRecords);
    }

    public void autoCloseResolvedTickets() {
        transactionRunner.required(() -> {
            Instant resolvedBefore = clock.instant().minus(ticketLifecyclePolicy.getAutoCloseDays(), ChronoUnit.DAYS);
            ticketRepository.findAllByStatusAndResolvedAtBefore(TicketStatus.RESOLVED, resolvedBefore)
                .forEach(ticket -> {
                    ticket.close(clock.instant());
                    Ticket savedTicket = ticketRepository.save(ticket);
                    historyRecorder.autoClosed(savedTicket.getId(), SYSTEM_ACTOR);
                    notificationDispatcher.ticketAutoClosed(savedTicket);
                });
        });
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

        historyRecorder.commentAdded(
            ticket.getId(),
            currentUser.id(),
            savedComment.id(),
            visibility == CommentVisibility.PUBLIC
        );
        return savedComment;
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

    private TicketDetailResponse toDetailResponse(Ticket ticket, AuthenticatedUser currentUser) {
        Map<String, User> usersById = loadUsersById(
            Stream.of(ticket.getRequesterId(), ticket.getAssignedAgentId())
                .filter(Objects::nonNull)
                .toList()
        );
        Map<String, Category> categoriesById = loadCategoriesById(List.of(ticket.getCategoryId()));
        return responseMapper.toDetailResponse(ticket, currentUser, usersById, categoriesById);
    }

    private TicketCommentResponse toCommentResponse(TicketComment comment, Map<String, User> usersById) {
        return responseMapper.toCommentResponse(comment, usersById);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

}
