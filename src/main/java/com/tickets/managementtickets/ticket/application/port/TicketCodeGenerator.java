package com.tickets.managementtickets.ticket.application.port;

import java.time.Instant;

public interface TicketCodeGenerator {

    String nextCode(Instant now);
}
