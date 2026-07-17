package com.tickets.managementtickets.bootstrap;

import com.tickets.managementtickets.identity.application.service.AuthService;
import com.tickets.managementtickets.ticket.application.service.TicketService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceScheduler {

    private final AuthService authService;
    private final TicketService ticketService;

    public MaintenanceScheduler(AuthService authService, TicketService ticketService) {
        this.authService = authService;
        this.ticketService = ticketService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void purgeExpiredRecords() {
        authService.purgeExpiredRefreshTokens();
        ticketService.purgeExpiredIdempotencyRecords();
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void autoCloseResolvedTickets() {
        ticketService.autoCloseResolvedTickets();
    }
}
