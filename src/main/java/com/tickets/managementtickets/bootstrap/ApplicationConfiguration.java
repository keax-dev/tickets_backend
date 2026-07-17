package com.tickets.managementtickets.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tickets.managementtickets.identity.infrastructure.security.SecurityProperties;
import com.tickets.managementtickets.ticket.infrastructure.support.IdempotencyProperties;
import com.tickets.managementtickets.ticket.infrastructure.support.TicketLifecycleProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({
    SecurityProperties.class,
    IdempotencyProperties.class,
    TicketLifecycleProperties.class
})
public class ApplicationConfiguration {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    ObjectMapper objectMapper() {
        return JsonMapper.builder()
            .findAndAddModules()
            .build();
    }
}
