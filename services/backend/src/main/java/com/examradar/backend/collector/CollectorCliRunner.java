package com.examradar.backend.collector;

import com.examradar.backend.ExamRadarApplication;
import com.examradar.backend.domain.Source;
import com.examradar.backend.notification.FcmPushService;
import com.examradar.backend.repository.SourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("!test")
public class CollectorCliRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CollectorCliRunner.class);

    private final IngestionEngine ingestionEngine;
    private final SourceRepository sourceRepository;
    private final FcmPushService fcmPushService;

    @Autowired
    public CollectorCliRunner(
            IngestionEngine ingestionEngine,
            SourceRepository sourceRepository,
            FcmPushService fcmPushService
    ) {
        this.ingestionEngine = ingestionEngine;
        this.sourceRepository = sourceRepository;
        this.fcmPushService = fcmPushService;
    }

    public CollectorCliRunner(IngestionEngine ingestionEngine, SourceRepository sourceRepository) {
        this(ingestionEngine, sourceRepository, null);
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(ExamRadarApplication.class)
                .web(WebApplicationType.NONE)
                .properties("examradar.collector.auto-scrape-enabled=false")
                .run(args);
        int exitCode = SpringApplication.exit(ctx, () -> 0);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    @Override
    public void run(String... args) {
        String targetSource = null;
        boolean runAll = false;

        for (String arg : args) {
            if ("--all".equalsIgnoreCase(arg)) {
                runAll = true;
            } else if (arg.startsWith("--source=")) {
                targetSource = arg.substring("--source=".length());
            }
        }

        if (runAll) {
            log.info("ExamRadar Official Source Collector CLI started with --all.");
            List<Source> sources = sourceRepository.findByIsEnabledTrue();
            log.info("Running ingestion for all {} enabled sources...", sources.size());
            for (Source s : sources) {
                try {
                    ingestionEngine.runIngestionForSource(s.getId());
                } catch (Exception e) {
                    log.error("Failed to ingest source [{}]: {}", s.getId(), e.getMessage());
                }
            }

            if (fcmPushService != null) {
                try {
                    int sent = fcmPushService.processDueNotificationJobs();
                    if (sent > 0) {
                        log.info("Dispatched {} queued push notification jobs.", sent);
                    }
                } catch (Exception e) {
                    log.warn("Error processing push notification queue: {}", e.getMessage());
                }
            }

            log.info("ExamRadar Ingestion run complete.");
        } else if (targetSource != null) {
            log.info("ExamRadar Official Source Collector CLI started for target source: [{}]", targetSource);
            try {
                ingestionEngine.runIngestionForSource(targetSource);
            } catch (Exception e) {
                log.error("Failed to ingest target source [{}]: {}", targetSource, e.getMessage());
            }

            if (fcmPushService != null) {
                try {
                    int sent = fcmPushService.processDueNotificationJobs();
                    if (sent > 0) {
                        log.info("Dispatched {} queued push notification jobs.", sent);
                    }
                } catch (Exception e) {
                    log.warn("Error processing push notification queue: {}", e.getMessage());
                }
            }

            log.info("ExamRadar Ingestion run complete.");
        } else {
            // Invoked without CLI flags (e.g., normal web server startup) - skip CLI action
            log.debug("No collector CLI arguments provided (--all or --source=<id>). CollectorCliRunner standby.");
        }
    }
}
