package com.examradar.backend.notification;

import com.examradar.backend.collector.PoliteHttpClient;
import com.examradar.backend.domain.*;
import com.examradar.backend.domain.enums.JobStatus;
import com.examradar.backend.domain.enums.NotificationEventType;
import com.examradar.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Coordinates notification generation when notices are extracted, revised, or published.
 * Enforces user preferences, exam following rules, and strict idempotency.
 */
@Service
public class NotificationDispatcherService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcherService.class);

    private final UserExamPreferenceRepository userExamPreferenceRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final NotificationJobRepository notificationJobRepository;
    private final NotificationEventRepository notificationEventRepository;

    public NotificationDispatcherService(
            UserExamPreferenceRepository userExamPreferenceRepository,
            NotificationPreferenceRepository notificationPreferenceRepository,
            NotificationJobRepository notificationJobRepository,
            NotificationEventRepository notificationEventRepository
    ) {
        this.userExamPreferenceRepository = userExamPreferenceRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.notificationJobRepository = notificationJobRepository;
        this.notificationEventRepository = notificationEventRepository;
    }

    /**
     * Dispatches notification jobs for users following the exam associated with this notice.
     * Generates a deterministic idempotency key to prevent duplicate delivery.
     */
    @Transactional
    public List<NotificationJob> dispatchForNotice(Notice notice, NotificationEventType eventType, int versionNumber) {
        if (notice == null || notice.getExam() == null) {
            log.warn("Cannot dispatch notification: notice or associated exam is null");
            return List.of();
        }

        String examId = notice.getExam().getId();
        log.info("Evaluating notification recipients for notice [{}] ({}) of exam [{}]",
                notice.getId(), eventType, examId);

        // Find users following this exam
        List<UserExamPreference> preferences;
        if (eventType == NotificationEventType.RESULT_OUT) {
            preferences = userExamPreferenceRepository.findByExamIdAndNotifyNewNoticesTrue(examId).stream()
                    .filter(UserExamPreference::getNotifyResults)
                    .toList();
        } else {
            preferences = userExamPreferenceRepository.findByExamIdAndNotifyNewNoticesTrue(examId);
        }

        List<NotificationJob> createdJobs = new ArrayList<>();

        for (UserExamPreference pref : preferences) {
            UserProfile user = pref.getUser();
            if (user == null) continue;

            // Check global notification preference
            Optional<NotificationPreference> userNotifPref = notificationPreferenceRepository.findByUserId(user.getId());
            if (userNotifPref.isPresent() && !userNotifPref.get().getPushEnabled()) {
                log.debug("Push disabled for user [{}], skipping notification job", user.getId());
                continue;
            }

            // Generate deterministic idempotency key: sha256(userId + ":" + noticeId + ":" + eventType + ":v" + versionNumber)
            String rawKey = user.getId().toString() + ":" + notice.getId().toString() + ":" + eventType.name() + ":v" + versionNumber;
            String idempotencyKey = PoliteHttpClient.computeSha256(rawKey);

            // Verify idempotency
            if (notificationJobRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
                log.debug("Idempotent notification job already exists for key [{}]", idempotencyKey);
                continue;
            }

            NotificationJob job = new NotificationJob(
                    idempotencyKey,
                    user,
                    notice,
                    eventType,
                    OffsetDateTime.now()
            );

            NotificationJob savedJob = notificationJobRepository.save(job);
            createdJobs.add(savedJob);
        }

        // Record high-level audit event
        NotificationEvent event = new NotificationEvent(
                notice,
                eventType.name(),
                String.format("{\"dispatchedCount\": %d, \"version\": %d, \"examId\": \"%s\"}",
                        createdJobs.size(), versionNumber, examId)
        );
        notificationEventRepository.save(event);

        log.info("Dispatched {} notification jobs for notice [{}]", createdJobs.size(), notice.getId());
        return createdJobs;
    }
}
