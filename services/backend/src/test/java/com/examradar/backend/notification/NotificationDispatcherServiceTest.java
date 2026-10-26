package com.examradar.backend.notification;

import com.examradar.backend.domain.*;
import com.examradar.backend.domain.enums.NotificationEventType;
import com.examradar.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherServiceTest {

    @Mock
    private UserExamPreferenceRepository userExamPreferenceRepository;

    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Mock
    private NotificationJobRepository notificationJobRepository;

    @Mock
    private NotificationEventRepository notificationEventRepository;

    private NotificationDispatcherService dispatcherService;

    @BeforeEach
    void setUp() {
        dispatcherService = new NotificationDispatcherService(
                userExamPreferenceRepository,
                notificationPreferenceRepository,
                notificationJobRepository,
                notificationEventRepository
        );
    }

    @Test
    void testDispatchForNoticeGeneratesIdempotentJobs() {
        ExamCategory category = new ExamCategory("cat-banking", "Banking", "banking", "desc", 1);
        Exam exam = new Exam("exam-ibps-po", category, "IBPS", "IBPS PO", "PO", "https://ibps.in", "desc", true);
        Source source = new Source("src-ibps", "IBPS", "IBPS", "https://ibps.in", null, null, 60, "1.0", null);
        Notice notice = new Notice(exam, source, null, "IBPS PO Notification 2026", "Summary",
                "https://ibps.in/po", null, null, null, null);
        notice.setId(UUID.randomUUID());

        UserProfile user1 = new UserProfile(UUID.randomUUID(), "candidate1@example.com", "Candidate One");
        UserProfile user2 = new UserProfile(UUID.randomUUID(), "candidate2@example.com", "Candidate Two");

        UserExamPreference pref1 = new UserExamPreference(user1, exam, category);
        UserExamPreference pref2 = new UserExamPreference(user2, exam, category);

        when(userExamPreferenceRepository.findByExamIdAndNotifyNewNoticesTrue("exam-ibps-po"))
                .thenReturn(List.of(pref1, pref2));

        when(notificationPreferenceRepository.findByUserIdIn(any()))
                .thenReturn(List.of()); // Defaults to push enabled for all

        when(notificationJobRepository.findByIdempotencyKeyIn(any()))
                .thenReturn(List.of()); // No existing jobs

        when(notificationJobRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<NotificationJob> jobs = dispatcherService.dispatchForNotice(notice, NotificationEventType.NEW_NOTICE, 1);

        assertEquals(2, jobs.size());
        verify(notificationJobRepository, times(1)).saveAll(any());
        verify(notificationEventRepository, times(1)).save(any(NotificationEvent.class));
    }

    @Test
    void testDispatchRespectsPushDisabledPreference() {
        ExamCategory category = new ExamCategory("cat-banking", "Banking", "banking", "desc", 1);
        Exam exam = new Exam("exam-ibps-po", category, "IBPS", "IBPS PO", "PO", "https://ibps.in", "desc", true);
        Source source = new Source("src-ibps", "IBPS", "IBPS", "https://ibps.in", null, null, 60, "1.0", null);
        Notice notice = new Notice(exam, source, null, "IBPS PO Notification 2026", "Summary",
                "https://ibps.in/po", null, null, null, null);
        notice.setId(UUID.randomUUID());

        UserProfile user = new UserProfile(UUID.randomUUID(), "candidate@example.com", "Candidate");
        UserExamPreference pref = new UserExamPreference(user, exam, category);

        NotificationPreference notifPref = new NotificationPreference(user);
        notifPref.setPushEnabled(false); // Push disabled!

        when(userExamPreferenceRepository.findByExamIdAndNotifyNewNoticesTrue("exam-ibps-po"))
                .thenReturn(List.of(pref));
        when(notificationPreferenceRepository.findByUserIdIn(any()))
                .thenReturn(List.of(notifPref));

        List<NotificationJob> jobs = dispatcherService.dispatchForNotice(notice, NotificationEventType.NEW_NOTICE, 1);

        assertTrue(jobs.isEmpty(), "Should not create jobs for users with push disabled");
        verify(notificationJobRepository, never()).saveAll(any());
    }
}
