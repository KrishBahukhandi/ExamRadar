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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Calculates and schedules 7-day, 3-day, and 1-day application deadline reminders
 * for candidate users. Enforces interval preferences and cancels obsolete reminder jobs
 * when official deadlines are revised or extended.
 */
@Service
public class DeadlineReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(DeadlineReminderScheduler.class);
    private static final ZoneOffset IST_OFFSET = ZoneOffset.ofHoursMinutes(5, 30);

    private final UserExamPreferenceRepository userExamPreferenceRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final NotificationJobRepository notificationJobRepository;

    public DeadlineReminderScheduler(
            UserExamPreferenceRepository userExamPreferenceRepository,
            NotificationPreferenceRepository notificationPreferenceRepository,
            NotificationJobRepository notificationJobRepository
    ) {
        this.userExamPreferenceRepository = userExamPreferenceRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.notificationJobRepository = notificationJobRepository;
    }

    /**
     * Schedules deadline reminders for all interested users who follow the exam.
     */
    @Transactional
    public List<NotificationJob> scheduleDeadlineReminders(Notice notice, LocalDate deadlineDate) {
        if (notice == null || notice.getExam() == null || deadlineDate == null) {
            return List.of();
        }

        String examId = notice.getExam().getId();
        List<UserExamPreference> candidates = userExamPreferenceRepository.findByExamIdAndNotifyDeadlinesTrue(examId);
        List<NotificationJob> scheduledJobs = new ArrayList<>();

        OffsetDateTime now = OffsetDateTime.now();

        for (UserExamPreference pref : candidates) {
            UserProfile user = pref.getUser();
            if (user == null) continue;

            Optional<NotificationPreference> notifPrefOpt = notificationPreferenceRepository.findByUserId(user.getId());
            NotificationPreference notifPref = notifPrefOpt.orElse(null);

            if (notifPref != null && !notifPref.getPushEnabled()) {
                continue;
            }

            boolean want7d = notifPref == null || notifPref.getReminder7Days();
            boolean want3d = notifPref == null || notifPref.getReminder3Days();
            boolean want1d = notifPref == null || notifPref.getReminder1Day();

            // 7 Days Reminder
            if (want7d) {
                scheduleSingleReminder(notice, user, deadlineDate, 7, NotificationEventType.DEADLINE_7D, now)
                        .ifPresent(scheduledJobs::add);
            }

            // 3 Days Reminder
            if (want3d) {
                scheduleSingleReminder(notice, user, deadlineDate, 3, NotificationEventType.DEADLINE_3D, now)
                        .ifPresent(scheduledJobs::add);
            }

            // 1 Day Reminder
            if (want1d) {
                scheduleSingleReminder(notice, user, deadlineDate, 1, NotificationEventType.DEADLINE_1D, now)
                        .ifPresent(scheduledJobs::add);
            }
        }

        log.info("Scheduled {} deadline reminder jobs for notice [{}] (deadline: {})",
                scheduledJobs.size(), notice.getId(), deadlineDate);
        return scheduledJobs;
    }

    private Optional<NotificationJob> scheduleSingleReminder(
            Notice notice, UserProfile user, LocalDate deadline, int daysBefore,
            NotificationEventType eventType, OffsetDateTime now
    ) {
        LocalDate reminderDate = deadline.minusDays(daysBefore);
        OffsetDateTime scheduledAt = reminderDate.atTime(9, 0).atOffset(IST_OFFSET);

        // Do not schedule if the target time has already passed
        if (scheduledAt.isBefore(now)) {
            return Optional.empty();
        }

        String rawKey = user.getId() + ":" + notice.getId() + ":" + eventType.name() + ":" + deadline;
        String idempotencyKey = PoliteHttpClient.computeSha256(rawKey);

        if (notificationJobRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            return Optional.empty();
        }

        NotificationJob job = new NotificationJob(idempotencyKey, user, notice, eventType, scheduledAt);
        return Optional.of(notificationJobRepository.save(job));
    }

    /**
     * When an official deadline is revised, cancels obsolete pending reminder jobs
     * and reschedules new reminder jobs for the updated date.
     */
    @Transactional
    public List<NotificationJob> handleDeadlineRevision(Notice notice, LocalDate revisedDeadline) {
        log.info("Handling deadline revision for notice [{}]: new deadline {}", notice.getId(), revisedDeadline);

        // Cancel existing pending deadline reminders for this notice
        List<NotificationJob> existingPending = notificationJobRepository.findByNoticeIdAndStatus(notice.getId(), JobStatus.PENDING);
        int cancelledCount = 0;
        for (NotificationJob job : existingPending) {
            if (job.getEventType() == NotificationEventType.DEADLINE_7D ||
                job.getEventType() == NotificationEventType.DEADLINE_3D ||
                job.getEventType() == NotificationEventType.DEADLINE_1D) {
                job.setStatus(JobStatus.CANCELLED);
                job.setLastError("Cancelled due to official deadline revision to " + revisedDeadline);
                notificationJobRepository.save(job);
                cancelledCount++;
            }
        }
        log.info("Cancelled {} obsolete deadline reminder jobs for notice [{}]", cancelledCount, notice.getId());

        // Schedule new reminder jobs for the revised deadline
        return scheduleDeadlineReminders(notice, revisedDeadline);
    }
}
