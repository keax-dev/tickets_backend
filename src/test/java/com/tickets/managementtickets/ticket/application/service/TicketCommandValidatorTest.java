package com.tickets.managementtickets.ticket.application.service;

import com.tickets.managementtickets.identity.domain.model.Role;
import com.tickets.managementtickets.identity.domain.model.User;
import com.tickets.managementtickets.shared.application.exception.ConflictException;
import com.tickets.managementtickets.shared.application.exception.ValidationException;
import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import com.tickets.managementtickets.ticket.domain.model.Ticket;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// This test suite verifies ticket command validations before domain transitions are executed by the application service.
class TicketCommandValidatorTest {

    // Use a real validator because this class contains pure application rules and has no external dependencies.
    private final TicketCommandValidator validator = new TicketCommandValidator();

    @Test
    void shouldRejectStaleTicketVersion() {
        // Arrange: create a ticket whose current version is newer than the request.
        Ticket ticket = ticket(TicketStatus.CREATED, 3);

        // Act: validate a stale requested version.
        ConflictException exception = assertThrows(
            ConflictException.class,
            () -> validator.ensureVersion(ticket, 2)
        );

        // Assert: verify optimistic concurrency failures use the shared conflict code.
        assertEquals("RESOURCE_VERSION_CONFLICT", exception.getCode());
    }

    @Test
    void shouldRejectTerminalTicketChanges() {
        // Arrange: create a terminal ticket.
        Ticket ticket = ticket(TicketStatus.CLOSED);

        // Act: validate whether the ticket can still be modified.
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> validator.ensureNotTerminal(ticket)
        );

        // Assert: verify terminal tickets are rejected with the expected validation code.
        assertEquals("TICKET_TERMINAL", exception.getCode());
    }

    @Test
    void shouldAllowSupportUsersAsAssignees() {
        // Arrange: create active support users that can handle tickets.
        User agent = user(Role.SUPPORT_AGENT, true);
        User manager = user(Role.SUPPORT_MANAGER, true);

        // Act and assert: verify both support roles are accepted.
        assertDoesNotThrow(() -> validator.ensureAssigneeCanHandleTickets(agent));
        assertDoesNotThrow(() -> validator.ensureAssigneeCanHandleTickets(manager));
    }

    @Test
    void shouldRejectInactiveAssignee() {
        // Arrange: create an inactive support user.
        User inactiveAgent = user(Role.SUPPORT_AGENT, false);

        // Act: validate the inactive assignee.
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> validator.ensureAssigneeCanHandleTickets(inactiveAgent)
        );

        // Assert: verify inactive assignees are rejected with a specific code.
        assertEquals("ASSIGNEE_INACTIVE", exception.getCode());
    }

    @Test
    void shouldRejectInvalidAssigneeRole() {
        // Arrange: create an active customer, which is not a support assignee role.
        User customer = user(Role.CUSTOMER, true);

        // Act: validate the invalid assignee role.
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> validator.ensureAssigneeCanHandleTickets(customer)
        );

        // Assert: verify non-support roles are rejected with a specific code.
        assertEquals("INVALID_ASSIGNEE_ROLE", exception.getCode());
    }

    @Test
    void shouldRequireAssignedTicketBeforeStart() {
        // Arrange: create one assigned ticket and one ticket in an invalid status.
        Ticket assignedTicket = ticket(TicketStatus.ASSIGNED);
        Ticket createdTicket = ticket(TicketStatus.CREATED);

        // Act: validate both start scenarios.
        assertDoesNotThrow(() -> validator.ensureStartAllowed(assignedTicket));
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> validator.ensureStartAllowed(createdTicket)
        );

        // Assert: verify only assigned tickets can start work.
        assertEquals("INVALID_TICKET_TRANSITION", exception.getCode());
    }

    @Test
    void shouldRequireInProgressTicketBeforeInformationRequest() {
        // Arrange: create one in-progress ticket and one ticket in an invalid status.
        Ticket inProgressTicket = ticket(TicketStatus.IN_PROGRESS);
        Ticket assignedTicket = ticket(TicketStatus.ASSIGNED);

        // Act: validate both request-information scenarios.
        assertDoesNotThrow(() -> validator.ensureInformationRequestAllowed(inProgressTicket));
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> validator.ensureInformationRequestAllowed(assignedTicket)
        );

        // Assert: verify information requests require an in-progress ticket.
        assertEquals("INVALID_TICKET_TRANSITION", exception.getCode());
    }

    @Test
    void shouldRequireResolvableStatusAndResolutionSummary() {
        // Arrange: create valid and invalid resolution scenarios.
        Ticket inProgressTicket = ticket(TicketStatus.IN_PROGRESS);
        Ticket waitingTicket = ticket(TicketStatus.WAITING_FOR_CUSTOMER);
        Ticket createdTicket = ticket(TicketStatus.CREATED);

        // Act: validate allowed resolution states and invalid resolution inputs.
        assertDoesNotThrow(() -> validator.ensureResolveAllowed(inProgressTicket, "Fixed"));
        assertDoesNotThrow(() -> validator.ensureResolveAllowed(waitingTicket, "Fixed"));
        ValidationException invalidStatus = assertThrows(
            ValidationException.class,
            () -> validator.ensureResolveAllowed(createdTicket, "Fixed")
        );
        ValidationException missingSummary = assertThrows(
            ValidationException.class,
            () -> validator.ensureResolveAllowed(inProgressTicket, " ")
        );

        // Assert: verify invalid status and missing summary keep distinct error codes.
        assertEquals("INVALID_TICKET_TRANSITION", invalidStatus.getCode());
        assertEquals("RESOLUTION_SUMMARY_REQUIRED", missingSummary.getCode());
    }

    @Test
    void shouldRequireResolvedTicketBeforeCloseOrReopen() {
        // Arrange: create resolved and in-progress tickets for close/reopen checks.
        Ticket resolvedTicket = ticket(TicketStatus.RESOLVED);
        Ticket inProgressTicket = ticket(TicketStatus.IN_PROGRESS);

        // Act: validate allowed and rejected close/reopen scenarios.
        assertDoesNotThrow(() -> validator.ensureCloseAllowed(resolvedTicket));
        assertDoesNotThrow(() -> validator.ensureReopenRequestAllowed(resolvedTicket, "Need more help"));
        ValidationException closeException = assertThrows(
            ValidationException.class,
            () -> validator.ensureCloseAllowed(inProgressTicket)
        );
        ValidationException reopenException = assertThrows(
            ValidationException.class,
            () -> validator.ensureReopenRequestAllowed(inProgressTicket, "Need more help")
        );
        ValidationException reasonException = assertThrows(
            ValidationException.class,
            () -> validator.ensureReopenRequestAllowed(resolvedTicket, "")
        );

        // Assert: verify invalid transitions and missing reason keep their public codes.
        assertEquals("INVALID_TICKET_TRANSITION", closeException.getCode());
        assertEquals("INVALID_TICKET_TRANSITION", reopenException.getCode());
        assertEquals("REOPEN_REASON_REQUIRED", reasonException.getCode());
    }

    @Test
    void shouldRequireCancelReasonAndCommentContent() {
        // Arrange: define invalid text inputs and one value that should be normalized.
        String missingCancelReason = null;
        String blankComment = " ";
        String valueToNormalize = " value ";

        // Act: validate cancel reason, comment content, and normalization.
        ValidationException cancelException = assertThrows(
            ValidationException.class,
            () -> validator.ensureCancelReason(missingCancelReason)
        );
        ValidationException commentException = assertThrows(
            ValidationException.class,
            () -> validator.ensureCommentContent(blankComment)
        );

        // Assert: verify required text checks and null-safe trimming behavior.
        assertEquals("CANCEL_REASON_REQUIRED", cancelException.getCode());
        assertEquals("COMMENT_CONTENT_REQUIRED", commentException.getCode());
        assertEquals("value", validator.normalize(valueToNormalize));
        assertEquals("", validator.normalize(null));
    }

    private Ticket ticket(TicketStatus status) {
        // Delegate to the full helper and default the optimistic-lock version to zero.
        return ticket(status, 0);
    }

    private Ticket ticket(TicketStatus status, long version) {
        // Build a minimal but valid aggregate snapshot tailored for validator scenarios.
        // The helper lets each test focus only on the status/version combination it cares about.
        return new Ticket(
            "ticket-1",
            "TCK-2026-000001",
            "Title",
            "Description",
            status,
            TicketPriority.MEDIUM,
            "requester-1",
            "agent-1",
            "category-1",
            Instant.parse("2026-07-16T04:00:00Z"),
            Instant.parse("2026-07-17T00:00:00Z"),
            null,
            // Resolved and closed tickets must expose a resolution timestamp because that is part of a coherent state.
            status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED ? Instant.parse("2026-07-16T12:00:00Z") : null,
            // Closed tickets additionally need a close timestamp.
            status == TicketStatus.CLOSED ? Instant.parse("2026-07-16T13:00:00Z") : null,
            // Cancelled tickets need a cancel timestamp.
            status == TicketStatus.CANCELLED ? Instant.parse("2026-07-16T13:00:00Z") : null,
            null,
            0,
            false,
            false,
            // Resolved and closed tickets carry a resolution summary so state-dependent validations remain realistic.
            status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED ? "Fixed" : null,
            Instant.parse("2026-07-16T00:00:00Z"),
            Instant.parse("2026-07-16T00:00:00Z"),
            version
        );
    }

    private User user(Role role, boolean active) {
        // Create a lightweight domain user with only the fields relevant to assignee validation.
        return new User(
            "user-1",
            "User",
            "Test",
            "user@test.com",
            "encoded",
            role,
            active,
            0,
            null,
            null,
            0
        );
    }
}
