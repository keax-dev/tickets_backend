package com.tickets.managementtickets.dashboard.infrastructure.web.controller;

import com.tickets.managementtickets.dashboard.application.service.DashboardService;
import com.tickets.managementtickets.dashboard.infrastructure.web.dto.DashboardSummaryResponse;
import com.tickets.managementtickets.dashboard.infrastructure.web.dto.RecentActivityResponse;
import com.tickets.managementtickets.identity.application.port.CurrentAuthenticatedUserProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentAuthenticatedUserProvider currentUserProvider;

    public DashboardController(DashboardService dashboardService, CurrentAuthenticatedUserProvider currentUserProvider) {
        this.dashboardService = dashboardService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse summary() {
        return DashboardSummaryResponse.from(dashboardService.summary(currentUserProvider.requireCurrentUser()));
    }

    @GetMapping("/recent-activity")
    public List<RecentActivityResponse> recentActivity() {
        return dashboardService.recentActivity(currentUserProvider.requireCurrentUser()).stream()
            .map(RecentActivityResponse::from)
            .toList();
    }
}
