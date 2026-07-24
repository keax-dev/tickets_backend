package com.tickets.managementtickets.dashboard.infrastructure.web.controller;

import com.tickets.managementtickets.dashboard.application.service.DashboardService;
import com.tickets.managementtickets.dashboard.infrastructure.web.dto.DashboardSummaryResponse;
import com.tickets.managementtickets.dashboard.infrastructure.web.dto.RecentActivityResponse;
import com.tickets.managementtickets.identity.application.port.CurrentAuthenticatedUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "Operational dashboard endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentAuthenticatedUserProvider currentUserProvider;

    public DashboardController(DashboardService dashboardService, CurrentAuthenticatedUserProvider currentUserProvider) {
        this.dashboardService = dashboardService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get dashboard summary", description = "Returns aggregate counters and SLA indicators for the authenticated user.")
    public DashboardSummaryResponse summary() {
        return DashboardSummaryResponse.from(dashboardService.summary(currentUserProvider.requireCurrentUser()));
    }

    @GetMapping("/recent-activity")
    @Operation(summary = "Get recent dashboard activity", description = "Returns recent ticket activity items visible to the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Recent activity returned successfully.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = RecentActivityResponse.class))))
    public List<RecentActivityResponse> recentActivity() {
        return dashboardService.recentActivity(currentUserProvider.requireCurrentUser()).stream()
            .map(RecentActivityResponse::from)
            .toList();
    }
}
