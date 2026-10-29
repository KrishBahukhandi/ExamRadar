package com.examradar.backend.collector;

import com.examradar.backend.domain.*;
import com.examradar.backend.domain.enums.DateType;
import com.examradar.backend.domain.enums.NoticeStatus;
import com.examradar.backend.domain.enums.NotificationEventType;
import com.examradar.backend.domain.enums.VerificationStatus;
import com.examradar.backend.repository.*;
import com.examradar.backend.config.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Handles transactional persistence and entity lifecycle operations for the ingestion pipeline.
 * Separated from IngestionEngine to ensure external network scraping is never executed within
 * an active database transaction.
 */
@Service
public class IngestionPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(IngestionPersistenceService.class);

    private final SourceRepository sourceRepository;
    private final SourceFetchRunRepository fetchRunRepository;
    private final SourceDocumentRepository documentRepository;
    private final NoticeRepository noticeRepository;
    private final NoticeVersionRepository versionRepository;
    private final NoticeDateRepository dateRepository;
    private final ExamRepository examRepository;
    private final SourceHealthSnapshotRepository healthSnapshotRepository;
    private final com.examradar.backend.notification.NotificationDispatcherService notificationDispatcherService;
    private final com.examradar.backend.notification.DeadlineReminderScheduler deadlineReminderScheduler;

    public IngestionPersistenceService(
            SourceRepository sourceRepository,
            SourceFetchRunRepository fetchRunRepository,
            SourceDocumentRepository documentRepository,
            NoticeRepository noticeRepository,
            NoticeVersionRepository versionRepository,
            NoticeDateRepository dateRepository,
            ExamRepository examRepository,
            SourceHealthSnapshotRepository healthSnapshotRepository,
            com.examradar.backend.notification.NotificationDispatcherService notificationDispatcherService,
            com.examradar.backend.notification.DeadlineReminderScheduler deadlineReminderScheduler
    ) {
        this.sourceRepository = sourceRepository;
        this.fetchRunRepository = fetchRunRepository;
        this.documentRepository = documentRepository;
        this.noticeRepository = noticeRepository;
        this.versionRepository = versionRepository;
        this.dateRepository = dateRepository;
        this.examRepository = examRepository;
        this.healthSnapshotRepository = healthSnapshotRepository;
        this.notificationDispatcherService = notificationDispatcherService;
        this.deadlineReminderScheduler = deadlineReminderScheduler;
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_SOURCES, allEntries = true)
    public void recordFailedRun(Source source, ExtractionResult result) {
        SourceFetchRun run = new SourceFetchRun(
                source, "FAILED", result.httpStatus(),
                result.responseTimeMs(), result.contentHash(), result.errorMessage()
        );
        fetchRunRepository.save(run);

        source.setConsecutiveFailures(source.getConsecutiveFailures() + 1);
        source.setLastErrorMessage(result.errorMessage());
        sourceRepository.save(source);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_SOURCES, allEntries = true)
    public void recordUnchangedRun(Source source, ExtractionResult result) {
        SourceFetchRun run = new SourceFetchRun(
                source, "NO_CHANGE", result.httpStatus(),
                result.responseTimeMs(), result.contentHash(), null
        );
        fetchRunRepository.save(run);

        source.setConsecutiveFailures(0);
        source.setLastSuccessfulFetch(OffsetDateTime.now());
        source.setLastSuccessfulContentHash(result.contentHash());
        sourceRepository.save(source);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_SOURCES, allEntries = true)
    public IngestionEngine.IngestionSummary persistSuccessfulIngestion(Source source, ExtractionResult result) {
        SourceFetchRun run = new SourceFetchRun(
                source, "SUCCESS", result.httpStatus(),
                result.responseTimeMs(), result.contentHash(), null
        );
        fetchRunRepository.save(run);

        source.setConsecutiveFailures(0);
        source.setLastSuccessfulFetch(OffsetDateTime.now());
        source.setLastSuccessfulContentHash(result.contentHash());
        sourceRepository.save(source);

        int newNoticesCount = 0;
        int revisedNoticesCount = 0;

        for (ExtractedNoticeDto noticeDto : result.notices()) {
            Optional<Notice> existingNoticeOpt = noticeRepository.findByOfficialNoticeUrl(noticeDto.officialNoticeUrl());

            if (existingNoticeOpt.isEmpty()) {
                // New Notice
                Optional<Exam> examOpt = examRepository.findById(noticeDto.examId());
                Exam exam = examOpt.orElseGet(() -> examRepository.findAll().stream().findFirst().orElse(null));
                if (exam == null) continue;

                NoticeStatus initialStatus = validateAndDetermineStatus(noticeDto);
                VerificationStatus initialVerification = initialStatus == NoticeStatus.VALIDATION_REQUIRED ?
                        VerificationStatus.QUARANTINED : VerificationStatus.VERIFIED;

                Notice notice = new Notice(
                        exam, source, noticeDto.noticeType(), noticeDto.title(),
                        noticeDto.summary(), noticeDto.officialNoticeUrl(), noticeDto.officialApplyUrl(),
                        initialStatus, noticeDto.publishedDate(), initialVerification
                );
                notice.setTotalVacancies(noticeDto.totalVacancies());
                notice.setVacancyBreakdownJson(noticeDto.vacancyBreakdownJson());
                notice.setPayScaleSummary(noticeDto.payScaleSummary());
                notice.setApplicationFeeGeneral(noticeDto.applicationFeeGeneral());
                notice.setApplicationFeeReserved(noticeDto.applicationFeeReserved());
                notice.setFeeExemptionDetails(noticeDto.feeExemptionDetails());
                notice.setAgeMin(noticeDto.ageMin());
                notice.setAgeMax(noticeDto.ageMax());
                notice.setAgeCutoffDate(noticeDto.ageCutoffDate());
                notice.setAgeRelaxationSummary(noticeDto.ageRelaxationSummary());
                notice.setEducationalQualification(noticeDto.educationalQualification());
                notice.setTitleHi(noticeDto.titleHi());
                notice.setSummaryHi(noticeDto.summaryHi());
                notice.setExamStages(noticeDto.examStages());
                notice.setNegativeMarking(noticeDto.negativeMarking());
                notice.setExamMode(noticeDto.examMode());
                notice.setSyllabusUrl(noticeDto.syllabusUrl());
                notice = noticeRepository.save(notice);

                SourceDocument doc = new SourceDocument(
                        source, run, noticeDto.officialNoticeUrl(), "HTML",
                        noticeDto.contentHash(), noticeDto.summary()
                );
                doc = documentRepository.save(doc);

                NoticeVersion version = new NoticeVersion(
                        notice, 1, doc, noticeDto.contentHash(),
                        "Initial detected publication", "{}"
                );
                versionRepository.save(version);

                LocalDate appDeadline = null;
                for (ExtractedDateDto dateDto : noticeDto.importantDates()) {
                    NoticeDate noticeDate = new NoticeDate(
                            notice, dateDto.dateType(), dateDto.eventDate(),
                            dateDto.isTentative(), dateDto.isRevised(),
                            dateDto.originalDateStr(), dateDto.sourceTextSnippet()
                    );
                    dateRepository.save(noticeDate);
                    if (dateDto.dateType() == DateType.APPLICATION_DEADLINE) {
                        appDeadline = dateDto.eventDate();
                    }
                }

                if (initialStatus == NoticeStatus.PUBLISHED) {
                    NotificationEventType eventType = noticeDto.noticeType() == com.examradar.backend.domain.enums.NoticeType.RESULT
                            ? NotificationEventType.RESULT_OUT
                            : NotificationEventType.NEW_NOTICE;
                    notificationDispatcherService.dispatchForNotice(notice, eventType, 1);

                    if (appDeadline != null) {
                        deadlineReminderScheduler.scheduleDeadlineReminders(notice, appDeadline);
                    }
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
                    if (noticeDto.totalVacancies() != null) existingNotice.setTotalVacancies(noticeDto.totalVacancies());
                    if (noticeDto.vacancyBreakdownJson() != null) existingNotice.setVacancyBreakdownJson(noticeDto.vacancyBreakdownJson());
                    if (noticeDto.payScaleSummary() != null) existingNotice.setPayScaleSummary(noticeDto.payScaleSummary());
                    if (noticeDto.applicationFeeGeneral() != null) existingNotice.setApplicationFeeGeneral(noticeDto.applicationFeeGeneral());
                    if (noticeDto.applicationFeeReserved() != null) existingNotice.setApplicationFeeReserved(noticeDto.applicationFeeReserved());
                    if (noticeDto.feeExemptionDetails() != null) existingNotice.setFeeExemptionDetails(noticeDto.feeExemptionDetails());
                    if (noticeDto.ageMin() != null) existingNotice.setAgeMin(noticeDto.ageMin());
                    if (noticeDto.ageMax() != null) existingNotice.setAgeMax(noticeDto.ageMax());
                    if (noticeDto.ageCutoffDate() != null) existingNotice.setAgeCutoffDate(noticeDto.ageCutoffDate());
                    if (noticeDto.ageRelaxationSummary() != null) existingNotice.setAgeRelaxationSummary(noticeDto.ageRelaxationSummary());
                    if (noticeDto.educationalQualification() != null) existingNotice.setEducationalQualification(noticeDto.educationalQualification());
                    existingNotice.setHasCorrigendum(true);
                    existingNotice.setCorrigendumSummary("Corrigendum #" + (nextVersion - 1) + ": Official amendment and schedule update");
                    existingNotice.setUpdatedAt(OffsetDateTime.now());
                    noticeRepository.save(existingNotice);

                    LocalDate revisedDeadline = null;
                    for (ExtractedDateDto dateDto : noticeDto.importantDates()) {
                        if (dateDto.dateType() == DateType.APPLICATION_DEADLINE) {
                            revisedDeadline = dateDto.eventDate();
                            break;
                        }
                    }
                    if (revisedDeadline != null) {
                        deadlineReminderScheduler.handleDeadlineRevision(existingNotice, revisedDeadline);
                    }
                    notificationDispatcherService.dispatchForNotice(existingNotice, NotificationEventType.DEADLINE_REVISED, nextVersion);

                    revisedNoticesCount++;
                }
            }
        }

        SourceHealthSnapshot snapshot = new SourceHealthSnapshot(
                source, 1, 1, 0, result.responseTimeMs(), 0
        );
        healthSnapshotRepository.save(snapshot);

        log.info("Ingestion completed for [{}]: {} new notices, {} revised.", source.getId(), newNoticesCount, revisedNoticesCount);
        return new IngestionEngine.IngestionSummary(source.getId(), true, newNoticesCount, revisedNoticesCount, "Success");
    }

    private NoticeStatus validateAndDetermineStatus(ExtractedNoticeDto noticeDto) {
        LocalDate openDate = null;
        LocalDate closeDate = null;

        for (ExtractedDateDto d : noticeDto.importantDates()) {
            if (d.dateType() == DateType.APPLICATION_START) {
                openDate = d.eventDate();
            }
            if (d.dateType() == DateType.APPLICATION_DEADLINE) {
                closeDate = d.eventDate();
            }
        }

        if (openDate != null && closeDate != null && closeDate.isBefore(openDate)) {
            log.warn("Quarantining notice [{}]: deadline {} precedes opening date {}", noticeDto.title(), closeDate, openDate);
            return NoticeStatus.VALIDATION_REQUIRED;
        }

        if (noticeDto.parserConfidence() < 0.70) {
            return NoticeStatus.VALIDATION_REQUIRED;
        }

        return NoticeStatus.PUBLISHED;
    }
}
