package com.tickets.managementtickets.dashboard.application.service;

import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.application.service.AuthorizationService;
import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.identity.infrastructure.persistence.entity.UserEntity;
import com.tickets.managementtickets.identity.infrastructure.persistence.repository.UserRepository;
import com.tickets.managementtickets.ticket.domain.model.TicketHistoryAction;
import com.tickets.managementtickets.ticket.domain.model.TicketPriority;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;
import com.tickets.managementtickets.ticket.infrastructure.persistence.entity.TicketEntity;
import com.tickets.managementtickets.ticket.infrastructure.persistence.entity.TicketHistoryEntity;
import com.tickets.managementtickets.ticket.infrastructure.persistence.repository.TicketHistoryRepository;
import com.tickets.managementtickets.ticket.infrastructure.persistence.repository.TicketRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final int RECENT_ACTIVITY_LIMIT = 20;
    private static final long DUE_SOON_WINDOW_HOURS = 4;

    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;
    private final Clock clock;

    public DashboardService(
        TicketRepository ticketRepository,
        TicketHistoryRepository ticketHistoryRepository,
        UserRepository userRepository,
        AuthorizationService authorizationService,
        Clock clock
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketHistoryRepository = ticketHistoryRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(AuthenticatedUser currentUser) {
        authorizationService.requireAnyPermission(
            currentUser,
            Permission.DASHBOARD_READ_GLOBAL,
            Permission.DASHBOARD_READ_PERSONAL
        );
        Instant now = clock.instant();
        Instant startOfDay = now.truncatedTo(ChronoUnit.DAYS);
        Specification<TicketEntity> accessSpecification = accessSpecification(currentUser);
        Map<TicketStatus, Long> byStatus = new EnumMap<>(TicketStatus.class);
        Map<TicketPriority, Long> byPriority = new EnumMap<>(TicketPriority.class);
        for (TicketStatus status : TicketStatus.values()) {
            byStatus.put(status, ticketRepository.count(accessSpecification.and(withStatus(status))));
        }
        for (TicketPriority priority : TicketPriority.values()) {
            byPriority.put(priority, ticketRepository.count(accessSpecification.and(withPriority(priority))));
        }

        long activeTickets = ticketRepository.count(accessSpecification.and(withoutTerminalStatuses()));
        long createdToday = ticketRepository.count(accessSpecification.and(createdAtFrom(startOfDay)));
        long unassignedTickets = ticketRepository.count(accessSpecification.and(unassigned()));
        long breachedTickets = ticketRepository.count(accessSpecification.and(withBreachedSla()));
        long dueSoonTickets = ticketRepository.count(accessSpecification.and(dueSoon(now, now.plus(DUE_SOON_WINDOW_HOURS, ChronoUnit.HOURS))));
        long assignedToCurrentUser = ticketRepository.count(accessSpecification.and(assignedTo(currentUser.id())));

        return new DashboardSummaryResponse(
            activeTickets,
            createdToday,
            unassignedTickets,
            breachedTickets,
            dueSoonTickets,
            assignedToCurrentUser,
            byStatus,
            byPriority
        );
    }

    @Transactional(readOnly = true)
    public List<RecentActivityResponse> recentActivity(AuthenticatedUser currentUser) {
        authorizationService.requireAnyPermission(
            currentUser,
            Permission.DASHBOARD_READ_GLOBAL,
            Permission.DASHBOARD_READ_PERSONAL
        );
        Pageable pageable = PageRequest.of(0, RECENT_ACTIVITY_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<TicketHistoryEntity> historyEntries = loadRecentHistory(currentUser, pageable);
        Map<String, UserEntity> usersById = userRepository.findAllById(
            historyEntries.stream().map(TicketHistoryEntity::getPerformedBy).filter(Objects::nonNull).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(UserEntity::getId, user -> user));

        return historyEntries.stream()
            .map(entry -> new RecentActivityResponse(
                entry.getId(),
                entry.getTicketId(),
                entry.getAction(),
                usersById.containsKey(entry.getPerformedBy()) ? usersById.get(entry.getPerformedBy()).getFirstName() + " " + usersById.get(entry.getPerformedBy()).getLastName() : null,
                entry.getCreatedAt()
            ))
            .toList();
    }

    private List<TicketHistoryEntity> loadRecentHistory(AuthenticatedUser currentUser, Pageable pageable) {
        if (currentUser.hasPermission(Permission.TICKET_READ_ALL)) {
            return ticketHistoryRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        if (currentUser.hasPermission(Permission.TICKET_READ_ASSIGNED)) {
            return ticketHistoryRepository.findRecentVisibleForAssignedUser(currentUser.id(), pageable);
        }
        return ticketHistoryRepository.findRecentVisibleForRequester(currentUser.id(), pageable);
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

    private Specification<TicketEntity> withStatus(TicketStatus status) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
    }

    private Specification<TicketEntity> withPriority(TicketPriority priority) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("priority"), priority);
    }

    private Specification<TicketEntity> withoutTerminalStatuses() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.not(root.get("status").in(TicketStatus.CLOSED, TicketStatus.CANCELLED));
    }

    private Specification<TicketEntity> createdAtFrom(Instant startOfDay) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startOfDay);
    }

    private Specification<TicketEntity> unassigned() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get("assignedAgentId"));
    }

    private Specification<TicketEntity> withBreachedSla() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
            criteriaBuilder.isTrue(root.get("slaFirstResponseBreached")),
            criteriaBuilder.isTrue(root.get("slaResolutionBreached"))
        );
    }

    private Specification<TicketEntity> dueSoon(Instant now, Instant threshold) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
            criteriaBuilder.greaterThan(root.get("resolutionDueAt"), now),
            criteriaBuilder.lessThanOrEqualTo(root.get("resolutionDueAt"), threshold)
        );
    }

    private Specification<TicketEntity> assignedTo(String userId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("assignedAgentId"), userId);
    }

    public record DashboardSummaryResponse(
        long activeTickets,
        long createdToday,
        long unassignedTickets,
        long breachedTickets,
        long dueSoonTickets,
        long assignedToCurrentUser,
        Map<TicketStatus, Long> ticketsByStatus,
        Map<TicketPriority, Long> ticketsByPriority
    ) {
    }

    public record RecentActivityResponse(
        String id,
        String ticketId,
        TicketHistoryAction action,
        String performedByName,
        Instant createdAt
    ) {
    }
}
