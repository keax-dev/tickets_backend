package com.tickets.managementtickets.sla.infrastructure.web.controller;

import com.tickets.managementtickets.identity.application.port.CurrentAuthenticatedUserProvider;
import com.tickets.managementtickets.sla.application.service.SlaPolicyService;
import com.tickets.managementtickets.sla.infrastructure.web.dto.SlaPolicyResponse;
import com.tickets.managementtickets.sla.infrastructure.web.dto.UpdateSlaPolicyRequest;
import com.tickets.managementtickets.shared.domain.model.TicketPriority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sla-policies")
@Tag(name = "SLA Policies", description = "SLA policy catalog administration endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class SlaPolicyController {

    private final SlaPolicyService slaPolicyService;
    private final CurrentAuthenticatedUserProvider currentUserProvider;

    public SlaPolicyController(SlaPolicyService slaPolicyService, CurrentAuthenticatedUserProvider currentUserProvider) {
        this.slaPolicyService = slaPolicyService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    @Operation(summary = "List SLA policies", description = "Returns the SLA policy catalog for all supported ticket priorities.")
    public List<SlaPolicyResponse> list() {
        return slaPolicyService.list(currentUserProvider.requireCurrentUser()).stream()
            .map(SlaPolicyResponse::from)
            .toList();
    }

    @PutMapping("/{priority}")
    @Operation(summary = "Update an SLA policy", description = "Updates first-response hours, resolution hours, and active flag for a priority.")
    public SlaPolicyResponse update(
        @PathVariable TicketPriority priority,
        @Valid @RequestBody UpdateSlaPolicyRequest request
    ) {
        return SlaPolicyResponse.from(
            slaPolicyService.update(
                currentUserProvider.requireCurrentUser(),
                priority,
                new com.tickets.managementtickets.sla.application.command.UpdateSlaPolicyRequest(
                    request.version(),
                    request.firstResponseHours(),
                    request.resolutionHours(),
                    request.active()
                )
            )
        );
    }
}
