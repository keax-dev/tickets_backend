package com.tickets.managementtickets.ticket.infrastructure.support;

import com.tickets.managementtickets.ticket.application.port.TicketLifecyclePolicy;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.ticket")
public class TicketLifecycleProperties implements TicketLifecyclePolicy {

    @Positive
    private int autoCloseDays;

    public int getAutoCloseDays() {
        return autoCloseDays;
    }

    public void setAutoCloseDays(int autoCloseDays) {
        this.autoCloseDays = autoCloseDays;
    }
}
