package com.tickets.managementtickets.sla.application.service;

import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.application.service.AuthorizationService;
import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.shared.application.exception.ConflictException;
import com.tickets.managementtickets.shared.application.exception.NotFoundException;
import com.tickets.managementtickets.shared.application.exception.ValidationException;
import com.tickets.managementtickets.sla.infrastructure.persistence.entity.SlaPolicyEntity;
import com.tickets.managementtickets.sla.infrastructure.persistence.repository.SlaPolicyRepository;
import com.tickets.managementtickets.ticket.domain.model.TicketPriority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SlaPolicyService {

    private final SlaPolicyRepository slaPolicyRepository;
    private final AuthorizationService authorizationService;

    public SlaPolicyService(SlaPolicyRepository slaPolicyRepository, AuthorizationService authorizationService) {
        this.slaPolicyRepository = slaPolicyRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public List<SlaPolicyResponse> list(AuthenticatedUser currentUser) {
        authorizationService.requirePermission(currentUser, Permission.SLA_READ);
        return slaPolicyRepository.findAll()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public SlaPolicyResponse update(AuthenticatedUser currentUser, TicketPriority priority, UpdateSlaPolicyRequest request) {
        authorizationService.requirePermission(currentUser, Permission.SLA_UPDATE);
        if (request.firstResponseHours() <= 0 || request.resolutionHours() <= 0) {
            throw new ValidationException("INVALID_SLA_POLICY", "SLA values must be greater than zero.");
        }

        SlaPolicyEntity policy = slaPolicyRepository.findByPriority(priority)
            .orElseThrow(() -> new NotFoundException("SLA_POLICY_NOT_FOUND", "The SLA policy could not be found."));
        ensureVersion(policy.getVersion(), request.version(), "The SLA policy was modified by another request.");

        policy.setFirstResponseHours(request.firstResponseHours());
        policy.setResolutionHours(request.resolutionHours());
        policy.setActive(request.active());
        return toResponse(policy);
    }

    private void ensureVersion(long currentVersion, long requestedVersion, String message) {
        if (currentVersion != requestedVersion) {
            throw new ConflictException("RESOURCE_VERSION_CONFLICT", message);
        }
    }

    private SlaPolicyResponse toResponse(SlaPolicyEntity policy) {
        return new SlaPolicyResponse(
            policy.getId(),
            policy.getPriority(),
            policy.getFirstResponseHours(),
            policy.getResolutionHours(),
            policy.isActive(),
            policy.getVersion()
        );
    }

    public record UpdateSlaPolicyRequest(long version, int firstResponseHours, int resolutionHours, boolean active) {
    }

    public record SlaPolicyResponse(
        String id,
        TicketPriority priority,
        int firstResponseHours,
        int resolutionHours,
        boolean active,
        long version
    ) {
    }
}
