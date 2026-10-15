package com.examradar.backend.collector;

import com.examradar.backend.domain.*;
import com.examradar.backend.domain.enums.NoticeStatus;
import com.examradar.backend.domain.enums.VerificationStatus;
import com.examradar.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class IngestionEngine {

    private static final Logger log = LoggerFactory.getLogger(IngestionEngine.class);

    private final Map<String, SourceAdapter> adapters;
    private final SourceRepository sourceRepository;
    private final SourceFetchRunRepository fetchRunRepository;
    private final SourceDocumentRepository documentRepository;
    private final NoticeRepository noticeRepository;
    private final NoticeVersionRepository versionRepository;
    private final NoticeDateRepository dateRepository;
    private final ExamRepository examRepository;
    private final SourceHealthSnapshotRepository healthSnapshotRepository;

    public IngestionEngine(
            List<SourceAdapter> adapterList,
            SourceRepository sourceRepository,
            SourceFetchRunRepository fetchRunRepository,
            SourceDocumentRepository documentRepository,
            NoticeRepository noticeRepository,
            NoticeVersionRepository versionRepository,
            NoticeDateRepository dateRepository,
            ExamRepository examRepository,
            SourceHealthSnapshotRepository healthSnapshotRepository
    ) {
        this.adapters = adapterList.stream().collect(Collectors.toMap(SourceAdapter::getSourceId, a -> a));
        this.sourceRepository = sourceRepository;
        this.fetchRunRepository = fetchRunRepository;
        this.documentRepository = documentRepository;
        this.noticeRepository = noticeRepository;
        this.versionRepository = versionRepository;
        this.dateRepository = dateRepository;
        this.examRepository = examRepository;
        this.healthSnapshotRepository = healthSnapshotRepository;
    }

    @Transactional
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

        source.setLastAttemptedFetch(OffsetDateTime.now());
        ExtractionResult result = adapter.fetchAndExtract(source, source.getLastSuccessfulContentHash());

        // Record fetch run
        SourceFetchRun run = new SourceFetchRun(
                source,
                result.isSuccessful() ? (result.contentChanged() ? "SUCCESS" : "NO_CHANGE") : "FAILED",
                result.httpStatus(),
                result.responseTimeMs(),
                result.contentHash(),
                result.errorMessage()
        );
        fetchRunRepository.save(run);

        if (!result.isSuccessful()) {
            source.setConsecutiveFailures(source.getConsecutiveFailures() + 1);
            source.setLastErrorMessage(result.errorMessage());
            sourceRepository.save(source);
            log.error("Ingestion failed for [{}]: {}", sourceId, result.errorMessage());
            return new IngestionSummary(sourceId, false, 0, 0, result.errorMessage());
        }

        source.setConsecutiveFailures(0);
        source.setLastSuccessfulFetch(OffsetDateTime.now());
        source.setLastSuccessfulContentHash(result.contentHash());
        sourceRepository.save(source);

        if (!result.contentChanged()) {
            log.info("Source content unchanged for [{}] (SHA-256 match).", sourceId);
            return new IngestionSummary(sourceId, true, 0, 0, "Content unchanged");
        }

        int newNoticesCount = 0;
        int revisedNoticesCount = 0;

        for (ExtractedNoticeDto noticeDto : result.notices()) {
            Optional<Notice> existingNoticeOpt = noticeRepository.findByOfficialNoticeUrl(noticeDto.officialNoticeUrl());

            if (existingNoticeOpt.isEmpty()) {
                // New Notice
                Optional<Exam> examOpt = examRepository.findById(noticeDto.examId());
                Exam exam = examOpt.orElseGet(() -> examRepository.findAll().stream().findFirst().orElse(null));
                if (exam == null) continue;

                // Validate Temporal Constraints & Quarantine
                NoticeStatus initialStatus = validateAndDetermineStatus(noticeDto);
                VerificationStatus initialVerification = initialStatus == NoticeStatus.VALIDATION_REQUIRED ?
                        VerificationStatus.QUARANTINED : VerificationStatus.VERIFIED;

                Notice notice = new Notice(
                        exam,
                        source,
                        noticeDto.noticeType(),
                        noticeDto.title(),
                        noticeDto.summary(),
                        noticeDto.officialNoticeUrl(),
                        noticeDto.officialApplyUrl(),
                        initialStatus,
                        noticeDto.publishedDate(),
                        initialVerification
                );
                notice = noticeRepository.save(notice);

                // Save Document Snapshot
                SourceDocument doc = new SourceDocument(
                        source, run, noticeDto.officialNoticeUrl(), "HTML",
                        noticeDto.contentHash(), noticeDto.summary()
                );
                doc = documentRepository.save(doc);

                // Save Initial Notice Version (v1)
                NoticeVersion version = new NoticeVersion(
                        notice, 1, doc, noticeDto.contentHash(),
                        "Initial detected publication", "{}"
                );
                versionRepository.save(version);

                // Save Dates
                for (ExtractedDateDto dateDto : noticeDto.importantDates()) {
                    NoticeDate noticeDate = new NoticeDate(
                            notice, dateDto.dateType(), dateDto.eventDate(),
                            dateDto.isTentative(), dateDto.isRevised(),
                            dateDto.originalDateStr(), dateDto.sourceTextSnippet()
                    );
                    dateRepository.save(noticeDate);
                }

                newNoticesCount++;
            } else {
                // Notice Exists: Check for revisions
                Notice existingNotice = existingNoticeOpt.get();
                List<NoticeVersion> versions = versionRepository.findByNoticeIdOrderByVersionNumberDesc(existingNotice.getId());
                NoticeVersion latestVersion = versions.isEmpty() ? null : versions.get(0);

                if (latestVersion == null || !latestVersion.getContentHash().equals(noticeDto.contentHash())) {
                    int nextVersion = (latestVersion == null ? 0 : latestVersion.getVersionNumber()) + 1;

                    SourceDocument doc = new SourceDocument(
                            source, run, noticeDto.officialNoticeUrl(), "HTML",
                            noticeDto.contentHash(), noticeDto.summary()
                    );
                    doc = documentRepository.save(doc);

                    NoticeVersion newVer = new NoticeVersion(
                            existingNotice, nextVersion, doc, noticeDto.contentHash(),
                            "Revised official announcement", "{}"
                    );
                    versionRepository.save(newVer);

                    existingNotice.setTitle(noticeDto.title());
                    existingNotice.setSummary(noticeDto.summary());
                    existingNotice.setUpdatedAt(OffsetDateTime.now());
                    noticeRepository.save(existingNotice);

                    revisedNoticesCount++;
                }
            }
        }

        // Record Health Snapshot
        SourceHealthSnapshot snapshot = new SourceHealthSnapshot(
                source, 1, 1, 0, result.responseTimeMs(), 0
        );
        healthSnapshotRepository.save(snapshot);

        log.info("Ingestion completed for [{}]: {} new notices, {} revised.", sourceId, newNoticesCount, revisedNoticesCount);
        return new IngestionSummary(sourceId, true, newNoticesCount, revisedNoticesCount, "Success");
    }

    private NoticeStatus validateAndDetermineStatus(ExtractedNoticeDto noticeDto) {
        LocalDate openDate = null;
        LocalDate closeDate = null;

        for (ExtractedDateDto d : noticeDto.importantDates()) {
            if (d.dateType() == com.examradar.backend.domain.enums.DateType.APPLICATION_START) {
                openDate = d.eventDate();
            }
            if (d.dateType() == com.examradar.backend.domain.enums.DateType.APPLICATION_DEADLINE) {
                closeDate = d.eventDate();
            }
        }

        // Temporal Consistency Check: deadline must not precede opening date
        if (openDate != null && closeDate != null && closeDate.isBefore(openDate)) {
            log.warn("Quarantining notice [{}]: deadline {} precedes opening date {}", noticeDto.title(), closeDate, openDate);
            return NoticeStatus.VALIDATION_REQUIRED;
        }

        if (noticeDto.parserConfidence() < 0.70) {
            return NoticeStatus.VALIDATION_REQUIRED;
        }

        return NoticeStatus.PUBLISHED;
    }

    public record IngestionSummary(
        String sourceId,
        boolean isSuccess,
        int newNotices,
        int revisedNotices,
        String message
    ) {}
}
