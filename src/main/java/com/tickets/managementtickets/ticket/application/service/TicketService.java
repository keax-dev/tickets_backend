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
import com.tickets.managementtickets.ticket.application.port.TicketMetricsPort;
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
    private final TicketCodeGenerator ticketCodeGenerator;
    private final AuthorizationService authorizationService;
    private final Clock clock;
    private final TicketLifecyclePolicy ticketLifecyclePolicy;
    private final TransactionRunner transactionRunner;
    private final TicketAccessPolicy accessPolicy;
    private final TicketFilterValidator filterValidator;
    private final TicketCommandValidator commandValidator;
    private final TicketReferenceResolver referenceResolver;
    private final TicketResponseMapper responseMapper;
    private final TicketHistoryRecorder historyRecorder;
    private final TicketNotificationDispatcher notificationDispatcher;
    private final TicketIdempotencyHandler idempotencyHandler;
    private final TicketMetricsPort metricsPort;

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
        this(
            ticketRepository,
            ticketCommentRepository,
            ticketHistoryRepository,
            idempotencyRecordRepository,
            userRepository,
            categoryRepository,
            slaPolicyRepository,
            notificationRepository,
            ticketCodeGenerator,
            authorizationService,
            hashingService,
            jsonCodec,
            clock,
            idempotencyPolicy,
            ticketLifecyclePolicy,
            transactionRunner,
            TicketMetricsPort.NO_OP
        );
    }

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
        TransactionRunner transactionRunner,
        TicketMetricsPort metricsPort
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketCommentRepository = ticketCommentRepository;
        this.ticketHistoryRepository = ticketHistoryRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.ticketCodeGenerator = ticketCodeGenerator;
        this.authorizationService = authorizationService;
        this.clock = clock;
        this.ticketLifecyclePolicy = ticketLifecyclePolicy;
        this.transactionRunner = transactionRunner;
        this.accessPolicy = new TicketAccessPolicy();
        this.filterValidator = new TicketFilterValidator();
        this.commandValidator = new TicketCommandValidator();
        this.referenceResolver = new TicketReferenceResolver(categoryRepository, slaPolicyRepository);
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
        this.metricsPort = metricsPort;
    }

    public PageResponse<TicketSummaryResponse> list(AuthenticatedUser currentUser, TicketFilterRequest filterRequest) {
        return transactionRunner.readOnly(() -> {
            filterValidator.validate(filterRequest);

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
            return buildDetailResponse(ticket, currentUser);
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

            Category category = referenceResolver.findActiveCategory(request.categoryId());
            SlaPolicy slaPolicy = referenceResolver.findActiveSlaPolicy(request.priority());

            Instant now = clock.instant();
            Ticket ticket = Ticket.create(
                ticketCodeGenerator.nextCode(now),
                commandValidator.normalize(request.title()),
                commandValidator.normalize(request.description()),
                request.priority(),
                requester.id(),
                category.id(),
                slaPolicy,
                now
            );
            ticket = ticketRepository.save(ticket);

            historyRecorder.created(ticket.getId(), currentUser.id(), ticket.getCode());
            notificationDispatcher.ticketCreated(ticket);
            metricsPort.recordTicketCreated(ticket);

            TicketDetailResponse response = buildDetailResponse(ticket, currentUser);
            idempotencyHandler.storeCreateResponse(idempotencyKey, currentUser.id(), requestHash, ticket.getId(), response);
            return response;
        });
    }

    public TicketDetailResponse update(AuthenticatedUser currentUser, String ticketId, UpdateTicketRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            commandValidator.ensureNotTerminal(ticket);
            accessPolicy.ensureCanUpdateTicket(currentUser, ticket);
            commandValidator.ensureVersion(ticket, request.version());

            String previousTitle = ticket.getTitle();
            String previousDescription = ticket.getDescription();
            String previousCategoryId = ticket.getCategoryId();
            TicketPriority previousPriority = ticket.getPriority();

            String requestedCategoryId = null;
            if (request.categoryId() != null && !request.categoryId().isBlank()) {
                requestedCategoryId = referenceResolver.findActiveCategory(request.categoryId()).id();
            }
            ticket.updateDetails(commandValidator.normalize(request.title()), commandValidator.normalize(request.description()), requestedCategoryId);
            if (request.priority() != null && request.priority() != ticket.getPriority()) {
                authorizationService.requirePermission(currentUser, Permission.TICKET_CHANGE_PRIORITY);
                ticket.changePriority(request.priority(), referenceResolver.findActiveSlaPolicy(request.priority()), clock.instant());
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

            return buildDetailResponse(ticket, currentUser);
        });
    }

    public TicketDetailResponse assign(AuthenticatedUser currentUser, String ticketId, AssignTicketRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            authorizationService.requirePermission(currentUser, ticket.getAssignedAgentId() == null ? Permission.TICKET_ASSIGN : Permission.TICKET_REASSIGN);
            commandValidator.ensureVersion(ticket, request.version());
            commandValidator.ensureNotTerminal(ticket);

            User assignee = userRepository.findById(request.agentId())
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "The assignee could not be found."));
            commandValidator.ensureAssigneeCanHandleTickets(assignee);

            String previousAgentId = ticket.getAssignedAgentId();
            ticket.assign(assignee.id());
            ticket = ticketRepository.save(ticket);

            historyRecorder.assigned(ticket.getId(), currentUser.id(), previousAgentId, assignee.id());
            notificationDispatcher.ticketAssigned(ticket, assignee.id());
            return buildDetailResponse(ticket, currentUser);
        });
    }

    public TicketDetailResponse start(AuthenticatedUser currentUser, String ticketId, VersionedRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            accessPolicy.ensureCanOperateTicket(currentUser, ticket);
            authorizationService.requirePermission(currentUser, Permission.TICKET_RESOLVE);
            commandValidator.ensureVersion(ticket, request.version());
            commandValidator.ensureStartAllowed(ticket);

            ticket.start(clock.instant());
            ticket = ticketRepository.save(ticket);
            historyRecorder.started(ticket.getId(), currentUser.id());
            return buildDetailResponse(ticket, currentUser);
        });
    }

    public TicketDetailResponse requestInformation(AuthenticatedUser currentUser, String ticketId, RequestInformationRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            accessPolicy.ensureCanOperateTicket(currentUser, ticket);
            commandValidator.ensureVersion(ticket, request.version());
            commandValidator.ensureInformationRequestAllowed(ticket);
            addCommentInternal(currentUser, ticket, request.content(), CommentVisibility.PUBLIC);
            ticket.requestInformation(clock.instant());
            ticket = ticketRepository.save(ticket);
            historyRecorder.requestedInformation(ticket.getId(), currentUser.id());
            notificationDispatcher.informationRequested(ticket);
            return buildDetailResponse(ticket, currentUser);
        });
    }

    public TicketDetailResponse resolve(AuthenticatedUser currentUser, String ticketId, ResolveTicketRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            accessPolicy.ensureCanOperateTicket(currentUser, ticket);
            commandValidator.ensureVersion(ticket, request.version());
            commandValidator.ensureResolveAllowed(ticket, request.resolutionSummary());

            ticket.resolve(commandValidator.normalize(request.resolutionSummary()), clock.instant());
            ticket = ticketRepository.save(ticket);
            historyRecorder.resolved(ticket.getId(), currentUser.id(), ticket.getResolutionSummary());
            notificationDispatcher.ticketResolved(ticket);
            metricsPort.recordTicketResolved(ticket);
            return buildDetailResponse(ticket, currentUser);
        });
    }

    public TicketDetailResponse close(AuthenticatedUser currentUser, String ticketId, VersionedRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            commandValidator.ensureVersion(ticket, request.version());
            accessPolicy.ensureCanCloseTicket(currentUser, ticket);
            commandValidator.ensureCloseAllowed(ticket);

            ticket.close(clock.instant());
            ticket = ticketRepository.save(ticket);
            historyRecorder.closed(ticket.getId(), currentUser.id());
            notificationDispatcher.ticketClosed(ticket);
            metricsPort.recordTicketClosed(ticket, false);
            return buildDetailResponse(ticket, currentUser);
        });
    }

    public TicketDetailResponse reopen(AuthenticatedUser currentUser, String ticketId, ReopenTicketRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            commandValidator.ensureVersion(ticket, request.version());
            commandValidator.ensureReopenRequestAllowed(ticket, request.reason());

            accessPolicy.ensureCanReopenTicket(currentUser, ticket, clock.instant());
            ticket.reopen(clock.instant(), referenceResolver.findActiveSlaPolicy(ticket.getPriority()));
            ticket = ticketRepository.save(ticket);
            historyRecorder.reopened(ticket.getId(), currentUser.id(), request.reason());
            notificationDispatcher.ticketReopened(ticket);
            return buildDetailResponse(ticket, currentUser);
        });
    }

    public TicketDetailResponse cancel(AuthenticatedUser currentUser, String ticketId, CancelTicketRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            commandValidator.ensureVersion(ticket, request.version());
            commandValidator.ensureCancelReason(request.reason());

            accessPolicy.ensureCanCancelTicket(currentUser, ticket);
            ticket.cancel(clock.instant());
            ticket = ticketRepository.save(ticket);
            historyRecorder.cancelled(ticket.getId(), currentUser.id(), request.reason());
            metricsPort.recordTicketCancelled(ticket);
            return buildDetailResponse(ticket, currentUser);
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
                .map(comment -> buildCommentResponse(comment, usersById))
                .toList();
        });
    }

    public TicketCommentResponse addComment(AuthenticatedUser currentUser, String ticketId, AddCommentRequest request) {
        return transactionRunner.required(() -> {
            Ticket ticket = findTicket(ticketId);
            accessPolicy.ensureCanViewTicket(currentUser, ticket);
            commandValidator.ensureVersion(ticket, request.version());
            commandValidator.ensureNotTerminal(ticket);
            commandValidator.ensureCommentContent(request.content());

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
            return buildCommentResponse(comment, usersById);
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
                    metricsPort.recordTicketClosed(savedTicket, true);
                });
        });
    }

    private Ticket findTicket(String ticketId) {
        return ticketRepository.findById(ticketId)
            .orElseThrow(() -> new NotFoundException("TICKET_NOT_FOUND", "The ticket could not be found."));
    }

    private TicketComment addCommentInternal(AuthenticatedUser currentUser, Ticket ticket, String content, CommentVisibility visibility) {
        TicketComment savedComment = ticketCommentRepository.save(TicketComment.create(
            ticket.getId(),
            currentUser.id(),
            commandValidator.normalize(content),
            visibility
        ));

        if (visibility == CommentVisibility.PUBLIC && currentUser.role() != Role.CUSTOMER) {
            ticket.applyFirstResponseIfMissing(clock.instant());
        }

        metricsPort.recordCommentAdded(visibility);
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

    private TicketDetailResponse buildDetailResponse(Ticket ticket, AuthenticatedUser currentUser) {
        Map<String, User> usersById = loadUsersById(
            Stream.of(ticket.getRequesterId(), ticket.getAssignedAgentId())
                .filter(Objects::nonNull)
                .toList()
        );
        Map<String, Category> categoriesById = loadCategoriesById(List.of(ticket.getCategoryId()));
        return responseMapper.toDetailResponse(ticket, currentUser, usersById, categoriesById);
    }

    private TicketCommentResponse buildCommentResponse(TicketComment comment, Map<String, User> usersById) {
        return responseMapper.toCommentResponse(comment, usersById);
    }

}
