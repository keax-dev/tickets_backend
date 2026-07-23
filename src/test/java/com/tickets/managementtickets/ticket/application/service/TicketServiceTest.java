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
import com.tickets.managementtickets.ticket.application.port.IdempotencyPolicy;
import com.tickets.managementtickets.ticket.application.port.IdempotencyRecordRepositoryPort;
import com.tickets.managementtickets.ticket.application.port.TicketCodeGenerator;
import com.tickets.managementtickets.ticket.application.port.TicketCommentRepositoryPort;
import com.tickets.managementtickets.ticket.application.port.TicketHistoryRepositoryPort;
import com.tickets.managementtickets.ticket.application.port.TicketLifecyclePolicy;
import com.tickets.managementtickets.ticket.application.port.TicketRepositoryPort;
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
class TicketServiceTest {

    @Mock
    private TicketRepositoryPort ticketRepository;

    @Mock
    private TicketCommentRepositoryPort ticketCommentRepository;

    @Mock
    private TicketHistoryRepositoryPort ticketHistoryRepository;

    @Mock
    private IdempotencyRecordRepositoryPort idempotencyRecordRepository;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private CategoryRepositoryPort categoryRepository;

    @Mock
    private SlaPolicyRepositoryPort slaPolicyRepository;

    @Mock
    private NotificationRepositoryPort notificationRepository;

    @Mock
    private TicketCodeGenerator ticketCodeGenerator;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private HashingService hashingService;

    private TicketService ticketService;
    private AuthenticatedUser currentUser;

    @BeforeEach
    void setUp() {
        IdempotencyPolicy idempotencyPolicy = () -> 24;
        TicketLifecyclePolicy ticketLifecyclePolicy = () -> 7;
        TransactionRunner transactionRunner = new TransactionRunner() {
            @Override
            public <T> T readOnly(java.util.function.Supplier<T> action) {
                return action.get();
            }

            @Override
            public <T> T required(java.util.function.Supplier<T> action) {
                return action.get();
            }
        };
        ObjectMapper objectMapper = new ObjectMapper();
        JsonCodec jsonCodec = new JsonCodec() {
            @Override
            public String serialize(Object value) {
                try {
                    return objectMapper.writeValueAsString(value);
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            }

            @Override
            public <T> T deserialize(String value, Class<T> type) {
                try {
                    return objectMapper.readValue(value, type);
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            }
        };

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
            Set.of(Permission.TICKET_READ_OWN)
        );
    }

    @Test
    void shouldRejectUnsupportedSortField() {
        TicketService.TicketFilterRequest filterRequest = new TicketService.TicketFilterRequest(
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

        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> ticketService.list(currentUser, filterRequest)
        );

        assertEquals("INVALID_SORT_FIELD", exception.getCode());
        verifyNoInteractions(ticketRepository);
    }

    @Test
    void shouldRejectInvalidCreatedAtRange() {
        TicketService.TicketFilterRequest filterRequest = new TicketService.TicketFilterRequest(
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

        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> ticketService.list(currentUser, filterRequest)
        );

        assertEquals("INVALID_DATE_RANGE", exception.getCode());
        verifyNoInteractions(ticketRepository);
    }

    @Test
    void shouldRejectCreateWhenAuthenticatedUserNoLongerExists() {
        AuthenticatedUser customerUser = new AuthenticatedUser(
            "missing-user",
            "customer@test.com",
            "Customer",
            "User",
            Role.CUSTOMER,
            Set.of(Permission.TICKET_CREATE, Permission.TICKET_READ_OWN)
        );

        when(userRepository.findById("missing-user")).thenReturn(Optional.empty());

        UnauthorizedException exception = assertThrows(
            UnauthorizedException.class,
            () -> ticketService.create(
                customerUser,
                new TicketService.CreateTicketRequest(
                    "Computadora sin memoria",
                    "No me permite crear archivos",
                    "category-1",
                    TicketPriority.HIGH
                ),
                "idem-1"
            )
        );

        assertEquals("AUTHENTICATED_USER_NOT_FOUND", exception.getCode());
        verifyNoInteractions(ticketRepository, categoryRepository, slaPolicyRepository, notificationRepository, idempotencyRecordRepository);
    }

    @Test
    void shouldReturnTicketDetailWhenAssignedAgentIsNull() {
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
        when(userRepository.findAllById(anyIterable())).thenReturn(List.of());
        when(categoryRepository.findAllById(anyIterable())).thenReturn(List.of());

        TicketService.TicketDetailResponse response = ticketService.getById(currentUser, "ticket-1");

        assertEquals("ticket-1", response.id());
        assertNull(response.assignedAgentId());
    }

    @Test
    void shouldRecordSystemActorWhenAutoClosingResolvedTickets() {
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
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ticketService.autoCloseResolvedTickets();

        ArgumentCaptor<TicketHistory> historyCaptor = ArgumentCaptor.forClass(TicketHistory.class);
        verify(ticketHistoryRepository).save(historyCaptor.capture());
        assertEquals("00000000-0000-0000-0000-000000000000", historyCaptor.getValue().performedBy());
    }
}
