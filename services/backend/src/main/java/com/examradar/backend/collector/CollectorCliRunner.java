package com.examradar.backend.collector;

import com.examradar.backend.domain.Source;
import com.examradar.backend.repository.SourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("!test")
public class CollectorCliRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CollectorCliRunner.class);

    private final IngestionEngine ingestionEngine;
    private final SourceRepository sourceRepository;

    public CollectorCliRunner(IngestionEngine ingestionEngine, SourceRepository sourceRepository) {
        this.ingestionEngine = ingestionEngine;
        this.sourceRepository = sourceRepository;
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(CollectorCliRunner.class, args);
        int exitCode = SpringApplication.exit(ctx, () -> 0);
        System.exit(exitCode);
    }

    @Override
    public void run(String... args) {
        log.info("ExamRadar Official Source Collector CLI started.");

        String targetSource = null;
        boolean runAll = false;

        for (String arg : args) {
            if ("--all".equalsIgnoreCase(arg)) {
                runAll = true;
            } else if (arg.startsWith("--source=")) {
                targetSource = arg.substring("--source=".length());
            }
        }

        if (runAll || targetSource == null) {
            List<Source> sources = sourceRepository.findByIsEnabledTrue();
            log.info("Running ingestion for all {} enabled sources...", sources.size());
            for (Source s : sources) {
                try {
                    ingestionEngine.runIngestionForSource(s.getId());
                } catch (Exception e) {
                    log.error("Failed to ingest source [{}]: {}", s.getId(), e.getMessage());
                }
            }
        } else {
            log.info("Running ingestion for target source: [{}]", targetSource);
            try {
                ingestionEngine.runIngestionForSource(targetSource);
            } catch (Exception e) {
                log.error("Failed to ingest target source [{}]: {}", targetSource, e.getMessage());
            }
        }

        log.info("ExamRadar Ingestion run complete.");
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelayString = "\${examradar.collector.poll-interval-ms:7200000}", initialDelay = 7200000)
    public void runPeriodicIngestion() {
        log.info("Running periodic ingestion cycle for all enabled sources...");
        List<Source> sources = sourceRepository.findByIsEnabledTrue();
        for (Source s : sources) {
            try {
                ingestionEngine.runIngestionForSource(s.getId());
            } catch (Exception e) {
                log.error("Failed periodic ingestion for source [{}]: {}", s.getId(), e.getMessage());
            }
        }
    }
}
