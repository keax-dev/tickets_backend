package com.tickets.managementtickets.ticket.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickets.managementtickets.category.infrastructure.persistence.repository.CategoryRepository;
import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.application.service.AuthorizationService;
import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.identity.domain.model.Role;
import com.tickets.managementtickets.identity.infrastructure.persistence.repository.UserRepository;
import com.tickets.managementtickets.notification.infrastructure.persistence.repository.NotificationRepository;
import com.tickets.managementtickets.shared.application.exception.BadRequestException;
import com.tickets.managementtickets.shared.application.port.HashingService;
import com.tickets.managementtickets.sla.infrastructure.persistence.repository.SlaPolicyRepository;
import com.tickets.managementtickets.ticket.infrastructure.persistence.repository.IdempotencyRecordRepository;
import com.tickets.managementtickets.ticket.infrastructure.persistence.repository.TicketCommentRepository;
import com.tickets.managementtickets.ticket.infrastructure.persistence.repository.TicketHistoryRepository;
import com.tickets.managementtickets.ticket.infrastructure.persistence.repository.TicketRepository;
import com.tickets.managementtickets.ticket.infrastructure.persistence.repository.TicketSequenceRepository;
import com.tickets.managementtickets.ticket.infrastructure.support.IdempotencyProperties;
import com.tickets.managementtickets.ticket.infrastructure.support.TicketLifecycleProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketCommentRepository ticketCommentRepository;

    @Mock
    private TicketHistoryRepository ticketHistoryRepository;

    @Mock
    private TicketSequenceRepository ticketSequenceRepository;

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SlaPolicyRepository slaPolicyRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private HashingService hashingService;

    private TicketService ticketService;
    private AuthenticatedUser currentUser;

    @BeforeEach
    void setUp() {
        IdempotencyProperties idempotencyProperties = new IdempotencyProperties();
        idempotencyProperties.setRecordTtlHours(24);
        TicketLifecycleProperties ticketLifecycleProperties = new TicketLifecycleProperties();
        ticketLifecycleProperties.setAutoCloseDays(7);

        ticketService = new TicketService(
            ticketRepository,
            ticketCommentRepository,
            ticketHistoryRepository,
            ticketSequenceRepository,
            idempotencyRecordRepository,
            userRepository,
            categoryRepository,
            slaPolicyRepository,
            notificationRepository,
            authorizationService,
            hashingService,
            new ObjectMapper(),
            Clock.fixed(Instant.parse("2026-07-16T00:00:00Z"), ZoneOffset.UTC),
            idempotencyProperties,
            ticketLifecycleProperties
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
            Sort.Direction.DESC
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
            Sort.Direction.DESC
        );

        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> ticketService.list(currentUser, filterRequest)
        );

        assertEquals("INVALID_DATE_RANGE", exception.getCode());
        verifyNoInteractions(ticketRepository);
    }
}
