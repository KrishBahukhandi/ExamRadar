package com.examradar.backend.notification;

import com.examradar.backend.domain.*;
import com.examradar.backend.domain.enums.JobStatus;
import com.examradar.backend.domain.enums.NotificationEventType;
import com.examradar.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeadlineReminderSchedulerTest {

    @Mock
    private UserExamPreferenceRepository userExamPreferenceRepository;

    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Mock
    private NotificationJobRepository notificationJobRepository;

    private DeadlineReminderScheduler reminderScheduler;

    @BeforeEach
    void setUp() {
        reminderScheduler = new DeadlineReminderScheduler(
                userExamPreferenceRepository,
                notificationPreferenceRepository,
                notificationJobRepository
        );
    }

    @Test
    void testScheduleDeadlineRemindersCreates7d3d1dJobs() {
        ExamCategory category = new ExamCategory("cat-ssc", "SSC", "ssc", "desc", 2);
        Exam exam = new Exam("exam-ssc-cgl", category, "SSC", "SSC CGL", "CGL", "https://ssc.gov.in", "desc", true);
        Notice notice = new Notice(exam, null, null, "SSC CGL Notification 2026", "Summary",
                "https://ssc.gov.in/cgl", null, null, null, null);
        notice.setId(UUID.randomUUID());

        UserProfile user = new UserProfile(UUID.randomUUID(), "candidate@example.com", "Candidate");
        UserExamPreference pref = new UserExamPreference(user, exam, category);

        when(userExamPreferenceRepository.findByExamIdAndNotifyDeadlinesTrue("exam-ssc-cgl"))
                .thenReturn(List.of(pref));

        when(notificationPreferenceRepository.findByUserId(user.getId()))
                .thenReturn(Optional.empty()); // default preferences: all 7d, 3d, 1d enabled

        when(notificationJobRepository.findByIdempotencyKey(anyString()))
                .thenReturn(Optional.empty());

        when(notificationJobRepository.save(any(NotificationJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Deadline 30 days in the future so 7d, 3d, 1d are all in the future
        LocalDate futureDeadline = LocalDate.now().plusDays(30);

        List<NotificationJob> jobs = reminderScheduler.scheduleDeadlineReminders(notice, futureDeadline);

        assertEquals(3, jobs.size());
        assertTrue(jobs.stream().anyMatch(j -> j.getEventType() == NotificationEventType.DEADLINE_7D));
        assertTrue(jobs.stream().anyMatch(j -> j.getEventType() == NotificationEventType.DEADLINE_3D));
        assertTrue(jobs.stream().anyMatch(j -> j.getEventType() == NotificationEventType.DEADLINE_1D));
    }

    @Test
    void testHandleDeadlineRevisionCancelsObsoleteJobs() {
        ExamCategory category = new ExamCategory("cat-ssc", "SSC", "ssc", "desc", 2);
        Exam exam = new Exam("exam-ssc-cgl", category, "SSC", "SSC CGL", "CGL", "https://ssc.gov.in", "desc", true);
        Notice notice = new Notice(exam, null, null, "SSC CGL Notification 2026", "Summary",
                "https://ssc.gov.in/cgl", null, null, null, null);
        UUID noticeId = UUID.randomUUID();
        notice.setId(noticeId);

        UserProfile user = new UserProfile(UUID.randomUUID(), "candidate@example.com", "Candidate");

        NotificationJob obsoleteJob = new NotificationJob(
                "key-old", user, notice, NotificationEventType.DEADLINE_7D, OffsetDateTime.now().plusDays(10)
        );

        when(notificationJobRepository.findByNoticeIdAndStatus(noticeId, JobStatus.PENDING))
                .thenReturn(List.of(obsoleteJob));

        LocalDate revisedDeadline = LocalDate.now().plusDays(40);

        reminderScheduler.handleDeadlineRevision(notice, revisedDeadline);

        assertEquals(JobStatus.CANCELLED, obsoleteJob.getStatus());
        assertTrue(obsoleteJob.getLastError().contains("Cancelled due to official deadline revision"));
        verify(notificationJobRepository, atLeastOnce()).save(obsoleteJob);
    }
}
