package com.examradar.backend.collector;

import com.examradar.backend.domain.Source;
import com.examradar.backend.repository.SourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Orchestrates the official exam notice ingestion pipeline across registered source adapters.
 * Separates external network I/O from database transactions to prevent HikariCP connection starvation.
 */
@Service
public class IngestionEngine {

    private static final Logger log = LoggerFactory.getLogger(IngestionEngine.class);

    private final Map<String, SourceAdapter> adapters;
    private final SourceRepository sourceRepository;
    private final IngestionPersistenceService persistenceService;

    @Autowired
    public IngestionEngine(
            List<SourceAdapter> adapterList,
            SourceRepository sourceRepository,
            IngestionPersistenceService persistenceService
    ) {
        this.adapters = adapterList.stream().collect(Collectors.toMap(SourceAdapter::getSourceId, a -> a));
        this.sourceRepository = sourceRepository;
        this.persistenceService = persistenceService;
    }

    /**
     * Backwards-compatible constructor for testing environments.
     */
    public IngestionEngine(
            List<SourceAdapter> adapterList,
            SourceRepository sourceRepository,
            com.examradar.backend.repository.SourceFetchRunRepository fetchRunRepository,
            com.examradar.backend.repository.SourceDocumentRepository documentRepository,
            com.examradar.backend.repository.NoticeRepository noticeRepository,
            com.examradar.backend.repository.NoticeVersionRepository versionRepository,
            com.examradar.backend.repository.NoticeDateRepository dateRepository,
            com.examradar.backend.repository.ExamRepository examRepository,
            com.examradar.backend.repository.SourceHealthSnapshotRepository healthSnapshotRepository,
            com.examradar.backend.notification.NotificationDispatcherService notificationDispatcherService,
            com.examradar.backend.notification.DeadlineReminderScheduler deadlineReminderScheduler
    ) {
        this(
                adapterList,
                sourceRepository,
                new IngestionPersistenceService(
                        sourceRepository,
                        fetchRunRepository,
                        documentRepository,
                        noticeRepository,
                        versionRepository,
                        dateRepository,
                        examRepository,
                        healthSnapshotRepository,
                        notificationDispatcherService,
                        deadlineReminderScheduler
                )
        );
    }

    /**
     * Executes official source ingestion without holding a long-running database transaction.
     * Network fetching is executed non-transactionally; persistence is isolated to IngestionPersistenceService.
     */
    public IngestionSummary runIngestionForSource(String sourceId) {
        log.info("Starting source ingestion for [{}]", sourceId);
        Optional<Source> sourceOpt = sourceRepository.findById(sourceId);
        if (sourceOpt.isEmpty()) {
            log.warn("Source [{}] not found in registry", sourceId);
            return new IngestionSummary(sourceId, false, 0, 0, "Source not registered");
        }

        Source source = sourceOpt.get();
        if (!source.getIsEnabled()) {
            log.info("Source [{}] is disabled. Skipping.", sourceId);
            return new IngestionSummary(sourceId, true, 0, 0, "Source is disabled");
        }

        SourceAdapter adapter = adapters.get(sourceId);
        if (adapter == null) {
            log.warn("No adapter implemented for [{}]", sourceId);
            return new IngestionSummary(sourceId, false, 0, 0, "No adapter registered");
        }

        // --- NETWORK I/O EXECUTED OUTSIDE ANY TRANSACTION ---
        source.setLastAttemptedFetch(OffsetDateTime.now());
        ExtractionResult result = adapter.fetchAndExtract(source, source.getLastSuccessfulContentHash());

        if (!result.isSuccessful()) {
            persistenceService.recordFailedRun(source, result);
            log.error("Ingestion failed for [{}]: {}", sourceId, result.errorMessage());
            return new IngestionSummary(sourceId, false, 0, 0, result.errorMessage());
        }

        if (!result.contentChanged()) {
            persistenceService.recordUnchangedRun(source, result);
            log.info("Source content unchanged for [{}] (SHA-256 match).", sourceId);
            return new IngestionSummary(sourceId, true, 0, 0, "Content unchanged");
        }

        // --- SHORT-LIVED TRANSACTION EXCLUSIVELY FOR PERSISTENCE ---
        return persistenceService.persistSuccessfulIngestion(source, result);
    }

    public record IngestionSummary(
        String sourceId,
        boolean isSuccess,
        int newNotices,
        int revisedNotices,
        String message
    ) {}
}
