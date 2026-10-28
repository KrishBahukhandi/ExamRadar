package com.examradar.backend.e2e;

import com.examradar.backend.collector.ExtractedDateDto;
import com.examradar.backend.collector.ExtractedNoticeDto;
import com.examradar.backend.collector.ExtractionResult;
import com.examradar.backend.collector.IngestionEngine;
import com.examradar.backend.collector.IngestionPersistenceService;
import com.examradar.backend.domain.*;
import com.examradar.backend.domain.enums.*;
import com.examradar.backend.dto.CalendarItemDto;
import com.examradar.backend.notification.FcmPushService;
import com.examradar.backend.notification.NotificationDispatcherService;
import com.examradar.backend.repository.*;
import com.examradar.backend.service.CalendarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration simulation verifying the entire pipeline:
 * Official Scraper Payload -> IngestionEngine & Persistence -> DB Persistence ->
 * NotificationDispatcher Fanout -> FCM Queue Processing -> Delivery Logs & Calendar Event Timeline.
 */
@SpringBootTest
class EndToEndIngestionAndPushNotificationSimulationTest {

    @Autowired
    private IngestionPersistenceService persistenceService;

    @Autowired
    private NotificationDispatcherService dispatcherService;

    @Autowired
    private FcmPushService fcmPushService;

    @Autowired
    private CalendarService calendarService;

    @Autowired
    private ExamCategoryRepository categoryRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private NoticeDateRepository noticeDateRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserExamPreferenceRepository preferenceRepository;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private NotificationJobRepository notificationJobRepository;

    @Autowired
    private NotificationDeliveryRepository deliveryRepository;

    private Exam testExam;
    private Source testSource;
    private UserProfile candidateRohan;
    private UserProfile candidatePriya;

    @BeforeEach
    void setUp() {
        deliveryRepository.deleteAll();
        notificationJobRepository.deleteAll();
        noticeDateRepository.deleteAll();
        noticeRepository.deleteAll();

        ExamCategory category = categoryRepository.save(
                new ExamCategory("cat-e2e-upsc", "UPSC", "upsc-e2e", "Central Civil Services", 1)
        );

        testExam = examRepository.save(
                new Exam("exam-e2e-cse", category, "UPSC", "Civil Services Examination 2026", "CSE-2026", "https://upsc.gov.in", "IAS/IPS")
        );

        testSource = sourceRepository.save(
                new Source("src-upsc-e2e", "UPSC", "UPSC Official Notices", "https://upsc.gov.in/whats-new",
                        SourceType.HTML_SCRAPE, FetchMethod.HTTP_GET, 60, "1.0", "Public Staging")
        );

        // Candidate 1 (Rohan) follows Civil Services Exam with active Android device token
        candidateRohan = userProfileRepository.save(
                new UserProfile(UUID.randomUUID(), "rohan.sharma@example.in", "Rohan Sharma")
        );
        preferenceRepository.save(new UserExamPreference(candidateRohan, testExam, category));
        deviceTokenRepository.save(new DeviceToken(candidateRohan, "fcm-token-rohan-android", "ANDROID", "1.0.0"));

        // Candidate 2 (Priya) does NOT follow Civil Services Exam (or push disabled)
        candidatePriya = userProfileRepository.save(
                new UserProfile(UUID.randomUUID(), "priya.verma@example.in", "Priya Verma")
        );
    }

    @Test
    void testFullPipelineFromScrapedPayloadToFcmDeliveryAndCalendar() {
        // 1. Simulate Scraped Payload from UPSC Official Portal
        ExtractedNoticeDto scrapedNotice = new ExtractedNoticeDto(
                "exam-e2e-cse",
                testSource.getId(),
                NoticeType.RECRUITMENT,
                "Civil Services (Preliminary) Examination, 2026 - Notification Announced",
                "UPSC has officially released the notification for CSE 2026 for recruitment to IAS, IPS, and IFS.",
                "https://upsc.gov.in/sites/default/files/Notice-CSPE-2026-E2E.pdf",
                "https://upsconline.nic.in",
                NoticeStatus.PUBLISHED,
                VerificationStatus.VERIFIED,
                LocalDate.of(2026, 2, 14),
                List.of(
                        new ExtractedDateDto(DateType.APPLICATION_START, LocalDate.of(2026, 2, 14), false, false, "14-02-2026", "Starts 14 Feb", 1.0),
                        new ExtractedDateDto(DateType.APPLICATION_DEADLINE, LocalDate.of(2026, 3, 5), false, false, "05-03-2026", "Last Date 05 March", 1.0),
                        new ExtractedDateDto(DateType.EXAM_DATE, LocalDate.of(2026, 5, 24), false, false, "24-05-2026", "Prelims Exam Date", 1.0)
                ),
                "Official Notification Text",
                "sha256-hash-cse-2026-payload-v1",
                "Graduate in any discipline",
                0.99
        );

        ExtractionResult extractionResult = ExtractionResult.success(
                testSource.getId(),
                true,
                "sha256-hash-cse-2026-payload-v1",
                200,
                320L,
                List.of(scrapedNotice)
        );

        // 2. Execute Ingestion & Transactional Persistence
        IngestionEngine.IngestionSummary summary = persistenceService.persistSuccessfulIngestion(testSource, extractionResult);
        assertThat(summary.isSuccess()).isTrue();
        assertThat(summary.newNotices()).isEqualTo(1);

        // 3. Verify Database State
        List<Notice> notices = noticeRepository.findAll();
        assertThat(notices).hasSize(1);
        Notice persistedNotice = notices.get(0);
        assertThat(persistedNotice.getStatus()).isEqualTo(NoticeStatus.PUBLISHED);
        assertThat(persistedNotice.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(persistedNotice.getOfficialNoticeUrl()).contains("Notice-CSPE-2026-E2E.pdf");

        List<NoticeDate> dates = noticeDateRepository.findByNoticeId(persistedNotice.getId());
        assertThat(dates).hasSize(3);

        // 4. Verify Notification Dispatcher queued job for Rohan only
        List<NotificationJob> jobs = notificationJobRepository.findAll();
        assertThat(jobs).hasSize(1);
        NotificationJob rohanJob = jobs.get(0);
        assertThat(rohanJob.getUser().getId()).isEqualTo(candidateRohan.getId());
        assertThat(rohanJob.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(rohanJob.getIdempotencyKey()).hasSize(64);

        // 5. Execute Scheduled FCM Queue Processing
        int sentCount = fcmPushService.processDueNotificationJobs();
        assertThat(sentCount).isEqualTo(1);

        // Verify status transitioned from PENDING to SENT
        NotificationJob sentJob = notificationJobRepository.findById(rohanJob.getId()).orElseThrow();
        assertThat(sentJob.getStatus()).isEqualTo(JobStatus.SENT);
        assertThat(sentJob.getUpdatedAt()).isNotNull();

        // Verify NotificationDelivery audit record was saved
        List<NotificationDelivery> deliveries = deliveryRepository.findAll();
        assertThat(deliveries).hasSize(1);
        assertThat(deliveries.get(0).getStatus()).isEqualTo(DeliveryStatus.ACCEPTED);

        // 6. Verify Examination Timeline via CalendarService
        List<CalendarItemDto> calendarDates = calendarService.getCalendarDates(
                "exam-e2e-cse",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                false
        );
        assertThat(calendarDates).hasSize(3);
        assertThat(calendarDates.stream().anyMatch(d -> d.dateType() == DateType.APPLICATION_DEADLINE && d.eventDate().equals(LocalDate.of(2026, 3, 5)))).isTrue();
        assertThat(calendarDates.stream().anyMatch(d -> d.dateType() == DateType.EXAM_DATE && d.eventDate().equals(LocalDate.of(2026, 5, 24)))).isTrue();

        // 7. Verify Ingestion Idempotency: Re-processing identical notice does not duplicate
        IngestionEngine.IngestionSummary idempotentSummary = persistenceService.persistSuccessfulIngestion(testSource, extractionResult);
        assertThat(idempotentSummary.newNotices()).isEqualTo(0);
        assertThat(idempotentSummary.revisedNotices()).isEqualTo(0);
        assertThat(noticeRepository.findAll()).hasSize(1);
        assertThat(notificationJobRepository.findAll()).hasSize(1);
    }
}
