package com.indocrm.indocrm.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.indocrm.indocrm.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('TELECALLER','EMPLOYEE','ADMIN','SUPER_ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public Map<String, Object> dashboard() {
        return dashboardService.getDashboard();
    }

    /** Everything the Admin / Super Admin dashboard page needs (real data, no mock). */
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @GetMapping("/admin")
    public Map<String, Object> adminDashboard() {
        return dashboardService.getAdminDashboard();
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return dashboardService.getDashboard();
    }

    // Backward-compatible endpoints used by existing frontend code.
    @SuppressWarnings("unchecked")
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return (Map<String, Object>) dashboardService.getDashboard().get("summary");
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/today-summary")
    public Map<String, Object> todaySummary() {
        return (Map<String, Object>) dashboardService.getDashboard().get("today");
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/recent-calls")
    public List<Map<String, Object>> recentCalls() {
        return (List<Map<String, Object>>) dashboardService.getDashboard().get("recentCalls");
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/performance")
    public List<Map<String, Object>> performance() {
        return (List<Map<String, Object>>) dashboardService.getDashboard().get("performanceTrend");
    }
}