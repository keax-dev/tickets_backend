package com.tickets.managementtickets.observability.infrastructure;

import com.tickets.managementtickets.shared.application.port.TransactionRunner;
import com.tickets.managementtickets.ticket.application.port.TicketCountQuery;
import com.tickets.managementtickets.ticket.application.port.TicketRepositoryPort;
import com.tickets.managementtickets.ticket.application.port.TicketVisibility;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

@Component
public class TicketMetricsBinder implements MeterBinder {

    private static final String SYSTEM_USER = "system";

    private final TicketRepositoryPort ticketRepository;
    private final TransactionRunner transactionRunner;

    public TicketMetricsBinder(TicketRepositoryPort ticketRepository, TransactionRunner transactionRunner) {
        this.ticketRepository = ticketRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public void bindTo(MeterRegistry meterRegistry) {
        Gauge.builder("management_tickets_open_total", this, TicketMetricsBinder::countOpenTickets)
            .description("Current number of non-terminal tickets.")
            .register(meterRegistry);

        Gauge.builder("management_tickets_unassigned_total", this, TicketMetricsBinder::countUnassignedTickets)
            .description("Current number of unassigned tickets.")
            .register(meterRegistry);

        Gauge.builder("management_tickets_sla_breached_total", this, TicketMetricsBinder::countBreachedTickets)
            .description("Current number of tickets with at least one breached SLA flag.")
            .register(meterRegistry);
    }

    private double countOpenTickets() {
        return transactionRunner.readOnly(() -> ticketRepository.count(
            TicketCountQuery.visibleTo(TicketVisibility.ALL, SYSTEM_USER).withoutTerminalStatuses()
        ));
    }

    private double countUnassignedTickets() {
        return transactionRunner.readOnly(() -> ticketRepository.count(
            TicketCountQuery.visibleTo(TicketVisibility.ALL, SYSTEM_USER)
                .withoutTerminalStatuses()
                .unassignedOnly()
        ));
    }

    private double countBreachedTickets() {
        return transactionRunner.readOnly(() -> ticketRepository.count(
            TicketCountQuery.visibleTo(TicketVisibility.ALL, SYSTEM_USER)
                .withoutTerminalStatuses()
                .breachedSlaOnly()
        ));
    }
}
