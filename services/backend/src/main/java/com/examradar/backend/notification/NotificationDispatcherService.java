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
import java.util.*;
import java.util.stream.Collectors;

/**
 * Coordinates notification generation when notices are extracted, revised, or published.
 * Enforces user preferences, exam following rules, and strict idempotency using batch database queries.
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
     * Uses batch queries to eliminate N+1 roundtrips and generates deterministic idempotency keys.
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

        if (preferences.isEmpty()) {
            log.info("No candidate preferences found following exam [{}] for event [{}]", examId, eventType);
            return List.of();
        }

        // 1. Batch collect all unique candidate users
        List<UserProfile> candidateUsers = preferences.stream()
                .map(UserExamPreference::getUser)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (candidateUsers.isEmpty()) {
            return List.of();
        }

        List<UUID> userIds = candidateUsers.stream().map(UserProfile::getId).toList();

        // 2. Batch fetch global notification preferences in a single query
        Set<UUID> pushDisabledUserIds = notificationPreferenceRepository.findByUserIdIn(userIds).stream()
                .filter(p -> Boolean.FALSE.equals(p.getPushEnabled()))
                .map(p -> p.getUser().getId())
                .collect(Collectors.toSet());

        // 3. Filter eligible users and compute idempotency keys
        Map<String, UserProfile> userByIdempotencyKey = new HashMap<>();
        for (UserProfile user : candidateUsers) {
            if (pushDisabledUserIds.contains(user.getId())) {
                log.debug("Push disabled for user [{}], skipping notification job", user.getId());
                continue;
            }

            String rawKey = user.getId().toString() + ":" + notice.getId().toString() + ":" + eventType.name() + ":v" + versionNumber;
            String idempotencyKey = PoliteHttpClient.computeSha256(rawKey);
            userByIdempotencyKey.put(idempotencyKey, user);
        }

        if (userByIdempotencyKey.isEmpty()) {
            log.info("All candidates have push disabled for exam [{}] notice [{}]", examId, notice.getId());
            return List.of();
        }

        // 4. Batch query existing jobs by idempotency keys in a single query
        Set<String> existingKeys = notificationJobRepository.findByIdempotencyKeyIn(userByIdempotencyKey.keySet()).stream()
                .map(NotificationJob::getIdempotencyKey)
                .collect(Collectors.toSet());

        // 5. Construct jobs for newly eligible keys
        List<NotificationJob> jobsToSave = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();
        for (Map.Entry<String, UserProfile> entry : userByIdempotencyKey.entrySet()) {
            String key = entry.getKey();
            if (!existingKeys.contains(key)) {
                jobsToSave.add(new NotificationJob(key, entry.getValue(), notice, eventType, now));
            } else {
                log.debug("Idempotent notification job already exists for key [{}]", key);
            }
        }

        // 6. Batch save all created jobs in a single roundtrip
        List<NotificationJob> createdJobs = jobsToSave.isEmpty() ? List.of() : notificationJobRepository.saveAll(jobsToSave);

        // Record high-level audit event
        NotificationEvent event = new NotificationEvent(
                notice,
                eventType.name(),
                String.format("{\"dispatchedCount\": %d, \"version\": %d, \"examId\": \"%s\"}",
                        createdJobs.size(), versionNumber, examId)
        );
        notificationEventRepository.save(event);

        log.info("Batch dispatched {} notification jobs for notice [{}]", createdJobs.size(), notice.getId());
        return createdJobs;
    }
}
