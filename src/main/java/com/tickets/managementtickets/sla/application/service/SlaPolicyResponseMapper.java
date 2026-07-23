package com.tickets.managementtickets.sla.application.service;

import com.tickets.managementtickets.sla.application.result.SlaPolicyResponse;
import com.tickets.managementtickets.sla.domain.model.SlaPolicy;

final class SlaPolicyResponseMapper {

    SlaPolicyResponse toResponse(SlaPolicy policy) {
        return new SlaPolicyResponse(
            policy.id(),
            policy.priority(),
            policy.firstResponseHours(),
            policy.resolutionHours(),
            policy.active(),
            policy.version()
        );
    }
}
