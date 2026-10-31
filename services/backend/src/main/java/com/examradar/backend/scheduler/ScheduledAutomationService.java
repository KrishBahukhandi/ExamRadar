package com.examradar.backend.scheduler;

import com.examradar.backend.collector.IngestionEngine;
import com.examradar.backend.domain.Source;
import com.examradar.backend.notification.FcmPushService;
import com.examradar.backend.repository.SourceRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orchestrates automated background execution of official exam notice scraping
 * and periodic Firebase Cloud Messaging (FCM) push notification queue processing.
 */
@Service
public class ScheduledAutomationService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledAutomationService.class);

    private final IngestionEngine ingestionEngine;
    private final SourceRepository sourceRepository;
    private final FcmPushService fcmPushService;

    @Value("${examradar.collector.auto-scrape-enabled:true}")
    private boolean autoScrapeEnabled;

    @Value("${examradar.notification.auto-push-enabled:true}")
    private boolean autoPushEnabled;

    // Execution metrics & runtime state
    private final AtomicBoolean ingestionRunning = new AtomicBoolean(false);
    private volatile OffsetDateTime lastIngestionStartTime;
    private volatile OffsetDateTime lastIngestionEndTime;
    private volatile Long lastIngestionDurationMs;
    private volatile int lastIngestionNewNotices;
    private volatile int lastIngestionRevisedNotices;

    public ScheduledAutomationService(
            IngestionEngine ingestionEngine,
            SourceRepository sourceRepository,
            FcmPushService fcmPushService
    ) {
        this.ingestionEngine = ingestionEngine;
        this.sourceRepository = sourceRepository;
        this.fcmPushService = fcmPushService;
    }

    /**
     * Periodically runs ingestion across all active official gazette sources (UPSC, SSC, IBPS).
     * Runs every 1 hour (3600000 ms) with a 5-minute initial delay on server start.
     */
    @Scheduled(fixedDelayString = "${examradar.collector.scheduled-interval-ms:3600000}", initialDelayString = "${examradar.collector.initial-delay-ms:300000}")
    @SchedulerLock(name = "ScheduledAutomationService_runScheduledSourceIngestion", lockAtMostFor = "PT25M", lockAtLeastFor = "PT2M")
    public void runScheduledSourceIngestion() {
        if (!autoScrapeEnabled) {
            log.debug("Automated source scraping is disabled via configuration.");
            return;
        }

        executeIngestionAcrossAllSources("SCHEDULED");
    }

    /**
     * Triggers ingestion across all enabled sources on-demand (e.g. from Admin control panel).
     */
    public IngestionRunResult executeManualIngestionAll() {
        return executeIngestionAcrossAllSources("MANUAL_ADMIN");
    }

    private IngestionRunResult executeIngestionAcrossAllSources(String triggerSource) {
        if (!ingestionRunning.compareAndSet(false, true)) {
            log.warn("Ingestion run already in progress. Skipping trigger [{}]", triggerSource);
            return new IngestionRunResult(false, "Ingestion run is already in progress", 0, 0, 0);
        }

        OffsetDateTime start = OffsetDateTime.now();
        lastIngestionStartTime = start;
        long startMs = System.currentTimeMillis();

        log.info("Starting official source ingestion run [trigger={}]...", triggerSource);
        List<Source> enabledSources = sourceRepository.findByIsEnabledTrue();

        int totalNewNotices = 0;
        int totalRevisedNotices = 0;
        int successfulSources = 0;

        try {
            for (Source source : enabledSources) {
                try {
                    IngestionEngine.IngestionSummary summary = ingestionEngine.runIngestionForSource(source.getId());
                    if (summary.isSuccess()) {
                        totalNewNotices += summary.newNotices();
                        totalRevisedNotices += summary.revisedNotices();
                        successfulSources++;
                    } else {
                        log.warn("Ingestion failed for source [{}]: {}", source.getId(), summary.message());
                    }
                } catch (Exception e) {
                    log.error("Unhandled error during ingestion of [{}]: {}", source.getId(), e.getMessage());
                }
            }
        } finally {
            long durationMs = System.currentTimeMillis() - startMs;
            OffsetDateTime end = OffsetDateTime.now();
            lastIngestionEndTime = end;
            lastIngestionDurationMs = durationMs;
            lastIngestionNewNotices = totalNewNotices;
            lastIngestionRevisedNotices = totalRevisedNotices;
            ingestionRunning.set(false);

            // Log memory usage and hint garbage collection for Render 512MB RAM ceiling
            Runtime runtime = Runtime.getRuntime();
            long freeMb = runtime.freeMemory() / (1024 * 1024);
            long totalMb = runtime.totalMemory() / (1024 * 1024);
            log.info("Ingestion run [trigger={}] complete in {}ms: {} new, {} revised across {}/{} sources. JVM Memory: {}MB free / {}MB total",
                    triggerSource, durationMs, totalNewNotices, totalRevisedNotices,
                    successfulSources, enabledSources.size(), freeMb, totalMb);
            System.gc();
        }

        return new IngestionRunResult(true, "Ingestion completed successfully", enabledSources.size(), totalNewNotices, totalRevisedNotices);
    }

    /**
     * Periodically processes pending and due push notification jobs in the queue.
     * Runs every 60 seconds with an initial delay of 30 seconds.
     */
    @Scheduled(fixedDelayString = "${examradar.notification.process-interval-ms:60000}", initialDelay = 30000)
    @SchedulerLock(name = "ScheduledAutomationService_runScheduledPushNotificationProcessing", lockAtMostFor = "PT50S", lockAtLeastFor = "PT10S")
    public void runScheduledPushNotificationProcessing() {
        if (!autoPushEnabled) {
            return;
        }

        try {
            int sentCount = fcmPushService.processDueNotificationJobs();
            if (sentCount > 0) {
                log.info("Processed and sent {} due push notifications via FCM.", sentCount);
            }
        } catch (Exception e) {
            log.error("Error processing scheduled push notification queue: {}", e.getMessage());
        }
    }

    // Status inspection getters
    public boolean isAutoScrapeEnabled() { return autoScrapeEnabled; }
    public boolean isAutoPushEnabled() { return autoPushEnabled; }
    public boolean isIngestionRunning() { return ingestionRunning.get(); }
    public OffsetDateTime getLastIngestionStartTime() { return lastIngestionStartTime; }
    public OffsetDateTime getLastIngestionEndTime() { return lastIngestionEndTime; }
    public Long getLastIngestionDurationMs() { return lastIngestionDurationMs; }
    public int getLastIngestionNewNotices() { return lastIngestionNewNotices; }
    public int getLastIngestionRevisedNotices() { return lastIngestionRevisedNotices; }

    public record IngestionRunResult(
            boolean success,
            String message,
            int sourcesProcessed,
            int newNotices,
            int revisedNotices
    ) {}
}
