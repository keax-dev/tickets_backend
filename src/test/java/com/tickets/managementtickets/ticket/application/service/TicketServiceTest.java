package com.tickets.managementtickets.ticket.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickets.managementtickets.category.application.port.CategoryRepositoryPort;
import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.application.port.UserRepositoryPort;
import com.tickets.managementtickets.identity.application.service.AuthorizationService;
import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.identity.domain.model.Role;
import com.tickets.managementtickets.notification.application.port.NotificationRepositoryPort;
import com.tickets.managementtickets.shared.application.exception.BadRequestException;
import com.tickets.managementtickets.shared.application.exception.UnauthorizedException;
import com.tickets.managementtickets.shared.application.model.SortDirection;
import com.tickets.managementtickets.shared.application.port.HashingService;
import com.tickets.managementtickets.shared.application.port.JsonCodec;
import com.tickets.managementtickets.shared.application.port.TransactionRunner;
import com.tickets.managementtickets.sla.application.port.SlaPolicyRepositoryPort;
import com.tickets.managementtickets.ticket.application.command.CreateTicketRequest;
import com.tickets.managementtickets.ticket.application.port.IdempotencyPolicy;
import com.tickets.managementtickets.ticket.application.port.IdempotencyRecordRepositoryPort;
import com.tickets.managementtickets.ticket.application.port.TicketCodeGenerator;
import com.tickets.managementtickets.ticket.application.port.TicketCommentRepositoryPort;
import com.tickets.managementtickets.ticket.application.port.TicketHistoryRepositoryPort;
import com.tickets.managementtickets.ticket.application.port.TicketLifecyclePolicy;
import com.tickets.managementtickets.ticket.application.port.TicketRepositoryPort;
import com.tickets.managementtickets.ticket.application.query.TicketFilterRequest;
import com.tickets.managementtickets.ticket.application.result.TicketDetailResponse;
import com.tickets.managementtickets.ticket.domain.model.Ticket;
import com.tickets.managementtickets.ticket.domain.model.TicketHistory;
import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
// This test suite verifies ticket application-service behavior around filtering, auth edge cases, mapping, and scheduled closure.
class TicketServiceTest {

    // Repository mock for the main ticket aggregate persistence boundary.
    @Mock
    private TicketRepositoryPort ticketRepository;

    // Repository mock for ticket comments; kept here because TicketService coordinates this dependency in other flows.
    @Mock
    private TicketCommentRepositoryPort ticketCommentRepository;

    // Repository mock for the ticket audit trail.
    @Mock
    private TicketHistoryRepositoryPort ticketHistoryRepository;

    // Repository mock for idempotency records used by create/update commands.
    @Mock
    private IdempotencyRecordRepositoryPort idempotencyRecordRepository;

    // Repository mock for users loaded by the service during authorization and mapping.
    @Mock
    private UserRepositoryPort userRepository;

    // Repository mock for category lookups.
    @Mock
    private CategoryRepositoryPort categoryRepository;

    // Repository mock for SLA policy lookups.
    @Mock
    private SlaPolicyRepositoryPort slaPolicyRepository;

    // Repository mock for outbound notifications triggered by use cases.
    @Mock
    private NotificationRepositoryPort notificationRepository;

    // Generator mock for human-readable ticket codes.
    @Mock
    private TicketCodeGenerator ticketCodeGenerator;

    // Authorization domain/application collaborator used by protected use cases.
    @Mock
    private AuthorizationService authorizationService;

    // Hashing collaborator used by idempotency and related logic.
    @Mock
    private HashingService hashingService;

    // System under test: this is the real application service instance wired with mocks/fakes.
    private TicketService ticketService;
    // Shared authenticated principal reused by read-oriented tests.
    private AuthenticatedUser currentUser;

    @BeforeEach
    void setUp() {
        // Arrange shared test fixture: use direct transactions and a real JSON codec over a mocked persistence boundary.
        IdempotencyPolicy idempotencyPolicy = () -> 24;
        TicketLifecyclePolicy ticketLifecyclePolicy = () -> 7;
        TransactionRunner transactionRunner = new TransactionRunner() {
            @Override
            public <T> T readOnly(java.util.function.Supplier<T> action) {
                // Execute the callback immediately because unit tests do not need a real transaction manager.
                return action.get();
            }

            @Override
            public <T> T required(java.util.function.Supplier<T> action) {
                // Execute the callback immediately for the same reason.
                return action.get();
            }
        };
        ObjectMapper objectMapper = new ObjectMapper();
        JsonCodec jsonCodec = new JsonCodec() {
            @Override
            public String serialize(Object value) {
                try {
                    // Reuse Jackson to mimic the real JSON serialization behavior used by the service.
                    return objectMapper.writeValueAsString(value);
                } catch (Exception exception) {
                    // Fail fast if a test fixture cannot be serialized.
                    throw new IllegalStateException(exception);
                }
            }

            @Override
            public <T> T deserialize(String value, Class<T> type) {
                try {
                    // Reuse Jackson to mimic the real JSON deserialization behavior used by the service.
                    return objectMapper.readValue(value, type);
                } catch (Exception exception) {
                    // Fail fast if a test fixture cannot be deserialized.
                    throw new IllegalStateException(exception);
                }
            }
        };

        // Instantiate the real service with mocked outbound ports, a fixed clock, and lightweight fakes.
        ticketService = new TicketService(
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
            Clock.fixed(Instant.parse("2026-07-16T00:00:00Z"), ZoneOffset.UTC),
            idempotencyPolicy,
            ticketLifecyclePolicy,
            transactionRunner
        );

        currentUser = new AuthenticatedUser(
            "user-1",
            "user@test.com",
            "User",
            "Test",
            Role.CUSTOMER,
            // This user can read only their own tickets, which is enough for the scenarios below.
            Set.of(Permission.TICKET_READ_OWN)
        );
    }

    @Test
    void shouldRejectUnsupportedSortField() {
        // Arrange: build a list filter with a sort field outside the public contract.
        TicketFilterRequest filterRequest = new TicketFilterRequest(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            0,
            10,
            "unsupportedField",
            SortDirection.DESC
        );

        // Act: execute the list use case with the invalid filter.
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> ticketService.list(currentUser, filterRequest)
        );

        // Assert: verify validation stops before repository access.
        assertEquals("INVALID_SORT_FIELD", exception.getCode());
        verifyNoInteractions(ticketRepository);
    }

    @Test
    void shouldRejectInvalidCreatedAtRange() {
        // Arrange: build a filter where the start date is after the end date.
        TicketFilterRequest filterRequest = new TicketFilterRequest(
            null,
            null,
            null,
            null,
            null,
            Instant.parse("2026-07-16T10:00:00Z"),
            Instant.parse("2026-07-16T09:00:00Z"),
            0,
            10,
            "createdAt",
            SortDirection.DESC
        );

        // Act: execute the list use case with the invalid date range.
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> ticketService.list(currentUser, filterRequest)
        );

        // Assert: verify validation stops before repository access.
        assertEquals("INVALID_DATE_RANGE", exception.getCode());
        verifyNoInteractions(ticketRepository);
    }

    @Test
    void shouldRejectCreateWhenAuthenticatedUserNoLongerExists() {
        // Arrange: create an authenticated principal whose user row no longer exists.
        AuthenticatedUser customerUser = new AuthenticatedUser(
            "missing-user",
            "customer@test.com",
            "Customer",
            "User",
            Role.CUSTOMER,
            Set.of(Permission.TICKET_CREATE, Permission.TICKET_READ_OWN)
        );

        // Simulate the repository lookup performed by the service when it rehydrates the requester.
        when(userRepository.findById("missing-user")).thenReturn(Optional.empty());

        // Act: attempt to create a ticket with the stale authenticated principal.
        UnauthorizedException exception = assertThrows(
            UnauthorizedException.class,
            () -> ticketService.create(
                customerUser,
                new CreateTicketRequest(
                    "Computadora sin memoria",
                    "No me permite crear archivos",
                    "category-1",
                    TicketPriority.HIGH
                ),
                "idem-1"
            )
        );

        // Assert: verify the use case fails before ticket persistence or side effects.
        assertEquals("AUTHENTICATED_USER_NOT_FOUND", exception.getCode());
        verifyNoInteractions(ticketRepository, categoryRepository, slaPolicyRepository, notificationRepository, idempotencyRecordRepository);
    }

    @Test
    void shouldReturnTicketDetailWhenAssignedAgentIsNull() {
        // Arrange: mock a visible ticket with no assigned agent.
        Ticket ticket = new Ticket(
            "ticket-1",
            "TCK-2026-000001",
            "Computadora sin memoria",
            "No me permite crear archivos",
            TicketStatus.CREATED,
            TicketPriority.HIGH,
            "user-1",
            null,
            "category-1",
            Instant.parse("2026-07-16T04:00:00Z"),
            Instant.parse("2026-07-17T00:00:00Z"),
            null,
            null,
            null,
            null,
            null,
            0,
            false,
            false,
            null,
            null,
            null,
            0
        );

        when(ticketRepository.findById("ticket-1")).thenReturn(Optional.of(ticket));
        // No requester/agent/category details are needed for this edge case, so return empty lookups.
        when(userRepository.findAllById(anyIterable())).thenReturn(List.of());
        when(categoryRepository.findAllById(anyIterable())).thenReturn(List.of());

        // Act: load the ticket detail.
        TicketDetailResponse response = ticketService.getById(currentUser, "ticket-1");

        // Assert: the service should return the ticket detail instead of failing on a missing assignee.
        assertEquals("ticket-1", response.id());
        // The important part of this regression test: a null assigned agent must remain null in the response.
        assertNull(response.assignedAgentId());
    }

    @Test
    void shouldRecordSystemActorWhenAutoClosingResolvedTickets() {
        // Arrange: mock an old resolved ticket eligible for automatic closure.
        Ticket ticket = new Ticket(
            "ticket-1",
            "TCK-2026-000001",
            "Computadora sin memoria",
            "No me permite crear archivos",
            TicketStatus.RESOLVED,
            TicketPriority.HIGH,
            "user-1",
            "agent-1",
            "category-1",
            Instant.parse("2026-07-10T04:00:00Z"),
            Instant.parse("2026-07-11T00:00:00Z"),
            Instant.parse("2026-07-10T01:00:00Z"),
            Instant.parse("2026-07-08T00:00:00Z"),
            null,
            null,
            null,
            0,
            false,
            false,
            "Resolved",
            Instant.parse("2026-07-01T00:00:00Z"),
            Instant.parse("2026-07-08T00:00:00Z"),
            0
        );

        when(ticketRepository.findAllByStatusAndResolvedAtBefore(eq(TicketStatus.RESOLVED), any(Instant.class))).thenReturn(List.of(ticket));
        // Persist the same ticket instance after the service mutates it, which is enough for this unit test.
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act: run the scheduled auto-close use case.
        ticketService.autoCloseResolvedTickets();

        // Assert: capture the history entry because we want to inspect what actor id the service stored.
        ArgumentCaptor<TicketHistory> historyCaptor = ArgumentCaptor.forClass(TicketHistory.class);
        verify(ticketHistoryRepository).save(historyCaptor.capture());
        // Auto-close is a system action, so the history must use the predefined system actor id.
        assertEquals("00000000-0000-0000-0000-000000000000", historyCaptor.getValue().performedBy());
    }
}
