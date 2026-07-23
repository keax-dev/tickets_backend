package com.tickets.managementtickets.sla.application.service;

import com.tickets.managementtickets.shared.application.exception.ValidationException;
import com.tickets.managementtickets.shared.application.service.ResourceVersionPolicy;
import com.tickets.managementtickets.sla.application.command.UpdateSlaPolicyRequest;
import com.tickets.managementtickets.sla.domain.model.SlaPolicy;

final class SlaPolicyCommandValidator {

    void ensureValid(UpdateSlaPolicyRequest request) {
        if (request.firstResponseHours() <= 0 || request.resolutionHours() <= 0) {
            throw new ValidationException("INVALID_SLA_POLICY", "SLA values must be greater than zero.");
        }
    }

    void ensureVersion(SlaPolicy policy, long requestedVersion) {
        ResourceVersionPolicy.ensureCurrent(policy.version(), requestedVersion, "The SLA policy was modified by another request.");
    }
}
