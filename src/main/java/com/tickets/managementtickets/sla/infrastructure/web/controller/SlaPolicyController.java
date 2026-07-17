package com.tickets.managementtickets.sla.infrastructure.web.controller;

import com.tickets.managementtickets.identity.infrastructure.security.CurrentUserService;
import com.tickets.managementtickets.sla.application.service.SlaPolicyService;
import com.tickets.managementtickets.sla.infrastructure.web.dto.SlaPolicyResponse;
import com.tickets.managementtickets.sla.infrastructure.web.dto.UpdateSlaPolicyRequest;
import com.tickets.managementtickets.ticket.domain.model.TicketPriority;
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
public class SlaPolicyController {

    private final SlaPolicyService slaPolicyService;
    private final CurrentUserService currentUserService;

    public SlaPolicyController(SlaPolicyService slaPolicyService, CurrentUserService currentUserService) {
        this.slaPolicyService = slaPolicyService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<SlaPolicyResponse> list() {
        return slaPolicyService.list(currentUserService.requireCurrentUser()).stream()
            .map(SlaPolicyResponse::from)
            .toList();
    }

    @PutMapping("/{priority}")
    public SlaPolicyResponse update(
        @PathVariable TicketPriority priority,
        @Valid @RequestBody UpdateSlaPolicyRequest request
    ) {
        return SlaPolicyResponse.from(
            slaPolicyService.update(
                currentUserService.requireCurrentUser(),
                priority,
                new SlaPolicyService.UpdateSlaPolicyRequest(
                    request.version(),
                    request.firstResponseHours(),
                    request.resolutionHours(),
                    request.active()
                )
            )
        );
    }
}
