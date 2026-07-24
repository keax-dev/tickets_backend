package com.tickets.managementtickets.bootstrap;

import com.tickets.managementtickets.identity.application.service.AuthService;
import com.tickets.managementtickets.ticket.application.service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceScheduler {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceScheduler.class);

    private final AuthService authService;
    private final TicketService ticketService;

    public MaintenanceScheduler(AuthService authService, TicketService ticketService) {
        this.authService = authService;
        this.ticketService = ticketService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void purgeExpiredRecords() {
        log.info("Running maintenance task: purge expired refresh tokens and idempotency records.");
        authService.purgeExpiredRefreshTokens();
        ticketService.purgeExpiredIdempotencyRecords();
        log.info("Completed maintenance task: purge expired refresh tokens and idempotency records.");
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void autoCloseResolvedTickets() {
        log.info("Running maintenance task: auto-close resolved tickets.");
        ticketService.autoCloseResolvedTickets();
        log.info("Completed maintenance task: auto-close resolved tickets.");
    }
}
