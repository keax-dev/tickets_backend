package com.tickets.managementtickets.dashboard.application.service;

import com.tickets.managementtickets.dashboard.application.result.DashboardSummaryResponse;
import com.tickets.managementtickets.dashboard.application.result.RecentActivityResponse;
import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.application.port.UserRepositoryPort;
import com.tickets.managementtickets.identity.application.service.AuthorizationService;
import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.identity.domain.model.User;
import com.tickets.managementtickets.shared.application.port.TransactionRunner;
import com.tickets.managementtickets.ticket.application.port.TicketCountQuery;
import com.tickets.managementtickets.ticket.application.port.TicketHistoryRepositoryPort;
import com.tickets.managementtickets.ticket.application.port.TicketRepositoryPort;
import com.tickets.managementtickets.ticket.application.port.TicketVisibility;
import com.tickets.managementtickets.ticket.domain.model.TicketHistory;
import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class DashboardService {

    private static final int RECENT_ACTIVITY_LIMIT = 20;
    private static final long DUE_SOON_WINDOW_HOURS = 4;

    private final TicketRepositoryPort ticketRepository;
    private final TicketHistoryRepositoryPort ticketHistoryRepository;
    private final UserRepositoryPort userRepository;
    private final AuthorizationService authorizationService;
    private final Clock clock;
    private final TransactionRunner transactionRunner;

    public DashboardService(
        TicketRepositoryPort ticketRepository,
        TicketHistoryRepositoryPort ticketHistoryRepository,
        UserRepositoryPort userRepository,
        AuthorizationService authorizationService,
        Clock clock,
        TransactionRunner transactionRunner
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketHistoryRepository = ticketHistoryRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
        this.clock = clock;
        this.transactionRunner = transactionRunner;
    }

    public DashboardSummaryResponse summary(AuthenticatedUser currentUser) {
        return transactionRunner.readOnly(() -> {
            authorizationService.requireAnyPermission(
                currentUser,
                Permission.DASHBOARD_READ_GLOBAL,
                Permission.DASHBOARD_READ_PERSONAL
            );
            Instant now = clock.instant();
            Instant startOfDay = now.truncatedTo(ChronoUnit.DAYS);
            TicketCountQuery baseQuery = TicketCountQuery.visibleTo(resolveVisibility(currentUser), currentUser.id());
            Map<TicketStatus, Long> byStatus = new EnumMap<>(TicketStatus.class);
            Map<TicketPriority, Long> byPriority = new EnumMap<>(TicketPriority.class);
            for (TicketStatus status : TicketStatus.values()) {
                byStatus.put(status, ticketRepository.count(baseQuery.withStatus(status)));
            }
            for (TicketPriority priority : TicketPriority.values()) {
                byPriority.put(priority, ticketRepository.count(baseQuery.withPriority(priority)));
            }

            long activeTickets = ticketRepository.count(baseQuery.withoutTerminalStatuses());
            long createdToday = ticketRepository.count(baseQuery.createdAtFrom(startOfDay));
            long unassignedTickets = ticketRepository.count(baseQuery.unassignedOnly());
            long breachedTickets = ticketRepository.count(baseQuery.breachedSlaOnly());
            long dueSoonTickets = ticketRepository.count(baseQuery.resolutionDueBetween(now, now.plus(DUE_SOON_WINDOW_HOURS, ChronoUnit.HOURS)));
            long assignedToCurrentUser = ticketRepository.count(baseQuery.assignedTo(currentUser.id()));

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
        });
    }

    public List<RecentActivityResponse> recentActivity(AuthenticatedUser currentUser) {
        return transactionRunner.readOnly(() -> {
            authorizationService.requireAnyPermission(
                currentUser,
                Permission.DASHBOARD_READ_GLOBAL,
                Permission.DASHBOARD_READ_PERSONAL
            );
            List<TicketHistory> historyEntries = ticketHistoryRepository.findRecent(resolveVisibility(currentUser), currentUser.id(), RECENT_ACTIVITY_LIMIT);
            Map<String, User> usersById = userRepository.findAllById(
                historyEntries.stream().map(TicketHistory::performedBy).filter(Objects::nonNull).collect(Collectors.toSet())
            ).stream().collect(Collectors.toMap(User::id, user -> user));

            return historyEntries.stream()
                .map(entry -> new RecentActivityResponse(
                    entry.id(),
                    entry.ticketId(),
                    entry.action(),
                    usersById.containsKey(entry.performedBy()) ? usersById.get(entry.performedBy()).displayName() : null,
                    entry.createdAt()
                ))
                .toList();
        });
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

}
