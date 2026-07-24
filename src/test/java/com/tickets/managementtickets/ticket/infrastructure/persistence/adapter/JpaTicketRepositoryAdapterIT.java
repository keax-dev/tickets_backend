package com.tickets.managementtickets.ticket.infrastructure.persistence.adapter;

import com.tickets.managementtickets.category.infrastructure.persistence.entity.CategoryEntity;
import com.tickets.managementtickets.category.infrastructure.persistence.repository.CategoryRepository;
import com.tickets.managementtickets.identity.domain.model.Role;
import com.tickets.managementtickets.identity.infrastructure.persistence.entity.UserEntity;
import com.tickets.managementtickets.identity.infrastructure.persistence.repository.UserRepository;
import com.tickets.managementtickets.shared.application.model.PageResponse;
import com.tickets.managementtickets.shared.application.model.SortDirection;
import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import com.tickets.managementtickets.support.AbstractMySqlContainerIntegrationTest;
import com.tickets.managementtickets.ticket.application.port.TicketCountQuery;
import com.tickets.managementtickets.ticket.application.port.TicketQuery;
import com.tickets.managementtickets.ticket.application.port.TicketVisibility;
import com.tickets.managementtickets.ticket.domain.model.Ticket;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;
import com.tickets.managementtickets.ticket.infrastructure.persistence.entity.TicketEntity;
import com.tickets.managementtickets.ticket.infrastructure.persistence.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// This integration test suite verifies the real JPA ticket adapter against MySQL, including visibility rules, filtered counts, and resolved-ticket queries.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaTicketRepositoryAdapter.class)
class JpaTicketRepositoryAdapterIT extends AbstractMySqlContainerIntegrationTest {

    @Autowired
    private JpaTicketRepositoryAdapter adapter;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void shouldReturnOnlyRequesterVisibleTickets() {
        // Arrange: create one requester, one category, and three tickets where only two belong to the same requester.
        String requesterId = "90000000-0000-0000-0000-000000000001";
        String otherRequesterId = "90000000-0000-0000-0000-000000000002";
        String categoryId = "91000000-0000-0000-0000-000000000001";
        saveUser(requesterId, "requester.one@test.local", Role.CUSTOMER);
        saveUser(otherRequesterId, "requester.two@test.local", Role.CUSTOMER);
        saveCategory(categoryId, "Integration Category");
        saveTicket(ticket("92000000-0000-0000-0000-000000000001", "TCK-2026-990001", "VPN issue", requesterId, null, categoryId, TicketStatus.CREATED, false, false, null));
        saveTicket(ticket("92000000-0000-0000-0000-000000000002", "TCK-2026-990002", "Laptop issue", requesterId, null, categoryId, TicketStatus.ASSIGNED, false, false, null));
        saveTicket(ticket("92000000-0000-0000-0000-000000000003", "TCK-2026-990003", "Printer issue", otherRequesterId, null, categoryId, TicketStatus.CREATED, false, false, null));

        // Act: query tickets using requester-only visibility and a page sorted by code.
        PageResponse<Ticket> result = adapter.findAll(new TicketQuery(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            0,
            10,
            "code",
            SortDirection.ASC,
            TicketVisibility.REQUESTER,
            requesterId
        ));

        // Assert: only the requester's own tickets should be returned.
        assertEquals(2, result.content().size());
        assertEquals(List.of("TCK-2026-990001", "TCK-2026-990002"), result.content().stream().map(Ticket::getCode).toList());
    }

    @Test
    void shouldReturnAssignedOrUnassignedTicketsForSupportQueueVisibility() {
        // Arrange: create one support agent, one requester, one category, and three tickets with different assignment states.
        String requesterId = "90000000-0000-0000-0000-000000000011";
        String currentAgentId = "90000000-0000-0000-0000-000000000012";
        String otherAgentId = "90000000-0000-0000-0000-000000000013";
        String categoryId = "91000000-0000-0000-0000-000000000011";
        saveUser(requesterId, "support.requester@test.local", Role.CUSTOMER);
        saveUser(currentAgentId, "agent.current@test.local", Role.SUPPORT_AGENT);
        saveUser(otherAgentId, "agent.other@test.local", Role.SUPPORT_AGENT);
        saveCategory(categoryId, "Support Queue Category");
        saveTicket(ticket("92000000-0000-0000-0000-000000000011", "TCK-2026-991001", "Assigned to me", requesterId, currentAgentId, categoryId, TicketStatus.ASSIGNED, false, false, null));
        saveTicket(ticket("92000000-0000-0000-0000-000000000012", "TCK-2026-991002", "Unassigned", requesterId, null, categoryId, TicketStatus.CREATED, false, false, null));
        saveTicket(ticket("92000000-0000-0000-0000-000000000013", "TCK-2026-991003", "Assigned elsewhere", requesterId, otherAgentId, categoryId, TicketStatus.ASSIGNED, false, false, null));

        // Act: query the support queue visibility for the current agent.
        PageResponse<Ticket> result = adapter.findAll(new TicketQuery(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            0,
            10,
            "code",
            SortDirection.ASC,
            TicketVisibility.ASSIGNED_OR_UNASSIGNED,
            currentAgentId
        ));

        // Assert: the support queue should include the agent's tickets plus currently unassigned ones.
        assertEquals(List.of("TCK-2026-991001", "TCK-2026-991002"), result.content().stream().map(Ticket::getCode).toList());
    }

    @Test
    void shouldCountBreachedAndUnassignedTicketsAndReturnOldResolvedOnes() {
        // Arrange: create isolated fixtures under one requester so count queries ignore the demo dataset.
        String requesterId = "90000000-0000-0000-0000-000000000021";
        String agentId = "90000000-0000-0000-0000-000000000022";
        String categoryId = "91000000-0000-0000-0000-000000000021";
        saveUser(requesterId, "count.requester@test.local", Role.CUSTOMER);
        saveUser(agentId, "count.agent@test.local", Role.SUPPORT_AGENT);
        saveCategory(categoryId, "Count Category");
        saveTicket(ticket("92000000-0000-0000-0000-000000000021", "TCK-2026-992001", "Breached and unassigned", requesterId, null, categoryId, TicketStatus.IN_PROGRESS, true, true, null));
        saveTicket(ticket("92000000-0000-0000-0000-000000000022", "TCK-2026-992002", "Assigned and healthy", requesterId, agentId, categoryId, TicketStatus.ASSIGNED, false, false, null));
        saveTicket(ticket("92000000-0000-0000-0000-000000000023", "TCK-2026-992003", "Very old resolved", requesterId, agentId, categoryId, TicketStatus.RESOLVED, false, false, Instant.parse("2020-01-01T00:00:00Z")));

        // Act: execute isolated count queries plus the old-resolved lookup used by the auto-close flow.
        long breachedCount = adapter.count(TicketCountQuery.visibleTo(TicketVisibility.REQUESTER, requesterId).breachedSlaOnly());
        long unassignedCount = adapter.count(TicketCountQuery.visibleTo(TicketVisibility.REQUESTER, requesterId).unassignedOnly());
        List<Ticket> oldResolvedTickets = adapter.findAllByStatusAndResolvedAtBefore(TicketStatus.RESOLVED, Instant.parse("2020-01-02T00:00:00Z"));

        // Assert: the adapter should honor the filters and return the expected resolved ticket candidate.
        assertEquals(1, breachedCount);
        assertEquals(1, unassignedCount);
        assertEquals(List.of("TCK-2026-992003"), oldResolvedTickets.stream().map(Ticket::getCode).toList());
    }

    private void saveUser(String id, String email, Role role) {
        // Persist the minimal valid user row needed by foreign keys in the ticket table.
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPasswordHash("encoded-password");
        user.setRole(role);
        user.setActive(true);
        userRepository.saveAndFlush(user);
    }

    private void saveCategory(String id, String name) {
        // Persist one active category so ticket foreign keys remain valid.
        CategoryEntity category = new CategoryEntity();
        category.setId(id);
        category.setName(name);
        category.setDescription("Integration test category");
        category.setActive(true);
        categoryRepository.saveAndFlush(category);
    }

    private void saveTicket(TicketEntity ticket) {
        // Persist and flush each ticket so subsequent queries see the row immediately.
        ticketRepository.saveAndFlush(ticket);
    }

    private TicketEntity ticket(
        String id,
        String code,
        String title,
        String requesterId,
        String assignedAgentId,
        String categoryId,
        TicketStatus status,
        boolean firstResponseBreached,
        boolean resolutionBreached,
        Instant resolvedAt
    ) {
        // Build a compact ticket entity fixture while letting the base entity manage created/updated timestamps automatically.
        TicketEntity ticket = new TicketEntity();
        ticket.setId(id);
        ticket.setCode(code);
        ticket.setTitle(title);
        ticket.setDescription(title + " description");
        ticket.setStatus(status);
        ticket.setPriority(TicketPriority.HIGH);
        ticket.setRequesterId(requesterId);
        ticket.setAssignedAgentId(assignedAgentId);
        ticket.setCategoryId(categoryId);
        ticket.setFirstResponseDueAt(Instant.parse("2026-07-24T08:00:00Z"));
        ticket.setResolutionDueAt(Instant.parse("2026-07-25T08:00:00Z"));
        ticket.setFirstRespondedAt(status == TicketStatus.CREATED ? null : Instant.parse("2026-07-24T08:30:00Z"));
        ticket.setResolvedAt(resolvedAt);
        ticket.setClosedAt(null);
        ticket.setCancelledAt(null);
        ticket.setSlaPausedAt(null);
        ticket.setAccumulatedPausedSeconds(0);
        ticket.setSlaFirstResponseBreached(firstResponseBreached);
        ticket.setSlaResolutionBreached(resolutionBreached);
        ticket.setResolutionSummary(resolvedAt == null ? null : "Resolved");
        return ticket;
    }
}
