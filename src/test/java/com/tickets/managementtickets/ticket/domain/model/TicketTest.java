package com.tickets.managementtickets.ticket.domain.model;

import com.tickets.managementtickets.shared.domain.exception.DomainRuleViolationException;
import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import com.tickets.managementtickets.sla.domain.model.SlaPolicy;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// This test suite verifies the Ticket aggregate business rules without Spring, persistence, or application services.
class TicketTest {

    // Freeze "now" so every SLA calculation is deterministic and easy to reason about in assertions.
    private static final Instant NOW = Instant.parse("2026-07-16T00:00:00Z");

    @Test
    void shouldCreateTicketWithInitialStatusAndSlaDeadlines() {
        // Arrange: define the SLA policy used by the aggregate factory.
        SlaPolicy policy = slaPolicy(TicketPriority.HIGH, 2, 8);

        // Act: create a new ticket from the domain factory.
        Ticket ticket = Ticket.create("TCK-2026-000001", "Title", "Description", TicketPriority.HIGH, "requester-1", "category-1", policy, NOW);

        // Assert: verify the aggregate starts in CREATED because no agent has been assigned yet.
        assertEquals(TicketStatus.CREATED, ticket.getStatus());
        // The first-response deadline should be "now + 2 hours", converted internally to seconds.
        assertEquals(NOW.plusSeconds(2 * 60L * 60L), ticket.getFirstResponseDueAt());
        // The resolution deadline should be "now + 8 hours", also converted to seconds.
        assertEquals(NOW.plusSeconds(8 * 60L * 60L), ticket.getResolutionDueAt());
        // A newly created ticket is not terminal because it can still move through the workflow.
        assertFalse(ticket.isTerminal());
    }

    @Test
    void shouldAssignCreatedTicketAndMoveItToAssigned() {
        // Arrange: start from a newly created ticket.
        Ticket ticket = createdTicket();

        // Act: assign the ticket to a support user.
        ticket.assign("agent-1");

        // Assert: verify the assignee and status transition were applied.
        assertEquals("agent-1", ticket.getAssignedAgentId());
        assertEquals(TicketStatus.ASSIGNED, ticket.getStatus());
    }

    @Test
    void shouldStartAssignedTicketAndRecordFirstResponse() {
        // Arrange: prepare an assigned ticket.
        Ticket ticket = createdTicket();
        ticket.assign("agent-1");

        // Act: start the ticket after the first response deadline.
        ticket.start(NOW.plusSeconds(3 * 60L * 60L));

        // Assert: verify the ticket is in progress and the first-response breach is detected.
        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
        assertEquals(NOW.plusSeconds(3 * 60L * 60L), ticket.getFirstRespondedAt());
        assertTrue(ticket.isSlaFirstResponseBreached());
    }

    @Test
    void shouldPauseAndResumeResolutionSlaWhenRequestingInformationAndCustomerResponds() {
        // Arrange: put the ticket in progress.
        Ticket ticket = createdTicket();
        ticket.assign("agent-1");
        ticket.start(NOW.plusSeconds(60));

        // Act: request information and later continue after the customer response.
        ticket.requestInformation(NOW.plusSeconds(2 * 60L * 60L));
        ticket.continueAfterCustomerResponse(NOW.plusSeconds(3 * 60L * 60L));

        // Assert: after the customer answers, the ticket goes back to IN_PROGRESS.
        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
        // The ticket spent one hour waiting for the customer, so that pause is accumulated.
        assertEquals(60L * 60L, ticket.getAccumulatedPausedSeconds());
        // The pause marker must be cleared once work resumes.
        assertNull(ticket.getSlaPausedAt());
        // The original 8-hour deadline is extended by the paused hour, resulting in 9 total hours.
        assertEquals(NOW.plusSeconds(9 * 60L * 60L), ticket.getResolutionDueAt());
    }

    @Test
    void shouldResolveTicketAndDetectResolutionBreach() {
        // Arrange: put the ticket in progress.
        Ticket ticket = createdTicket();
        ticket.assign("agent-1");
        ticket.start(NOW.plusSeconds(60));

        // Act: resolve the ticket after the resolution deadline.
        ticket.resolve("Fixed", NOW.plusSeconds(9 * 60L * 60L));

        // Assert: verify resolved state, summary, timestamp, and SLA breach flag.
        assertEquals(TicketStatus.RESOLVED, ticket.getStatus());
        assertEquals("Fixed", ticket.getResolutionSummary());
        assertEquals(NOW.plusSeconds(9 * 60L * 60L), ticket.getResolvedAt());
        assertTrue(ticket.isSlaResolutionBreached());
    }

    @Test
    void shouldCloseOnlyResolvedTickets() {
        // Arrange: prepare a resolved ticket and an invalid in-progress ticket.
        Ticket resolvedTicket = createdTicket();
        resolvedTicket.assign("agent-1");
        resolvedTicket.start(NOW.plusSeconds(60));
        resolvedTicket.resolve("Fixed", NOW.plusSeconds(2 * 60L * 60L));
        Ticket inProgressTicket = createdTicket();
        inProgressTicket.assign("agent-1");
        inProgressTicket.start(NOW.plusSeconds(60));

        // Act: close the valid ticket and try to close the invalid one.
        resolvedTicket.close(NOW.plusSeconds(3 * 60L * 60L));
        DomainRuleViolationException exception = assertThrows(
            DomainRuleViolationException.class,
            () -> inProgressTicket.close(NOW.plusSeconds(3 * 60L * 60L))
        );

        // Assert: verify successful closure and rejection of the invalid transition.
        assertEquals(TicketStatus.CLOSED, resolvedTicket.getStatus());
        assertTrue(resolvedTicket.isTerminal());
        assertEquals("Only resolved tickets can be closed.", exception.getMessage());
    }

    @Test
    void shouldReopenResolvedTicketAndResetResolutionFields() {
        // Arrange: prepare a resolved ticket.
        Ticket ticket = createdTicket();
        ticket.assign("agent-1");
        ticket.start(NOW.plusSeconds(60));
        ticket.resolve("Fixed", NOW.plusSeconds(2 * 60L * 60L));

        // Act: reopen the ticket with a new SLA policy.
        ticket.reopen(NOW.plusSeconds(3 * 60L * 60L), slaPolicy(TicketPriority.HIGH, 2, 6));

        // Assert: verify resolution fields were cleared and the new deadline was calculated.
        assertEquals(TicketStatus.IN_PROGRESS, ticket.getStatus());
        assertNull(ticket.getResolvedAt());
        assertNull(ticket.getClosedAt());
        assertNull(ticket.getResolutionSummary());
        assertEquals(NOW.plusSeconds(9 * 60L * 60L), ticket.getResolutionDueAt());
        assertFalse(ticket.isSlaResolutionBreached());
    }

    @Test
    void shouldRejectChangesOnTerminalTickets() {
        // Arrange: cancel a ticket so it becomes terminal.
        Ticket ticket = createdTicket();
        ticket.cancel(NOW.plusSeconds(60));

        // Act: try to update a terminal ticket.
        DomainRuleViolationException exception = assertThrows(
            DomainRuleViolationException.class,
            () -> ticket.updateDetails("New title", "New description", "category-2")
        );

        // Assert: verify the aggregate protects terminal tickets from changes.
        assertEquals(TicketStatus.CANCELLED, ticket.getStatus());
        assertTrue(ticket.isTerminal());
        assertEquals("Terminal tickets cannot be modified.", exception.getMessage());
    }

    private Ticket createdTicket() {
        // Centralize ticket creation so all tests start from the same valid aggregate state.
        return Ticket.create(
            "TCK-2026-000001",
            "Title",
            "Description",
            TicketPriority.HIGH,
            "requester-1",
            "category-1",
            slaPolicy(TicketPriority.HIGH, 2, 8),
            NOW
        );
    }

    private SlaPolicy slaPolicy(TicketPriority priority, int firstResponseHours, int resolutionHours) {
        // Build a simple active SLA policy with caller-controlled durations.
        return new SlaPolicy("sla-1", priority, firstResponseHours, resolutionHours, true, 0);
    }
}
