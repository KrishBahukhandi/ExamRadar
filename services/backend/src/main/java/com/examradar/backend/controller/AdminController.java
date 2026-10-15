package com.examradar.backend.controller;

import com.examradar.backend.collector.IngestionEngine;
import com.examradar.backend.domain.NotificationJob;
import com.examradar.backend.domain.ParserFailure;
import com.examradar.backend.domain.Source;
import com.examradar.backend.domain.SourceFetchRun;
import com.examradar.backend.dto.ApiResponse;
import com.examradar.backend.dto.SourceHealthDto;
import com.examradar.backend.notification.FcmPushService;
import com.examradar.backend.repository.NotificationJobRepository;
import com.examradar.backend.repository.ParserFailureRepository;
import com.examradar.backend.repository.SourceFetchRunRepository;
import com.examradar.backend.repository.SourceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration", description = "Privileged endpoints for source health, manual collector execution, and notification audits")
@SecurityRequirement(name = "adminAuth")
public class AdminController {

    private final SourceRepository sourceRepository;
    private final SourceFetchRunRepository fetchRunRepository;
    private final ParserFailureRepository parserFailureRepository;
    private final NotificationJobRepository notificationJobRepository;
    private final IngestionEngine ingestionEngine;
    private final FcmPushService fcmPushService;

    public AdminController(
            SourceRepository sourceRepository,
            SourceFetchRunRepository fetchRunRepository,
            ParserFailureRepository parserFailureRepository,
            NotificationJobRepository notificationJobRepository,
            IngestionEngine ingestionEngine,
            FcmPushService fcmPushService
    ) {
        this.sourceRepository = sourceRepository;
        this.fetchRunRepository = fetchRunRepository;
        this.parserFailureRepository = parserFailureRepository;
        this.notificationJobRepository = notificationJobRepository;
        this.ingestionEngine = ingestionEngine;
        this.fcmPushService = fcmPushService;
    }

    @GetMapping("/sources")
    @Operation(summary = "List all official source registries, polling intervals, and health status")
    public ApiResponse<List<SourceHealthDto>> getSources() {
        List<SourceHealthDto> dtos = sourceRepository.findAll().stream().map(s -> new SourceHealthDto(
                s.getId(),
                s.getOrganization(),
                s.getSourceName(),
                s.getOfficialBaseUrl(),
                s.getIsEnabled(),
                s.getLastAttemptedFetch(),
                s.getLastSuccessfulFetch(),
                s.getConsecutiveFailures(),
                s.getLastErrorMessage(),
                s.getLastSuccessfulContentHash()
        )).toList();

        return ApiResponse.ok(dtos);
    }

    @GetMapping("/sources/{id}/health")
    @Operation(summary = "Inspect detailed health and recent fetch runs for a source")
    public ApiResponse<List<SourceFetchRun>> getSourceHealth(@PathVariable String id) {
        if (!sourceRepository.existsById(id)) {
            throw new NoSuchElementException("Source not registered: " + id);
        }
        List<SourceFetchRun> runs = fetchRunRepository.findTop10BySourceIdOrderByCreatedAtDesc(id);
        return ApiResponse.ok(runs);
    }

    @PostMapping("/sources/{id}/run")
    @Operation(summary = "Manually trigger on-demand ingestion for a specific official source")
    public ApiResponse<IngestionEngine.IngestionSummary> triggerSourceRun(@PathVariable String id) {
        IngestionEngine.IngestionSummary summary = ingestionEngine.runIngestionForSource(id);
        return ApiResponse.ok("Collector execution finished", summary);
    }

    @GetMapping("/parser-failures")
    @Operation(summary = "Retrieve recent official parser failures and quarantine logs")
    public ApiResponse<List<ParserFailure>> getParserFailures() {
        return ApiResponse.ok(parserFailureRepository.findTop20ByOrderByCreatedAtDesc());
    }

    @GetMapping("/notification-jobs")
    @Operation(summary = "Inspect recent notification jobs and delivery states")
    public ApiResponse<List<NotificationJob>> getNotificationJobs() {
        return ApiResponse.ok(notificationJobRepository.findAll());
    }

    @PostMapping("/notifications/process-due")
    @Operation(summary = "Manually trigger processing of all pending due notification jobs")
    public ApiResponse<Integer> processDueNotifications() {
        int processedCount = fcmPushService.processDueNotificationJobs();
        return ApiResponse.ok("Processed due notification jobs", processedCount);
    }
}
