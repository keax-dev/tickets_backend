package com.tickets.managementtickets.bootstrap;

import com.tickets.managementtickets.category.infrastructure.persistence.entity.CategoryEntity;
import com.tickets.managementtickets.category.infrastructure.persistence.repository.CategoryRepository;
import com.tickets.managementtickets.identity.domain.model.Role;
import com.tickets.managementtickets.identity.infrastructure.persistence.entity.UserEntity;
import com.tickets.managementtickets.identity.infrastructure.persistence.repository.UserRepository;
import com.tickets.managementtickets.sla.infrastructure.persistence.entity.SlaPolicyEntity;
import com.tickets.managementtickets.sla.infrastructure.persistence.repository.SlaPolicyRepository;
import com.tickets.managementtickets.ticket.domain.model.TicketPriority;
import com.tickets.managementtickets.ticket.domain.model.TicketStatus;
import com.tickets.managementtickets.ticket.infrastructure.persistence.entity.TicketEntity;
import com.tickets.managementtickets.ticket.infrastructure.persistence.entity.TicketSequenceEntity;
import com.tickets.managementtickets.ticket.infrastructure.persistence.repository.TicketRepository;
import com.tickets.managementtickets.ticket.infrastructure.persistence.repository.TicketSequenceRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Component
@Profile("local")
public class LocalDevelopmentSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final TicketRepository ticketRepository;
    private final TicketSequenceRepository ticketSequenceRepository;
    private final Clock clock;

    public LocalDevelopmentSeeder(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        CategoryRepository categoryRepository,
        SlaPolicyRepository slaPolicyRepository,
        TicketRepository ticketRepository,
        TicketSequenceRepository ticketSequenceRepository,
        Clock clock
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.categoryRepository = categoryRepository;
        this.slaPolicyRepository = slaPolicyRepository;
        this.ticketRepository = ticketRepository;
        this.ticketSequenceRepository = ticketSequenceRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureUser("admin@tickets.local", "Admin", "Support", Role.ADMIN);
        UserEntity manager = ensureUser("manager@tickets.local", "Manager", "Support", Role.SUPPORT_MANAGER);
        UserEntity agent = ensureUser("agent@tickets.local", "Agent", "Support", Role.SUPPORT_AGENT);
        UserEntity customer = ensureUser("customer@tickets.local", "Cliente", "Demo", Role.CUSTOMER);

        ensureDefaultCategories();
        ensureDefaultSlaPolicies();

        if (ticketRepository.count() > 0) {
            return;
        }

        CategoryEntity category = categoryRepository.findByNameIgnoreCase("Access")
            .orElseGet(() -> categoryRepository.findAll().stream().findFirst().orElse(null));
        SlaPolicyEntity slaPolicy = slaPolicyRepository.findByPriority(TicketPriority.MEDIUM).orElse(null);
        if (category == null || slaPolicy == null) {
            return;
        }

        createDemoTicket(
            "No puedo iniciar sesion",
            "El usuario no logra ingresar al sistema corporativo.",
            customer,
            agent,
            manager,
            category,
            slaPolicy,
            TicketStatus.ASSIGNED
        );
        createDemoTicket(
            "Solicitud de acceso a facturacion",
            "Se requiere acceso al modulo de facturacion para el cierre mensual.",
            customer,
            agent,
            manager,
            category,
            slaPolicy,
            TicketStatus.IN_PROGRESS
        );
        createDemoTicket(
            "Equipo no enciende",
            "La computadora asignada no enciende desde esta manana.",
            customer,
            agent,
            manager,
            category,
            slaPolicy,
            TicketStatus.WAITING_FOR_CUSTOMER
        );
    }

    private void ensureDefaultCategories() {
        ensureCategory("Access", "Access and login related issues");
        ensureCategory("Hardware", "Hardware issues and equipment requests");
        ensureCategory("Software", "Software errors and installation requests");
        ensureCategory("Network", "Network and connectivity incidents");
        ensureCategory("Service Request", "General service requests");
        ensureCategory("Other", "Other requests");
    }

    private void ensureDefaultSlaPolicies() {
        ensureSlaPolicy(TicketPriority.LOW, 24, 72);
        ensureSlaPolicy(TicketPriority.MEDIUM, 8, 48);
        ensureSlaPolicy(TicketPriority.HIGH, 4, 24);
        ensureSlaPolicy(TicketPriority.URGENT, 2, 8);
    }

    private UserEntity ensureUser(String email, String firstName, String lastName, Role role) {
        return userRepository.findByEmail(email)
            .orElseGet(() -> {
                UserEntity user = new UserEntity();
                user.setFirstName(firstName);
                user.setLastName(lastName);
                user.setEmail(email);
                user.setPasswordHash(passwordEncoder.encode("Password123!"));
                user.setRole(role);
                user.setActive(true);
                return userRepository.save(user);
            });
    }

    private CategoryEntity ensureCategory(String name, String description) {
        return categoryRepository.findByNameIgnoreCase(name)
            .orElseGet(() -> {
                CategoryEntity category = new CategoryEntity();
                category.setName(name);
                category.setDescription(description);
                category.setActive(true);
                return categoryRepository.save(category);
            });
    }

    private SlaPolicyEntity ensureSlaPolicy(TicketPriority priority, int firstResponseHours, int resolutionHours) {
        return slaPolicyRepository.findByPriority(priority)
            .orElseGet(() -> {
                SlaPolicyEntity slaPolicy = new SlaPolicyEntity();
                slaPolicy.setPriority(priority);
                slaPolicy.setFirstResponseHours(firstResponseHours);
                slaPolicy.setResolutionHours(resolutionHours);
                slaPolicy.setActive(true);
                return slaPolicyRepository.save(slaPolicy);
            });
    }

    private void createDemoTicket(
        String title,
        String description,
        UserEntity requester,
        UserEntity agent,
        UserEntity manager,
        CategoryEntity category,
        SlaPolicyEntity slaPolicy,
        TicketStatus status
    ) {
        Instant now = clock.instant();
        TicketEntity ticket = new TicketEntity();
        ticket.setCode(generateTicketCode(now));
        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setStatus(status);
        ticket.setPriority(TicketPriority.MEDIUM);
        ticket.setRequesterId(requester.getId());
        ticket.setAssignedAgentId(agent.getId());
        ticket.setCategoryId(category.getId());
        ticket.setFirstResponseDueAt(now.plus(slaPolicy.getFirstResponseHours(), ChronoUnit.HOURS));
        ticket.setResolutionDueAt(now.plus(slaPolicy.getResolutionHours(), ChronoUnit.HOURS));
        if (status == TicketStatus.IN_PROGRESS || status == TicketStatus.WAITING_FOR_CUSTOMER) {
            ticket.setFirstRespondedAt(now.minus(1, ChronoUnit.HOURS));
        }
        if (status == TicketStatus.WAITING_FOR_CUSTOMER) {
            ticket.setSlaPausedAt(now.minus(2, ChronoUnit.HOURS));
        }
        ticketRepository.save(ticket);
    }

    private String generateTicketCode(Instant now) {
        int currentYear = now.atZone(ZoneOffset.UTC).getYear();
        TicketSequenceEntity sequence = ticketSequenceRepository.findByYearForUpdate(currentYear)
            .orElseGet(() -> {
                TicketSequenceEntity createdSequence = new TicketSequenceEntity();
                createdSequence.setSequenceYear(currentYear);
                createdSequence.setCurrentValue(0L);
                return ticketSequenceRepository.save(createdSequence);
            });

        sequence.setCurrentValue(sequence.getCurrentValue() + 1);
        ticketSequenceRepository.save(sequence);
        return "TCK-" + currentYear + "-" + String.format("%06d", sequence.getCurrentValue());
    }
}
