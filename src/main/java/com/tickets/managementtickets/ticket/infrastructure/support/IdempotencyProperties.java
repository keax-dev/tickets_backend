package com.tickets.managementtickets.ticket.infrastructure.support;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.idempotency")
public class IdempotencyProperties {

    @Positive
    private int recordTtlHours;

    public int getRecordTtlHours() {
        return recordTtlHours;
    }

    public void setRecordTtlHours(int recordTtlHours) {
        this.recordTtlHours = recordTtlHours;
    }
}
