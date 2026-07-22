package com.tickets.managementtickets.ticket.application.port;

public interface IdempotencyPolicy {

    int getRecordTtlHours();
}
