package com.tickets.managementtickets.sla.application.service;

import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.application.service.AuthorizationService;
import com.tickets.managementtickets.identity.domain.model.Permission;
import com.tickets.managementtickets.shared.application.exception.ConflictException;
import com.tickets.managementtickets.shared.application.exception.NotFoundException;
import com.tickets.managementtickets.shared.application.exception.ValidationException;
import com.tickets.managementtickets.shared.application.port.TransactionRunner;
import com.tickets.managementtickets.sla.application.command.UpdateSlaPolicyRequest;
import com.tickets.managementtickets.sla.application.port.SlaPolicyRepositoryPort;
import com.tickets.managementtickets.sla.application.result.SlaPolicyResponse;
import com.tickets.managementtickets.sla.domain.model.SlaPolicy;
import com.tickets.managementtickets.shared.domain.model.TicketPriority;

import java.util.List;

public class SlaPolicyService {

    private final SlaPolicyRepositoryPort slaPolicyRepository;
    private final AuthorizationService authorizationService;
    private final TransactionRunner transactionRunner;
    private final SlaPolicyResponseMapper responseMapper;

    public SlaPolicyService(
        SlaPolicyRepositoryPort slaPolicyRepository,
        AuthorizationService authorizationService,
        TransactionRunner transactionRunner
    ) {
        this.slaPolicyRepository = slaPolicyRepository;
        this.authorizationService = authorizationService;
        this.transactionRunner = transactionRunner;
        this.responseMapper = new SlaPolicyResponseMapper();
    }

    public List<SlaPolicyResponse> list(AuthenticatedUser currentUser) {
        return transactionRunner.readOnly(() -> {
            authorizationService.requirePermission(currentUser, Permission.SLA_READ);
            return slaPolicyRepository.findAll()
                .stream()
                .map(responseMapper::toResponse)
                .toList();
        });
    }

    public SlaPolicyResponse update(AuthenticatedUser currentUser, TicketPriority priority, UpdateSlaPolicyRequest request) {
        return transactionRunner.required(() -> {
            authorizationService.requirePermission(currentUser, Permission.SLA_UPDATE);
            if (request.firstResponseHours() <= 0 || request.resolutionHours() <= 0) {
                throw new ValidationException("INVALID_SLA_POLICY", "SLA values must be greater than zero.");
            }

            SlaPolicy policy = slaPolicyRepository.findByPriority(priority)
                .orElseThrow(() -> new NotFoundException("SLA_POLICY_NOT_FOUND", "The SLA policy could not be found."));
            ensureVersion(policy.version(), request.version(), "The SLA policy was modified by another request.");

            return responseMapper.toResponse(slaPolicyRepository.save(policy.update(
                request.firstResponseHours(),
                request.resolutionHours(),
                request.active()
            )));
        });
    }

    private void ensureVersion(long currentVersion, long requestedVersion, String message) {
        if (currentVersion != requestedVersion) {
            throw new ConflictException("RESOURCE_VERSION_CONFLICT", message);
        }
    }

}
