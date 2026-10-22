package com.examradar.backend.controller;

import com.examradar.backend.collector.IngestionEngine;
import com.examradar.backend.domain.NotificationJob;
import com.examradar.backend.domain.ParserFailure;
import com.examradar.backend.domain.SourceFetchRun;
import com.examradar.backend.dto.ApiResponse;
import com.examradar.backend.dto.SourceHealthDto;
import com.examradar.backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration", description = "Privileged endpoints for source health, manual collector execution, and notification audits")
@SecurityRequirement(name = "adminAuth")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/sources")
    @Operation(summary = "List all official source registries, polling intervals, and health status")
    public ApiResponse<List<SourceHealthDto>> getSources() {
        return ApiResponse.ok(adminService.getSources());
    }

    @GetMapping("/sources/{id}/health")
    @Operation(summary = "Inspect detailed health and recent fetch runs for a source")
    public ApiResponse<List<SourceFetchRun>> getSourceHealth(@PathVariable String id) {
        return ApiResponse.ok(adminService.getSourceHealth(id));
    }

    @PostMapping("/sources/{id}/run")
    @Operation(summary = "Manually trigger on-demand ingestion for a specific official source")
    public ApiResponse<IngestionEngine.IngestionSummary> triggerSourceRun(@PathVariable String id) {
        IngestionEngine.IngestionSummary summary = adminService.triggerSourceRun(id);
        return ApiResponse.ok("Collector execution finished", summary);
    }

    @GetMapping("/parser-failures")
    @Operation(summary = "Retrieve recent official parser failures and quarantine logs")
    public ApiResponse<List<ParserFailure>> getParserFailures() {
        return ApiResponse.ok(adminService.getParserFailures());
    }

    @GetMapping("/notification-jobs")
    @Operation(summary = "Inspect recent notification jobs and delivery states")
    public ApiResponse<List<NotificationJob>> getNotificationJobs() {
        return ApiResponse.ok(adminService.getNotificationJobs());
    }

    @PostMapping("/notifications/process-due")
    @Operation(summary = "Manually trigger processing of all pending due notification jobs")
    public ApiResponse<Integer> processDueNotifications() {
        return ApiResponse.ok("Processed due notification jobs", adminService.processDueNotifications());
    }
}
