package com.examradar.backend.scheduler;

import com.examradar.backend.collector.IngestionEngine;
import com.examradar.backend.domain.Source;
import com.examradar.backend.notification.FcmPushService;
import com.examradar.backend.repository.SourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

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
     * Runs every 1 hour (3600000 ms) with a 2-minute initial delay on server start.
     */
    @Scheduled(fixedDelayString = "${examradar.collector.scheduled-interval-ms:3600000}", initialDelayString = "${examradar.collector.initial-delay-ms:300000}")
    public void runScheduledSourceIngestion() {
        if (!autoScrapeEnabled) {
            log.debug("Automated source scraping is disabled via configuration.");
            return;
        }

        log.info("Starting scheduled official source ingestion run...");
        List<Source> enabledSources = sourceRepository.findByIsEnabledTrue();

        int totalNewNotices = 0;
        int totalRevisedNotices = 0;

        for (Source source : enabledSources) {
            try {
                IngestionEngine.IngestionSummary summary = ingestionEngine.runIngestionForSource(source.getId());
                if (summary.isSuccess()) {
                    totalNewNotices += summary.newNotices();
                    totalRevisedNotices += summary.revisedNotices();
                } else {
                    log.warn("Ingestion failed for source [{}]: {}", source.getId(), summary.message());
                }
            } catch (Exception e) {
                log.error("Unhandled error during scheduled ingestion of [{}]: {}", source.getId(), e.getMessage());
            }
        }

        log.info("Scheduled ingestion run complete: {} new notices, {} revised across {} sources.",
                totalNewNotices, totalRevisedNotices, enabledSources.size());
    }

    /**
     * Periodically processes pending and due push notification jobs in the queue.
     * Runs every 60 seconds with an initial delay of 30 seconds.
     */
    @Scheduled(fixedDelayString = "${examradar.notification.process-interval-ms:60000}", initialDelay = 30000)
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
}
