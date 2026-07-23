package com.tickets.managementtickets.ticket.infrastructure.persistence.adapter;

import com.tickets.managementtickets.ticket.application.port.TicketCodeGenerator;
import com.tickets.managementtickets.ticket.infrastructure.persistence.entity.TicketSequenceEntity;
import com.tickets.managementtickets.ticket.infrastructure.persistence.repository.TicketSequenceRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;

@Component
public class JpaTicketCodeGenerator implements TicketCodeGenerator {

    private final TicketSequenceRepository repository;

    public JpaTicketCodeGenerator(TicketSequenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public String nextCode(Instant now) {
        int currentYear = now.atZone(ZoneOffset.UTC).getYear();
        repository.ensureSequenceExists(currentYear);
        TicketSequenceEntity sequence = repository.findByYearForUpdate(currentYear)
            .orElseThrow(() -> new IllegalStateException("Ticket sequence could not be initialized."));

        sequence.setCurrentValue(sequence.getCurrentValue() + 1);
        repository.save(sequence);
        return "TCK-" + currentYear + "-" + String.format("%06d", sequence.getCurrentValue());
    }
}
