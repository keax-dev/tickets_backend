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
        TicketSequenceEntity sequence = repository.findByYearForUpdate(currentYear)
            .orElseGet(() -> {
                TicketSequenceEntity createdSequence = new TicketSequenceEntity();
                createdSequence.setSequenceYear(currentYear);
                createdSequence.setCurrentValue(0L);
                return repository.save(createdSequence);
            });

        sequence.setCurrentValue(sequence.getCurrentValue() + 1);
        repository.save(sequence);
        return "TCK-" + currentYear + "-" + String.format("%06d", sequence.getCurrentValue());
    }
}
