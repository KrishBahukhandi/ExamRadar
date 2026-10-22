package com.examradar.backend.service;

import com.examradar.backend.collector.IngestionEngine;
import com.examradar.backend.domain.NotificationJob;
import com.examradar.backend.domain.ParserFailure;
import com.examradar.backend.domain.SourceFetchRun;
import com.examradar.backend.dto.SourceHealthDto;
import com.examradar.backend.notification.FcmPushService;
import com.examradar.backend.repository.NotificationJobRepository;
import com.examradar.backend.repository.ParserFailureRepository;
import com.examradar.backend.repository.SourceFetchRunRepository;
import com.examradar.backend.repository.SourceRepository;
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

    public AdminService(
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
}
