package com.tickets.managementtickets.ticket.infrastructure.support;

import com.tickets.managementtickets.ticket.application.port.IdempotencyPolicy;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.idempotency")
public class IdempotencyProperties implements IdempotencyPolicy {

    @Positive
    private int recordTtlHours;

    public int getRecordTtlHours() {
        return recordTtlHours;
    }

    public void setRecordTtlHours(int recordTtlHours) {
        this.recordTtlHours = recordTtlHours;
    }
}
