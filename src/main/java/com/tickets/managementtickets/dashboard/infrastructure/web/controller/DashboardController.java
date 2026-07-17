package com.tickets.managementtickets.dashboard.infrastructure.web.controller;

import com.tickets.managementtickets.dashboard.application.service.DashboardService;
import com.tickets.managementtickets.dashboard.infrastructure.web.dto.DashboardSummaryResponse;
import com.tickets.managementtickets.dashboard.infrastructure.web.dto.RecentActivityResponse;
import com.tickets.managementtickets.identity.infrastructure.security.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUserService currentUserService;

    public DashboardController(DashboardService dashboardService, CurrentUserService currentUserService) {
        this.dashboardService = dashboardService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse summary() {
        return DashboardSummaryResponse.from(dashboardService.summary(currentUserService.requireCurrentUser()));
    }

    @GetMapping("/recent-activity")
    public List<RecentActivityResponse> recentActivity() {
        return dashboardService.recentActivity(currentUserService.requireCurrentUser()).stream()
            .map(RecentActivityResponse::from)
            .toList();
    }
}
