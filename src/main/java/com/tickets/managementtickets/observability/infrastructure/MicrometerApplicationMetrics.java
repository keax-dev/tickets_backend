package com.tickets.managementtickets.observability.infrastructure;

import com.tickets.managementtickets.identity.application.port.AuthMetricsPort;
import com.tickets.managementtickets.identity.domain.model.Role;
import com.tickets.managementtickets.ticket.application.port.TicketMetricsPort;
import com.tickets.managementtickets.ticket.domain.model.CommentVisibility;
import com.tickets.managementtickets.ticket.domain.model.Ticket;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MicrometerApplicationMetrics implements TicketMetricsPort, AuthMetricsPort {

    private final MeterRegistry meterRegistry;

    public MicrometerApplicationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordTicketCreated(Ticket ticket) {
        counter("management_tickets_ticket_created_total", "priority", ticket.getPriority().name()).increment();
    }

    @Override
    public void recordTicketResolved(Ticket ticket) {
        counter(
            "management_tickets_ticket_resolved_total",
            "priority", ticket.getPriority().name(),
            "slaBreached", Boolean.toString(ticket.isSlaFirstResponseBreached() || ticket.isSlaResolutionBreached())
        ).increment();
    }

    @Override
    public void recordTicketClosed(Ticket ticket, boolean automatic) {
        counter(
            "management_tickets_ticket_closed_total",
            "priority", ticket.getPriority().name(),
            "automatic", Boolean.toString(automatic)
        ).increment();
    }

    @Override
    public void recordTicketCancelled(Ticket ticket) {
        counter("management_tickets_ticket_cancelled_total", "priority", ticket.getPriority().name()).increment();
    }

    @Override
    public void recordCommentAdded(CommentVisibility visibility) {
        counter("management_tickets_ticket_comment_added_total", "visibility", visibility.name()).increment();
    }

    @Override
    public void recordLoginSuccess(Role role) {
        counter("management_tickets_auth_login_total", "outcome", "success", "role", role.name()).increment();
    }

    @Override
    public void recordLoginFailure(String reason) {
        counter("management_tickets_auth_login_total", "outcome", "failure", "reason", reason).increment();
    }

    @Override
    public void recordRefreshSuccess(Role role) {
        counter("management_tickets_auth_refresh_total", "outcome", "success", "role", role.name()).increment();
    }

    @Override
    public void recordRefreshFailure(String reason) {
        counter("management_tickets_auth_refresh_total", "outcome", "failure", "reason", reason).increment();
    }

    @Override
    public void recordRateLimitBlocked() {
        counter("management_tickets_auth_rate_limited_total").increment();
    }

    private Counter counter(String name, String... tags) {
        return Counter.builder(name)
            .tags(tags)
            .register(meterRegistry);
    }
}
