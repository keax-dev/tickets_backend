package com.tickets.managementtickets.ticket.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickets.managementtickets.category.infrastructure.persistence.entity.CategoryEntity;
import com.tickets.managementtickets.category.infrastructure.persistence.repository.CategoryRepository;
import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.application.service.AuthorizationService;
import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.identity.domain.model.Role;
import com.tickets.managementtickets.identity.infrastructure.persistence.entity.UserEntity;
import com.tickets.managementtickets.identity.infrastructure.persistence.repository.UserRepository;
import com.tickets.managementtickets.notification.domain.model.NotificationType;
import com.tickets.managementtickets.notification.infrastructure.persistence.entity.NotificationEntity;
import com.tickets.managementtickets.notification.infrastructure.persistence.repository.NotificationRepository;
import com.tickets.managementtickets.shared.application.exception.BadRequestException;
import com.tickets.managementtickets.shared.application.model.PageResponse;
import com.tickets.managementtickets.shared.application.port.HashingService;
import com.tickets.managementtickets.shared.application.exception.ConflictException;
import com.tickets.managementtickets.shared.application.exception.ForbiddenException;
import com.tickets.managementtickets.shared.application.exception.NotFoundException;
import com.tickets.managementtickets.shared.application.exception.UnauthorizedException;
import com.tickets.managementtickets.shared.application.exception.ValidationException;
import com.tickets.managementtickets.sla.infrastructure.persistence.entity.SlaPolicyEntity;
import com.tickets.managementtickets.sla.infrastructure.persistence.repository.SlaPolicyRepository;
import com.tickets.managementtickets.ticket.domain.model.CommentVisibility;
import com.tickets.managementtickets.ticket.domain.model.TicketHistoryAction;
import com.tickets.managementtickets.ticket.domain.model.TicketPriority;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;
import com.tickets.managementtickets.ticket.infrastructure.persistence.entity.IdempotencyRecordEntity;
import com.tickets.managementtickets.ticket.infrastructure.persistence.entity.TicketCommentEntity;
import com.tickets.managementtickets.ticket.infrastructure.persistence.entity.TicketEntity;
import com.tickets.managementtickets.ticket.infrastructure.persistence.entity.TicketHistoryEntity;
import com.tickets.managementtickets.ticket.infrastructure.persistence.entity.TicketSequenceEntity;
import com.tickets.managementtickets.ticket.infrastructure.persistence.repository.IdempotencyRecordRepository;
import com.tickets.managementtickets.ticket.infrastructure.persistence.repository.TicketCommentRepository;
import com.tickets.managementtickets.ticket.infrastructure.persistence.repository.TicketHistoryRepository;
import com.tickets.managementtickets.ticket.infrastructure.persistence.repository.TicketRepository;
import com.tickets.managementtickets.ticket.infrastructure.persistence.repository.TicketSequenceRepository;
import com.tickets.managementtickets.ticket.infrastructure.support.IdempotencyProperties;
import com.tickets.managementtickets.ticket.infrastructure.support.TicketLifecycleProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
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

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final TicketSequenceRepository ticketSequenceRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final NotificationRepository notificationRepository;
    private final AuthorizationService authorizationService;
    private final HashingService hashingService;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final IdempotencyProperties idempotencyProperties;
    private final TicketLifecycleProperties ticketLifecycleProperties;

    public TicketService(
        TicketRepository ticketRepository,
        TicketCommentRepository ticketCommentRepository,
        TicketHistoryRepository ticketHistoryRepository,
        TicketSequenceRepository ticketSequenceRepository,
        IdempotencyRecordRepository idempotencyRecordRepository,
        UserRepository userRepository,
        CategoryRepository categoryRepository,
        SlaPolicyRepository slaPolicyRepository,
        NotificationRepository notificationRepository,
        AuthorizationService authorizationService,
        HashingService hashingService,
        ObjectMapper objectMapper,
        Clock clock,
        IdempotencyProperties idempotencyProperties,
        TicketLifecycleProperties ticketLifecycleProperties
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketCommentRepository = ticketCommentRepository;
        this.ticketHistoryRepository = ticketHistoryRepository;
        this.ticketSequenceRepository = ticketSequenceRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.slaPolicyRepository = slaPolicyRepository;
        this.notificationRepository = notificationRepository;
        this.authorizationService = authorizationService;
        this.hashingService = hashingService;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.idempotencyProperties = idempotencyProperties;
        this.ticketLifecycleProperties = ticketLifecycleProperties;
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketSummaryResponse> list(AuthenticatedUser currentUser, TicketFilterRequest filterRequest) {
        validateTicketFilter(filterRequest);

        Pageable pageable = PageRequest.of(
            filterRequest.page(),
            filterRequest.size(),
            Sort.by(filterRequest.direction(), filterRequest.sortBy())
        );

        Page<TicketEntity> page = ticketRepository.findAll(buildSpecification(currentUser, filterRequest), pageable);
        Map<String, UserEntity> usersById = loadUsersById(
            page.getContent().stream().flatMap(ticket -> java.util.stream.Stream.of(ticket.getRequesterId(), ticket.getAssignedAgentId())).filter(Objects::nonNull).toList()
        );
        Map<String, CategoryEntity> categoriesById = loadCategoriesById(page.getContent().stream().map(TicketEntity::getCategoryId).toList());

        return PageResponse.fromPage(page.map(ticket -> toSummaryResponse(ticket, usersById, categoriesById)));
    }

    @Transactional(readOnly = true)
    public TicketDetailResponse getById(AuthenticatedUser currentUser, String ticketId) {
        TicketEntity ticket = findTicket(ticketId);
        ensureCanViewTicket(currentUser, ticket);
        return toDetailResponse(ticket, currentUser);
    }

    @Transactional
    public TicketDetailResponse create(AuthenticatedUser currentUser, CreateTicketRequest request, String idempotencyKey) {
        authorizationService.requirePermission(currentUser, Permission.TICKET_CREATE);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ValidationException("IDEMPOTENCY_KEY_REQUIRED", "The idempotency key is required.");
        }

        UserEntity requester = userRepository.findById(currentUser.id())
            .orElseThrow(() -> new UnauthorizedException(
                "AUTHENTICATED_USER_NOT_FOUND",
                "The authenticated user no longer exists. Please sign in again."
            ));

        String requestHash = hashingService.hash(currentUser.id() + "|" + normalize(request.title()) + "|" + normalize(request.description()) + "|" + request.categoryId() + "|" + request.priority().name());
        Optional<IdempotencyRecordEntity> existingRecord = idempotencyRecordRepository.findByIdempotencyKeyAndUserId(idempotencyKey, currentUser.id());
        if (existingRecord.isPresent()) {
            IdempotencyRecordEntity record = existingRecord.get();
            if (!record.getRequestHash().equals(requestHash)) {
                throw new ConflictException("IDEMPOTENCY_KEY_CONFLICT", "The idempotency key was already used with a different payload.");
            }
            return deserializeStoredResponse(record.getResponseBody());
        }

        CategoryEntity category = findActiveCategory(request.categoryId());
        SlaPolicyEntity slaPolicy = findActiveSlaPolicy(request.priority());

        Instant now = clock.instant();
        TicketEntity ticket = new TicketEntity();
        ticket.setCode(generateTicketCode(now));
        ticket.setTitle(normalize(request.title()));
        ticket.setDescription(normalize(request.description()));
        ticket.setStatus(TicketStatus.CREATED);
        ticket.setPriority(request.priority());
        ticket.setRequesterId(requester.getId());
        ticket.setCategoryId(category.getId());
        ticket.setFirstResponseDueAt(now.plus(slaPolicy.getFirstResponseHours(), ChronoUnit.HOURS));
        ticket.setResolutionDueAt(now.plus(slaPolicy.getResolutionHours(), ChronoUnit.HOURS));
        ticketRepository.save(ticket);

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
    }

    @Transactional
    public TicketDetailResponse update(AuthenticatedUser currentUser, String ticketId, UpdateTicketRequest request) {
        TicketEntity ticket = findTicket(ticketId);
        ensureCanUpdateTicket(currentUser, ticket);
        ensureVersion(ticket, request.version());

        String previousTitle = ticket.getTitle();
        String previousDescription = ticket.getDescription();
        String previousCategoryId = ticket.getCategoryId();
        TicketPriority previousPriority = ticket.getPriority();

        if (request.title() != null && !request.title().isBlank()) {
            ticket.setTitle(normalize(request.title()));
        }
        if (request.description() != null && !request.description().isBlank()) {
            ticket.setDescription(normalize(request.description()));
        }
        if (request.categoryId() != null && !request.categoryId().isBlank()) {
            CategoryEntity category = findActiveCategory(request.categoryId());
            ticket.setCategoryId(category.getId());
        }
        if (request.priority() != null && request.priority() != ticket.getPriority()) {
            ticket.setPriority(request.priority());
            recalculateSlaForPriorityChange(ticket, findActiveSlaPolicy(request.priority()));
        }

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
    }

    @Transactional
    public TicketDetailResponse assign(AuthenticatedUser currentUser, String ticketId, AssignTicketRequest request) {
        TicketEntity ticket = findTicket(ticketId);
        authorizationService.requirePermission(currentUser, ticket.getAssignedAgentId() == null ? Permission.TICKET_ASSIGN : Permission.TICKET_REASSIGN);
        ensureVersion(ticket, request.version());
        ensureNotTerminal(ticket);

        UserEntity assignee = userRepository.findById(request.agentId())
            .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "The assignee could not be found."));
        if (!assignee.isActive()) {
            throw new ValidationException("ASSIGNEE_INACTIVE", "The assignee must be active.");
        }
        if (assignee.getRole() != Role.SUPPORT_AGENT && assignee.getRole() != Role.SUPPORT_MANAGER) {
            throw new ValidationException("INVALID_ASSIGNEE_ROLE", "The assignee must be a support user.");
        }

        String previousAgentId = ticket.getAssignedAgentId();
        ticket.setAssignedAgentId(assignee.getId());
        if (ticket.getStatus() == TicketStatus.CREATED) {
            ticket.setStatus(TicketStatus.ASSIGNED);
        }

        addHistory(ticket.getId(), previousAgentId == null ? TicketHistoryAction.ASSIGNED : TicketHistoryAction.REASSIGNED, currentUser.id(), previousAgentId, assignee.getId(), null);
        notifyUser(assignee.getId(), NotificationType.TICKET_ASSIGNED, "Ticket asignado", "Se te asigno el ticket " + ticket.getCode() + ".", ticket.getId());
        return toDetailResponse(ticket, currentUser);
    }

    @Transactional
    public TicketDetailResponse start(AuthenticatedUser currentUser, String ticketId, VersionedRequest request) {
        TicketEntity ticket = findTicket(ticketId);
        ensureCanOperateTicket(currentUser, ticket);
        ensureVersion(ticket, request.version());
        if (ticket.getStatus() != TicketStatus.ASSIGNED) {
            throw new ValidationException("INVALID_TICKET_TRANSITION", "The ticket must be in ASSIGNED status.");
        }

        ticket.setStatus(TicketStatus.IN_PROGRESS);
        applyFirstResponseIfMissing(ticket);
        addHistory(ticket.getId(), TicketHistoryAction.STARTED, currentUser.id(), TicketStatus.ASSIGNED.name(), TicketStatus.IN_PROGRESS.name(), null);
        return toDetailResponse(ticket, currentUser);
    }

    @Transactional
    public TicketDetailResponse requestInformation(AuthenticatedUser currentUser, String ticketId, RequestInformationRequest request) {
        TicketEntity ticket = findTicket(ticketId);
        ensureCanOperateTicket(currentUser, ticket);
        ensureVersion(ticket, request.version());
        if (ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            throw new ValidationException("INVALID_TICKET_TRANSITION", "The ticket must be in IN_PROGRESS status.");
        }
        addCommentInternal(currentUser, ticket, request.content(), CommentVisibility.PUBLIC);
        ticket.setStatus(TicketStatus.WAITING_FOR_CUSTOMER);
        pauseResolutionSla(ticket);
        addHistory(ticket.getId(), TicketHistoryAction.REQUESTED_INFORMATION, currentUser.id(), TicketStatus.IN_PROGRESS.name(), TicketStatus.WAITING_FOR_CUSTOMER.name(), null);
        notifyUser(ticket.getRequesterId(), NotificationType.INFORMATION_REQUESTED, "Se requiere informacion", "Hay una solicitud de informacion en el ticket " + ticket.getCode() + ".", ticket.getId());
        return toDetailResponse(ticket, currentUser);
    }

    @Transactional
    public TicketDetailResponse resolve(AuthenticatedUser currentUser, String ticketId, ResolveTicketRequest request) {
        TicketEntity ticket = findTicket(ticketId);
        ensureCanOperateTicket(currentUser, ticket);
        ensureVersion(ticket, request.version());
        if (ticket.getStatus() != TicketStatus.IN_PROGRESS && ticket.getStatus() != TicketStatus.WAITING_FOR_CUSTOMER) {
            throw new ValidationException("INVALID_TICKET_TRANSITION", "The ticket cannot be resolved from its current status.");
        }
        if (request.resolutionSummary() == null || request.resolutionSummary().isBlank()) {
            throw new ValidationException("RESOLUTION_SUMMARY_REQUIRED", "The resolution summary is required.");
        }

        resumeResolutionSla(ticket);
        applyFirstResponseIfMissing(ticket);
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolutionSummary(normalize(request.resolutionSummary()));
        ticket.setResolvedAt(clock.instant());
        if (ticket.getResolvedAt().isAfter(ticket.getResolutionDueAt())) {
            ticket.setSlaResolutionBreached(true);
        }

        addHistory(ticket.getId(), TicketHistoryAction.RESOLVED, currentUser.id(), null, ticket.getResolutionSummary(), null);
        notifyUser(ticket.getRequesterId(), NotificationType.TICKET_RESOLVED, "Ticket resuelto", "El ticket " + ticket.getCode() + " fue resuelto.", ticket.getId());
        return toDetailResponse(ticket, currentUser);
    }

    @Transactional
    public TicketDetailResponse close(AuthenticatedUser currentUser, String ticketId, VersionedRequest request) {
        TicketEntity ticket = findTicket(ticketId);
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

        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setClosedAt(clock.instant());
        addHistory(ticket.getId(), TicketHistoryAction.CLOSED, currentUser.id(), null, null, null);
        notifyUser(ticket.getRequesterId(), NotificationType.TICKET_CLOSED, "Ticket cerrado", "El ticket " + ticket.getCode() + " fue cerrado.", ticket.getId());
        return toDetailResponse(ticket, currentUser);
    }

    @Transactional
    public TicketDetailResponse reopen(AuthenticatedUser currentUser, String ticketId, ReopenTicketRequest request) {
        TicketEntity ticket = findTicket(ticketId);
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

        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setResolvedAt(null);
        ticket.setResolutionSummary(null);
        ticket.setResolutionDueAt(clock.instant().plus(findActiveSlaPolicy(ticket.getPriority()).getResolutionHours(), ChronoUnit.HOURS));
        ticket.setSlaResolutionBreached(false);
        addHistory(ticket.getId(), TicketHistoryAction.REOPENED, currentUser.id(), TicketStatus.RESOLVED.name(), TicketStatus.IN_PROGRESS.name(), "{\"reason\":\"" + escapeJson(request.reason()) + "\"}");
        if (ticket.getAssignedAgentId() != null) {
            notifyUser(ticket.getAssignedAgentId(), NotificationType.TICKET_REOPENED, "Ticket reabierto", "El ticket " + ticket.getCode() + " fue reabierto.", ticket.getId());
        }
        return toDetailResponse(ticket, currentUser);
    }

    @Transactional
    public TicketDetailResponse cancel(AuthenticatedUser currentUser, String ticketId, CancelTicketRequest request) {
        TicketEntity ticket = findTicket(ticketId);
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

        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setCancelledAt(clock.instant());
        addHistory(ticket.getId(), TicketHistoryAction.CANCELLED, currentUser.id(), null, null, "{\"reason\":\"" + escapeJson(request.reason()) + "\"}");
        return toDetailResponse(ticket, currentUser);
    }

    @Transactional(readOnly = true)
    public List<TicketCommentResponse> listComments(AuthenticatedUser currentUser, String ticketId) {
        TicketEntity ticket = findTicket(ticketId);
        ensureCanViewTicket(currentUser, ticket);
        Map<String, UserEntity> usersById = loadUsersById(ticketCommentRepository.findAllByTicketIdOrderByCreatedAtAsc(ticketId).stream().map(TicketCommentEntity::getAuthorId).toList());

        return ticketCommentRepository.findAllByTicketIdOrderByCreatedAtAsc(ticketId)
            .stream()
            .filter(comment -> comment.getVisibility() == CommentVisibility.PUBLIC || canSeeInternalComments(currentUser))
            .map(comment -> toCommentResponse(comment, usersById))
            .toList();
    }

    @Transactional
    public TicketCommentResponse addComment(AuthenticatedUser currentUser, String ticketId, AddCommentRequest request) {
        TicketEntity ticket = findTicket(ticketId);
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

        TicketCommentEntity comment = addCommentInternal(currentUser, ticket, request.content(), visibility);
        if (currentUser.role() == Role.CUSTOMER && ticket.getStatus() == TicketStatus.WAITING_FOR_CUSTOMER && visibility == CommentVisibility.PUBLIC) {
            resumeResolutionSla(ticket);
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        if (currentUser.role() != Role.CUSTOMER && visibility == CommentVisibility.PUBLIC && ticket.getRequesterId() != null) {
            notifyUser(ticket.getRequesterId(), NotificationType.PUBLIC_COMMENT_ADDED, "Nuevo comentario", "Hay un nuevo comentario en el ticket " + ticket.getCode() + ".", ticket.getId());
        }
        if (currentUser.role() == Role.CUSTOMER && ticket.getAssignedAgentId() != null) {
            notifyUser(ticket.getAssignedAgentId(), NotificationType.PUBLIC_COMMENT_ADDED, "Respuesta del cliente", "El cliente respondio en el ticket " + ticket.getCode() + ".", ticket.getId());
        }

        Map<String, UserEntity> usersById = loadUsersById(List.of(comment.getAuthorId()));
        return toCommentResponse(comment, usersById);
    }

    @Transactional(readOnly = true)
    public List<TicketHistoryResponse> listHistory(AuthenticatedUser currentUser, String ticketId) {
        TicketEntity ticket = findTicket(ticketId);
        ensureCanViewTicket(currentUser, ticket);
        authorizationService.requirePermission(currentUser, Permission.AUDIT_READ);
        List<TicketHistoryEntity> historyEntries = ticketHistoryRepository.findAllByTicketIdOrderByCreatedAtDesc(ticketId);
        Map<String, UserEntity> usersById = loadUsersById(historyEntries.stream().map(TicketHistoryEntity::getPerformedBy).toList());

        return historyEntries.stream()
            .map(entry -> new TicketHistoryResponse(
                entry.getId(),
                entry.getAction(),
                entry.getPerformedBy(),
                displayName(usersById.get(entry.getPerformedBy())),
                entry.getPreviousValue(),
                entry.getNewValue(),
                entry.getMetadataJson(),
                entry.getCreatedAt()
            ))
            .toList();
    }

    @Transactional
    public void purgeExpiredIdempotencyRecords() {
        idempotencyRecordRepository.deleteByExpiresAtBefore(clock.instant());
    }

    @Transactional
    public void autoCloseResolvedTickets() {
        Instant resolvedBefore = clock.instant().minus(ticketLifecycleProperties.getAutoCloseDays(), ChronoUnit.DAYS);
        ticketRepository.findAllByStatusAndResolvedAtBefore(TicketStatus.RESOLVED, resolvedBefore)
            .forEach(ticket -> {
                ticket.setStatus(TicketStatus.CLOSED);
                ticket.setClosedAt(clock.instant());
                addHistory(
                    ticket.getId(),
                    TicketHistoryAction.CLOSED,
                    SYSTEM_ACTOR,
                    TicketStatus.RESOLVED.name(),
                    TicketStatus.CLOSED.name(),
                    "{\"source\":\"auto-close\"}"
                );
                notifyUser(
                    ticket.getRequesterId(),
                    NotificationType.TICKET_CLOSED,
                    "Ticket cerrado automaticamente",
                    "El ticket " + ticket.getCode() + " fue cerrado automaticamente por inactividad.",
                    ticket.getId()
                );
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

    private Specification<TicketEntity> buildSpecification(AuthenticatedUser currentUser, TicketFilterRequest filterRequest) {
        Specification<TicketEntity> specification = accessSpecification(currentUser);

        if (filterRequest.search() != null && !filterRequest.search().isBlank()) {
            String search = "%" + filterRequest.search().trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), search),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), search),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), search)
            ));
        }
        if (filterRequest.status() != null) {
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), filterRequest.status()));
        }
        if (filterRequest.priority() != null) {
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("priority"), filterRequest.priority()));
        }
        if (filterRequest.categoryId() != null && !filterRequest.categoryId().isBlank()) {
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("categoryId"), filterRequest.categoryId()));
        }
        if (filterRequest.assignedAgentId() != null && !filterRequest.assignedAgentId().isBlank()) {
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("assignedAgentId"), filterRequest.assignedAgentId()));
        }
        if (filterRequest.createdFrom() != null) {
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), filterRequest.createdFrom()));
        }
        if (filterRequest.createdTo() != null) {
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), filterRequest.createdTo()));
        }

        return specification;
    }

    private Specification<TicketEntity> accessSpecification(AuthenticatedUser currentUser) {
        if (currentUser.hasPermission(Permission.TICKET_READ_ALL)) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        }
        if (currentUser.hasPermission(Permission.TICKET_READ_ASSIGNED)) {
            return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.equal(root.get("assignedAgentId"), currentUser.id()),
                criteriaBuilder.isNull(root.get("assignedAgentId"))
            );
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("requesterId"), currentUser.id());
    }

    private TicketEntity findTicket(String ticketId) {
        return ticketRepository.findById(ticketId)
            .orElseThrow(() -> new NotFoundException("TICKET_NOT_FOUND", "The ticket could not be found."));
    }

    private CategoryEntity findActiveCategory(String categoryId) {
        CategoryEntity category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "The category could not be found."));
        if (!category.isActive()) {
            throw new ValidationException("CATEGORY_INACTIVE", "The category is inactive.");
        }
        return category;
    }

    private SlaPolicyEntity findActiveSlaPolicy(TicketPriority priority) {
        SlaPolicyEntity policy = slaPolicyRepository.findByPriority(priority)
            .orElseThrow(() -> new NotFoundException("SLA_POLICY_NOT_FOUND", "The SLA policy could not be found."));
        if (!policy.isActive()) {
            throw new ValidationException("SLA_POLICY_INACTIVE", "The SLA policy is inactive.");
        }
        return policy;
    }

    private void ensureCanViewTicket(AuthenticatedUser currentUser, TicketEntity ticket) {
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

    private void ensureCanUpdateTicket(AuthenticatedUser currentUser, TicketEntity ticket) {
        if (ticket.getStatus() == TicketStatus.CLOSED || ticket.getStatus() == TicketStatus.CANCELLED) {
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

    private void ensureCanOperateTicket(AuthenticatedUser currentUser, TicketEntity ticket) {
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

    private void ensureVersion(TicketEntity ticket, long version) {
        if (ticket.getVersion() != version) {
            throw new ConflictException("RESOURCE_VERSION_CONFLICT", "The ticket was modified by another request.");
        }
    }

    private void ensureNotTerminal(TicketEntity ticket) {
        if (isTerminal(ticket)) {
            throw new ValidationException("TICKET_TERMINAL", "Terminal tickets cannot be modified.");
        }
    }

    private boolean isTerminal(TicketEntity ticket) {
        return ticket.getStatus() == TicketStatus.CLOSED || ticket.getStatus() == TicketStatus.CANCELLED;
    }

    private TicketCommentEntity addCommentInternal(AuthenticatedUser currentUser, TicketEntity ticket, String content, CommentVisibility visibility) {
        TicketCommentEntity comment = new TicketCommentEntity();
        comment.setTicketId(ticket.getId());
        comment.setAuthorId(currentUser.id());
        comment.setContent(normalize(content));
        comment.setVisibility(visibility);
        TicketCommentEntity savedComment = ticketCommentRepository.save(comment);

        if (visibility == CommentVisibility.PUBLIC && currentUser.role() != Role.CUSTOMER) {
            applyFirstResponseIfMissing(ticket);
        }

        addHistory(
            ticket.getId(),
            visibility == CommentVisibility.PUBLIC ? TicketHistoryAction.COMMENT_ADDED_PUBLIC : TicketHistoryAction.COMMENT_ADDED_INTERNAL,
            currentUser.id(),
            null,
            null,
            "{\"commentId\":\"" + savedComment.getId() + "\"}"
        );
        return savedComment;
    }

    private void applyFirstResponseIfMissing(TicketEntity ticket) {
        if (ticket.getFirstRespondedAt() == null) {
            Instant now = clock.instant();
            ticket.setFirstRespondedAt(now);
            if (now.isAfter(ticket.getFirstResponseDueAt())) {
                ticket.setSlaFirstResponseBreached(true);
            }
        }
    }

    private void pauseResolutionSla(TicketEntity ticket) {
        if (ticket.getSlaPausedAt() == null) {
            ticket.setSlaPausedAt(clock.instant());
        }
    }

    private void resumeResolutionSla(TicketEntity ticket) {
        if (ticket.getSlaPausedAt() != null) {
            Instant now = clock.instant();
            long pausedSeconds = ChronoUnit.SECONDS.between(ticket.getSlaPausedAt(), now);
            ticket.setAccumulatedPausedSeconds(ticket.getAccumulatedPausedSeconds() + pausedSeconds);
            ticket.setResolutionDueAt(ticket.getResolutionDueAt().plusSeconds(pausedSeconds));
            ticket.setSlaPausedAt(null);
        }
    }

    private void recalculateSlaForPriorityChange(TicketEntity ticket, SlaPolicyEntity slaPolicy) {
        if (!ticket.isSlaFirstResponseBreached() && ticket.getFirstRespondedAt() == null) {
            ticket.setFirstResponseDueAt(ticket.getCreatedAt().plus(slaPolicy.getFirstResponseHours(), ChronoUnit.HOURS));
        }
        if (!ticket.isSlaResolutionBreached() && ticket.getStatus() != TicketStatus.RESOLVED && ticket.getStatus() != TicketStatus.CLOSED && ticket.getStatus() != TicketStatus.CANCELLED) {
            ticket.setResolutionDueAt(
                ticket.getCreatedAt()
                    .plus(slaPolicy.getResolutionHours(), ChronoUnit.HOURS)
                    .plusSeconds(ticket.getAccumulatedPausedSeconds())
            );
        }
    }

    private String generateTicketCode(Instant now) {
        int currentYear = now.atZone(ZoneOffset.UTC).getYear();
        TicketSequenceEntity sequence = ticketSequenceRepository.findByYearForUpdate(currentYear)
            .orElseGet(() -> {
                TicketSequenceEntity createdSequence = new TicketSequenceEntity();
                createdSequence.setSequenceYear(currentYear);
                createdSequence.setCurrentValue(0L);
                return ticketSequenceRepository.save(createdSequence);
            });

        sequence.setCurrentValue(sequence.getCurrentValue() + 1);
        ticketSequenceRepository.save(sequence);
        return "TCK-" + currentYear + "-" + String.format("%06d", sequence.getCurrentValue());
    }

    private void addHistory(String ticketId, TicketHistoryAction action, String performedBy, String previousValue, String newValue, String metadataJson) {
        TicketHistoryEntity history = new TicketHistoryEntity();
        history.setTicketId(ticketId);
        history.setAction(action);
        history.setPerformedBy(performedBy);
        history.setPreviousValue(previousValue);
        history.setNewValue(newValue);
        history.setMetadataJson(metadataJson);
        ticketHistoryRepository.save(history);
    }

    private void notifySupportUsers(NotificationType type, String title, String message, String ticketId) {
        List<UserEntity> recipients = userRepository.findAllByRoleInAndActiveTrue(List.of(Role.ADMIN, Role.SUPPORT_MANAGER));
        recipients.forEach(user -> notifyUser(user.getId(), type, title, message, ticketId));
    }

    private void notifyUser(String recipientId, NotificationType type, String title, String message, String ticketId) {
        NotificationEntity notification = new NotificationEntity();
        notification.setRecipientId(recipientId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedTicketId(ticketId);
        notification.setRead(false);
        notificationRepository.save(notification);
    }

    private void storeIdempotencyRecord(String idempotencyKey, String userId, String requestHash, String resourceId, TicketDetailResponse response) {
        try {
            IdempotencyRecordEntity record = new IdempotencyRecordEntity();
            record.setIdempotencyKey(idempotencyKey);
            record.setUserId(userId);
            record.setRequestHash(requestHash);
            record.setResponseStatus(201);
            record.setResponseBody(objectMapper.writeValueAsString(response));
            record.setResourceId(resourceId);
            record.setExpiresAt(clock.instant().plus(idempotencyProperties.getRecordTtlHours(), ChronoUnit.HOURS));
            idempotencyRecordRepository.save(record);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("The ticket response could not be serialized.", exception);
        }
    }

    private TicketDetailResponse deserializeStoredResponse(String responseBody) {
        try {
            return objectMapper.readValue(responseBody, TicketDetailResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("The stored idempotency response could not be deserialized.", exception);
        }
    }

    private Map<String, UserEntity> loadUsersById(Collection<String> userIds) {
        Set<String> uniqueIds = userIds.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (uniqueIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(uniqueIds)
            .stream()
            .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
    }

    private Map<String, CategoryEntity> loadCategoriesById(Collection<String> categoryIds) {
        Set<String> uniqueIds = categoryIds.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (uniqueIds.isEmpty()) {
            return Map.of();
        }
        return categoryRepository.findAllById(uniqueIds)
            .stream()
            .collect(Collectors.toMap(CategoryEntity::getId, Function.identity()));
    }

    private TicketSummaryResponse toSummaryResponse(
        TicketEntity ticket,
        Map<String, UserEntity> usersById,
        Map<String, CategoryEntity> categoriesById
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
            categoriesById.containsKey(ticket.getCategoryId()) ? categoriesById.get(ticket.getCategoryId()).getName() : null,
            ticket.getResolutionDueAt(),
            ticket.isSlaFirstResponseBreached(),
            ticket.isSlaResolutionBreached(),
            ticket.getCreatedAt(),
            ticket.getUpdatedAt(),
            ticket.getVersion()
        );
    }

    private TicketDetailResponse toDetailResponse(TicketEntity ticket, AuthenticatedUser currentUser) {
        Map<String, UserEntity> usersById = loadUsersById(
            java.util.stream.Stream.of(ticket.getRequesterId(), ticket.getAssignedAgentId())
                .filter(Objects::nonNull)
                .toList()
        );
        Map<String, CategoryEntity> categoriesById = loadCategoriesById(List.of(ticket.getCategoryId()));
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
            categoriesById.containsKey(ticket.getCategoryId()) ? categoriesById.get(ticket.getCategoryId()).getName() : null,
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

    private TicketCommentResponse toCommentResponse(TicketCommentEntity comment, Map<String, UserEntity> usersById) {
        return new TicketCommentResponse(
            comment.getId(),
            comment.getTicketId(),
            comment.getAuthorId(),
            displayName(usersById.get(comment.getAuthorId())),
            comment.getContent(),
            comment.getVisibility(),
            comment.getCreatedAt(),
            comment.getUpdatedAt()
        );
    }

    private List<String> availableActions(AuthenticatedUser currentUser, TicketEntity ticket) {
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
        if (!isTerminal(ticket) && (currentUser.hasPermission(Permission.TICKET_ASSIGN) || currentUser.hasPermission(Permission.TICKET_REASSIGN))) {
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
        if (!isTerminal(ticket) && (
            currentUser.hasPermission(Permission.COMMENT_CREATE_PUBLIC) ||
                currentUser.hasPermission(Permission.COMMENT_CREATE_INTERNAL)
        )) {
            actions.add("comment");
        }
        return actions;
    }

    private String displayName(UserEntity user) {
        if (user == null) {
            return null;
        }
        return user.getFirstName() + " " + user.getLastName();
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
        Sort.Direction direction
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
