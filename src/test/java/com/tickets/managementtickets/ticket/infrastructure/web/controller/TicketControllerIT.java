package com.tickets.managementtickets.ticket.infrastructure.web.controller;

import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.application.port.AccessTokenService;
import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.identity.domain.model.Role;
import com.tickets.managementtickets.identity.infrastructure.security.AccessTokenAuthenticationFilter;
import com.tickets.managementtickets.identity.infrastructure.security.CurrentUserService;
import com.tickets.managementtickets.identity.infrastructure.security.JsonAccessDeniedHandler;
import com.tickets.managementtickets.identity.infrastructure.security.JsonAuthenticationEntryPoint;
import com.tickets.managementtickets.shared.application.exception.ForbiddenException;
import com.tickets.managementtickets.shared.application.model.PageResponse;
import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import com.tickets.managementtickets.shared.infrastructure.web.ApiExceptionHandler;
import com.tickets.managementtickets.shared.infrastructure.web.CorrelationIdFilter;
import com.tickets.managementtickets.support.WebSecurityTestConfiguration;
import com.tickets.managementtickets.ticket.application.service.TicketService;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// This integration test suite verifies the real HTTP contract of the ticket controller, including JWT security, validation, problem details, and correlation id behavior.
@WebMvcTest(TicketController.class)
@Import({
    WebSecurityTestConfiguration.class,
    AccessTokenAuthenticationFilter.class,
    CurrentUserService.class,
    JsonAuthenticationEntryPoint.class,
    JsonAccessDeniedHandler.class,
    CorrelationIdFilter.class,
    ApiExceptionHandler.class
})
class TicketControllerIT {

    private static final String VALID_TOKEN = "valid-access-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private AccessTokenService accessTokenService;

    @Test
    void shouldReturnUnauthorizedProblemDetailAndEchoCorrelationIdWhenAuthenticationIsMissing() throws Exception {
        // Arrange: provide a client-supplied correlation id without any Authorization header.
        String correlationId = "corr-ticket-401";

        // Act and assert: the security chain should stop the request before the controller executes.
        mockMvc.perform(get("/api/v1/tickets").header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId))
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.correlationId").value(correlationId))
            .andExpect(jsonPath("$.detail").value("Authentication is required."));
    }

    @Test
    void shouldReturnTicketPageForAuthenticatedUser() throws Exception {
        // Arrange: authenticate the request through the real JWT filter and stub the service response.
        when(accessTokenService.parseAccessToken(VALID_TOKEN)).thenReturn(authenticatedUser());
        when(ticketService.list(any(), any())).thenReturn(PageResponse.of(
            List.of(summaryResponse()),
            0,
            20,
            1,
            1,
            List.of("createdAt,desc")
        ));

        // Act and assert: the controller should serialize the paged response with the mapped ticket summary.
        mockMvc.perform(get("/api/v1/tickets").header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value("ticket-1"))
            .andExpect(jsonPath("$.content[0].code").value("TCK-2026-900001"))
            .andExpect(jsonPath("$.content[0].status").value("CREATED"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturnValidationProblemDetailForInvalidCreatePayload() throws Exception {
        // Arrange: authenticate the request and send an invalid JSON body with missing required values.
        when(accessTokenService.parseAccessToken(VALID_TOKEN)).thenReturn(authenticatedUser());
        String invalidPayload = """
            {
              "title": "",
              "description": "",
              "categoryId": "",
              "priority": null
            }
            """;

        // Act and assert: bean validation should be translated into the shared problem-detail format.
        mockMvc.perform(
                post("/api/v1/tickets")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidPayload)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.fieldErrors").isArray())
            .andExpect(jsonPath("$.fieldErrors[0].field").exists())
            .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER));
    }

    @Test
    void shouldReturnForbiddenProblemDetailWhenServiceRejectsAccess() throws Exception {
        // Arrange: authenticate the request and force the application service to reject ticket access.
        when(accessTokenService.parseAccessToken(VALID_TOKEN)).thenReturn(authenticatedUser());
        when(ticketService.getById(eq(authenticatedUser()), eq("ticket-404")))
            .thenThrow(new ForbiddenException("ACCESS_DENIED", "You do not have permission to view this ticket."));

        // Act and assert: the controller advice should convert the application exception into HTTP 403.
        mockMvc.perform(get("/api/v1/tickets/ticket-404").header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
            .andExpect(jsonPath("$.detail").value("You do not have permission to view this ticket."));
    }

    private AuthenticatedUser authenticatedUser() {
        // Build the principal that the JWT filter will place into Spring Security for authenticated requests.
        return new AuthenticatedUser(
            "user-1",
            "customer.finance@tickets.local",
            "Fabian",
            "Finance",
            Role.CUSTOMER,
            Set.of(Permission.TICKET_CREATE, Permission.TICKET_READ_OWN)
        );
    }

    private com.tickets.managementtickets.ticket.application.result.TicketSummaryResponse summaryResponse() {
        // Build one application-layer ticket summary so the controller can serialize it as JSON.
        return new com.tickets.managementtickets.ticket.application.result.TicketSummaryResponse(
            "ticket-1",
            "TCK-2026-900001",
            "VPN access for new analyst",
            TicketStatus.CREATED,
            TicketPriority.HIGH,
            "user-1",
            "Fabian Finance",
            null,
            null,
            "category-1",
            "Access",
            Instant.parse("2026-07-24T12:00:00Z"),
            false,
            false,
            Instant.parse("2026-07-24T08:00:00Z"),
            Instant.parse("2026-07-24T08:00:00Z"),
            0
        );
    }
}
