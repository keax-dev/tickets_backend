package com.tickets.managementtickets.ticket.infrastructure.web.controller;

import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.application.port.CurrentAuthenticatedUserProvider;
import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.identity.domain.model.Role;
import com.tickets.managementtickets.shared.application.model.PageResponse;
import com.tickets.managementtickets.shared.application.model.SortDirection;
import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import com.tickets.managementtickets.ticket.application.query.TicketFilterRequest;
import com.tickets.managementtickets.ticket.application.service.TicketService;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;
import com.tickets.managementtickets.ticket.infrastructure.web.dto.CreateTicketRequest;
import com.tickets.managementtickets.ticket.infrastructure.web.dto.TicketDetailResponse;
import com.tickets.managementtickets.ticket.infrastructure.web.dto.TicketSummaryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// This test suite verifies the ticket web adapter maps HTTP-facing inputs into application commands and responses.
@ExtendWith(MockitoExtension.class)
class TicketControllerTest {

    // Mock the application service because the controller should delegate business work instead of implementing it.
    @Mock
    private TicketService ticketService;

    // Mock the current-user provider because the controller obtains the authenticated principal from this abstraction.
    @Mock
    private CurrentAuthenticatedUserProvider currentUserProvider;

    @Test
    void shouldMapListParametersToApplicationFilter() {
        // Arrange: create a controller and stub the current user plus application page response.
        TicketController controller = new TicketController(ticketService, currentUserProvider);
        AuthenticatedUser user = currentUser();
        Instant createdFrom = Instant.parse("2026-07-16T00:00:00Z");
        Instant createdTo = Instant.parse("2026-07-17T00:00:00Z");
        // The controller first asks for the authenticated principal from the security boundary.
        when(currentUserProvider.requireCurrentUser()).thenReturn(user);
        // Then it delegates the real work to the application service and receives application-layer data back.
        when(ticketService.list(org.mockito.ArgumentMatchers.eq(user), org.mockito.ArgumentMatchers.any(TicketFilterRequest.class)))
            .thenReturn(PageResponse.of(List.of(summaryResponse()), 2, 25, 1, 1, List.of("code,ASC")));

        // Act: call the controller as Spring MVC would after binding query parameters.
        PageResponse<TicketSummaryResponse> response = controller.list(
            "memory",
            TicketStatus.CREATED,
            TicketPriority.HIGH,
            "category-1",
            "agent-1",
            createdFrom,
            createdTo,
            2,
            25,
            "code",
            Sort.Direction.ASC
        );

        // Assert: verify the controller returned web DTOs and passed the correct application filter.
        ArgumentCaptor<TicketFilterRequest> filterCaptor = ArgumentCaptor.forClass(TicketFilterRequest.class);
        verify(ticketService).list(org.mockito.ArgumentMatchers.eq(user), filterCaptor.capture());
        TicketFilterRequest filter = filterCaptor.getValue();
        assertEquals("memory", filter.search());
        assertEquals(TicketStatus.CREATED, filter.status());
        assertEquals(TicketPriority.HIGH, filter.priority());
        assertEquals("category-1", filter.categoryId());
        assertEquals("agent-1", filter.assignedAgentId());
        assertEquals(createdFrom, filter.createdFrom());
        assertEquals(createdTo, filter.createdTo());
        assertEquals(2, filter.page());
        assertEquals(25, filter.size());
        assertEquals("code", filter.sortBy());
        assertEquals(SortDirection.ASC, filter.direction());
        assertEquals("ticket-1", response.content().getFirst().id());
    }

    @Test
    void shouldMapCreateBodyAndIdempotencyHeaderToApplicationCommand() {
        // Arrange: create a controller and stub the service detail response.
        TicketController controller = new TicketController(ticketService, currentUserProvider);
        AuthenticatedUser user = currentUser();
        CreateTicketRequest request = new CreateTicketRequest("Title", "Description", "category-1", TicketPriority.URGENT);
        // The controller must obtain the principal before calling the application service.
        when(currentUserProvider.requireCurrentUser()).thenReturn(user);
        // The controller should pass an application command plus the idempotency key header to the service.
        when(ticketService.create(
            org.mockito.ArgumentMatchers.eq(user),
            org.mockito.ArgumentMatchers.any(com.tickets.managementtickets.ticket.application.command.CreateTicketRequest.class),
            org.mockito.ArgumentMatchers.eq("idem-1")
        )).thenReturn(detailResponse());

        // Act: call the controller create endpoint method with a request DTO and idempotency key.
        TicketDetailResponse response = controller.create(request, "idem-1");

        // Assert: verify the web DTO was translated to the application command without losing values.
        ArgumentCaptor<com.tickets.managementtickets.ticket.application.command.CreateTicketRequest> commandCaptor =
            ArgumentCaptor.forClass(com.tickets.managementtickets.ticket.application.command.CreateTicketRequest.class);
        verify(ticketService).create(org.mockito.ArgumentMatchers.eq(user), commandCaptor.capture(), org.mockito.ArgumentMatchers.eq("idem-1"));
        com.tickets.managementtickets.ticket.application.command.CreateTicketRequest command = commandCaptor.getValue();
        assertEquals("Title", command.title());
        assertEquals("Description", command.description());
        assertEquals("category-1", command.categoryId());
        assertEquals(TicketPriority.URGENT, command.priority());
        assertEquals("ticket-1", response.id());
    }

    private AuthenticatedUser currentUser() {
        // Create the authenticated principal that the controller will receive from the security boundary.
        return new AuthenticatedUser(
            "user-1",
            "user@test.com",
            "User",
            "Test",
            Role.CUSTOMER,
            Set.of(Permission.TICKET_READ_OWN, Permission.TICKET_CREATE)
        );
    }

    private com.tickets.managementtickets.ticket.application.result.TicketSummaryResponse summaryResponse() {
        // Build an application-layer summary result that the controller must map back to a web DTO.
        return new com.tickets.managementtickets.ticket.application.result.TicketSummaryResponse(
            "ticket-1",
            "TCK-2026-000001",
            "Title",
            TicketStatus.CREATED,
            TicketPriority.HIGH,
            "user-1",
            "User Test",
            null,
            null,
            "category-1",
            "Hardware",
            Instant.parse("2026-07-17T00:00:00Z"),
            false,
            false,
            Instant.parse("2026-07-16T00:00:00Z"),
            Instant.parse("2026-07-16T00:00:00Z"),
            0
        );
    }

    private com.tickets.managementtickets.ticket.application.result.TicketDetailResponse detailResponse() {
        // Build an application-layer detail result that the controller must expose as an HTTP response DTO.
        return new com.tickets.managementtickets.ticket.application.result.TicketDetailResponse(
            "ticket-1",
            "TCK-2026-000001",
            "Title",
            "Description",
            TicketStatus.CREATED,
            TicketPriority.URGENT,
            "user-1",
            "User Test",
            null,
            null,
            "category-1",
            "Hardware",
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
            Instant.parse("2026-07-16T00:00:00Z"),
            Instant.parse("2026-07-16T00:00:00Z"),
            0,
            List.of("update")
        );
    }
}
