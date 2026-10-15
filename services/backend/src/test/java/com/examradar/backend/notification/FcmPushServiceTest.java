package com.examradar.backend.notification;

import com.examradar.backend.domain.*;
import com.examradar.backend.domain.enums.JobStatus;
import com.examradar.backend.domain.enums.NotificationEventType;
import com.examradar.backend.repository.DeviceTokenRepository;
import com.examradar.backend.repository.NotificationDeliveryRepository;
import com.examradar.backend.repository.NotificationJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FcmPushServiceTest {

    @Mock
    private NotificationJobRepository jobRepository;

    @Mock
    private NotificationDeliveryRepository deliveryRepository;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    private FcmPushService fcmPushService;

    @BeforeEach
    void setUp() {
        fcmPushService = new FcmPushService(jobRepository, deliveryRepository, deviceTokenRepository);
    }

    @Test
    void testProcessDueJobsInSimulatedMode() {
        UserProfile user = new UserProfile(UUID.randomUUID(), "candidate@example.com", "Candidate");
        Notice notice = new Notice(null, null, null, "IBPS PO 2026 Notification", "Summary",
                "https://ibps.in", null, null, null, null);
        notice.setId(UUID.randomUUID());

        NotificationJob job = new NotificationJob(
                "key-sim-1", user, notice, NotificationEventType.NEW_NOTICE, OffsetDateTime.now().minusMinutes(5)
        );

        DeviceToken deviceToken = new DeviceToken(user, "fcm-token-12345", "ANDROID", "1.0.0");

        when(jobRepository.findByStatusAndScheduledAtLessThanEqual(eq(JobStatus.PENDING), any(OffsetDateTime.class)))
                .thenReturn(List.of(job));

        when(deviceTokenRepository.findByUserIdAndIsActiveTrue(user.getId()))
                .thenReturn(List.of(deviceToken));

        int processed = fcmPushService.processDueNotificationJobs();

        assertEquals(1, processed);
        assertEquals(JobStatus.SENT, job.getStatus());
        verify(deliveryRepository, times(1)).save(any(NotificationDelivery.class));
        verify(jobRepository, atLeastOnce()).save(job);
    }
}
