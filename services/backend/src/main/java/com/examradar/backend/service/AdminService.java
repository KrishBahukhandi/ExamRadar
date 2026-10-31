package com.examradar.backend.service;

import com.examradar.backend.collector.IngestionEngine;
import com.examradar.backend.domain.NotificationJob;
import com.examradar.backend.domain.ParserFailure;
import com.examradar.backend.domain.SourceFetchRun;
import com.examradar.backend.domain.enums.JobStatus;
import com.examradar.backend.dto.CollectorStatusDto;
import com.examradar.backend.dto.SourceHealthDto;
import com.examradar.backend.notification.FcmPushService;
import com.examradar.backend.repository.NotificationJobRepository;
import com.examradar.backend.repository.ParserFailureRepository;
import com.examradar.backend.repository.SourceFetchRunRepository;
import com.examradar.backend.repository.SourceRepository;
import com.examradar.backend.scheduler.ScheduledAutomationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private final SourceRepository sourceRepository;
    private final SourceFetchRunRepository fetchRunRepository;
    private final ParserFailureRepository parserFailureRepository;
    private final NotificationJobRepository notificationJobRepository;
    private final IngestionEngine ingestionEngine;
    private final FcmPushService fcmPushService;
    private final ScheduledAutomationService scheduledAutomationService;

    public AdminService(
            SourceRepository sourceRepository,
            SourceFetchRunRepository fetchRunRepository,
            ParserFailureRepository parserFailureRepository,
            NotificationJobRepository notificationJobRepository,
            IngestionEngine ingestionEngine,
            FcmPushService fcmPushService,
            ScheduledAutomationService scheduledAutomationService
    ) {
        this.sourceRepository = sourceRepository;
        this.fetchRunRepository = fetchRunRepository;
        this.parserFailureRepository = parserFailureRepository;
        this.notificationJobRepository = notificationJobRepository;
        this.ingestionEngine = ingestionEngine;
        this.fcmPushService = fcmPushService;
        this.scheduledAutomationService = scheduledAutomationService;
    }

    @org.springframework.cache.annotation.Cacheable(value = com.examradar.backend.config.CacheConfig.CACHE_SOURCES)
    public List<SourceHealthDto> getSources() {
        return sourceRepository.findAll().stream().map(s -> new SourceHealthDto(
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
    }

    public List<SourceFetchRun> getSourceHealth(String sourceId) {
        if (!sourceRepository.existsById(sourceId)) {
            throw new NoSuchElementException("Source not registered: " + sourceId);
        }
        return fetchRunRepository.findTop10BySourceIdOrderByCreatedAtDesc(sourceId);
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = com.examradar.backend.config.CacheConfig.CACHE_SOURCES, allEntries = true)
    public IngestionEngine.IngestionSummary triggerSourceRun(String sourceId) {
        return ingestionEngine.runIngestionForSource(sourceId);
    }

    public List<ParserFailure> getParserFailures() {
        return parserFailureRepository.findTop20ByOrderByCreatedAtDesc();
    }

    public List<NotificationJob> getNotificationJobs() {
        return notificationJobRepository.findAll();
    }

    @Transactional
    public int processDueNotifications() {
        return fcmPushService.processDueNotificationJobs();
    }

    public CollectorStatusDto getCollectorStatus() {
        Runtime runtime = Runtime.getRuntime();
        long freeMb = runtime.freeMemory() / (1024 * 1024);
        long totalMb = runtime.totalMemory() / (1024 * 1024);
        long maxMb = runtime.maxMemory() / (1024 * 1024);

        long enabledSources = sourceRepository.countByIsEnabledTrue();
        long totalSources = sourceRepository.count();
        long pendingJobs = notificationJobRepository.countByStatus(JobStatus.PENDING);

        return new CollectorStatusDto(
                scheduledAutomationService.isAutoScrapeEnabled(),
                scheduledAutomationService.isAutoPushEnabled(),
                scheduledAutomationService.isIngestionRunning(),
                scheduledAutomationService.getLastIngestionStartTime(),
                scheduledAutomationService.getLastIngestionEndTime(),
                scheduledAutomationService.getLastIngestionDurationMs(),
                scheduledAutomationService.getLastIngestionNewNotices(),
                scheduledAutomationService.getLastIngestionRevisedNotices(),
                enabledSources,
                totalSources,
                pendingJobs,
                freeMb,
                totalMb,
                maxMb
        );
    }

    @Transactional
    public ScheduledAutomationService.IngestionRunResult triggerAllSources() {
        return scheduledAutomationService.executeManualIngestionAll();
    }

    public String broadcastTestTopic(String topic, String title, String body) {
        return fcmPushService.broadcastToTopic(
                topic != null && !topic.isBlank() ? topic : "notices_all",
                title != null && !title.isBlank() ? title : "ExamRadar Test Alert",
                body != null && !body.isBlank() ? body : "Test push broadcast from ExamRadar administration plane.",
                java.util.Map.of("type", "TEST", "timestamp", String.valueOf(System.currentTimeMillis()))
        );
    }
}
