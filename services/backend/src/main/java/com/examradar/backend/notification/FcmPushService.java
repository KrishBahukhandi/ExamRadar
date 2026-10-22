package com.examradar.backend.notification;

import com.examradar.backend.domain.*;
import com.examradar.backend.domain.enums.JobStatus;
import com.examradar.backend.domain.enums.NotificationEventType;
import com.examradar.backend.repository.DeviceTokenRepository;
import com.examradar.backend.repository.NotificationDeliveryRepository;
import com.examradar.backend.repository.NotificationJobRepository;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Executes push notification jobs via Firebase Cloud Messaging (FCM).
 * Operates in production mode when FCM service account credentials are provided,
 * and gracefully falls back to simulation mode in local/CI environments.
 */
@Service
public class FcmPushService {

    private static final Logger log = LoggerFactory.getLogger(FcmPushService.class);

    private final NotificationJobRepository jobRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    @Value("${examradar.fcm.credentials-path:}")
    private String fcmCredentialsPath;

    @Value("${examradar.fcm.credentials-json:}")
    private String fcmCredentialsJson;

    private boolean isFirebaseInitialized = false;

    public FcmPushService(
            NotificationJobRepository jobRepository,
            NotificationDeliveryRepository deliveryRepository,
            DeviceTokenRepository deviceTokenRepository
    ) {
        this.jobRepository = jobRepository;
        this.deliveryRepository = deliveryRepository;
        this.deviceTokenRepository = deviceTokenRepository;
    }

    @PostConstruct
    public void initFirebase() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccountStream = null;

                if (fcmCredentialsJson != null && !fcmCredentialsJson.isBlank()) {
                    serviceAccountStream = new ByteArrayInputStream(fcmCredentialsJson.getBytes(StandardCharsets.UTF_8));
                } else if (fcmCredentialsPath != null && !fcmCredentialsPath.isBlank()) {
                    if (fcmCredentialsPath.startsWith("classpath:")) {
                        serviceAccountStream = getClass().getClassLoader().getResourceAsStream(fcmCredentialsPath.substring(10));
                    } else {
                        serviceAccountStream = new FileInputStream(fcmCredentialsPath);
                    }
                } else {
                    InputStream cp = getClass().getClassLoader().getResourceAsStream("firebase-service-account.json");
                    if (cp != null) {
                        serviceAccountStream = cp;
                    }
                }

                if (serviceAccountStream != null) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                            .build();
                    FirebaseApp.initializeApp(options);
                    isFirebaseInitialized = true;
                    log.info("Firebase Cloud Messaging initialized successfully for ExamRadar.");
                } else {
                    log.info("FCM credentials not provided. FcmPushService running in simulated mode for local/zero-cost operation.");
                }
            } else {
                isFirebaseInitialized = true;
            }
        } catch (Exception e) {
            log.warn("Could not initialize Firebase Admin SDK: {}. Falling back to simulation mode.", e.getMessage());
            isFirebaseInitialized = false;
        }
    }

    /**
     * Processes all pending notification jobs due for execution.
     */
    @Transactional
    public int processDueNotificationJobs() {
        OffsetDateTime now = OffsetDateTime.now();
        List<NotificationJob> dueJobs = jobRepository.findByStatusAndScheduledAtLessThanEqual(JobStatus.PENDING, now);

        if (dueJobs.isEmpty()) {
            return 0;
        }

        log.info("Processing {} due notification jobs", dueJobs.size());
        int successCount = 0;

        for (NotificationJob job : dueJobs) {
            boolean sent = sendPushForJob(job);
            if (sent) {
                successCount++;
            }
        }

        return successCount;
    }

    /**
     * Sends push notifications to all active devices registered to the job's user.
     */
    @Transactional
    public boolean sendPushForJob(NotificationJob job) {
        job.setStatus(JobStatus.PROCESSING);
        job.setUpdatedAt(OffsetDateTime.now());
        jobRepository.save(job);

        UserProfile user = job.getUser();
        Notice notice = job.getNotice();
        String examName = notice.getExam() != null ? notice.getExam().getExamName() : "Government Exam";

        List<DeviceToken> deviceTokens = deviceTokenRepository.findByUserIdAndIsActiveTrue(user.getId());
        if (deviceTokens.isEmpty()) {
            log.info("No active devices for user [{}]. Marking job as SENT.", user.getId());
            job.setStatus(JobStatus.SENT);
            jobRepository.save(job);
            return true;
        }

        String title = formatNotificationTitle(job.getEventType(), examName);
        String body = formatNotificationBody(job.getEventType(), notice);

        boolean allSucceeded = true;

        for (DeviceToken tokenEntity : deviceTokens) {
            String token = tokenEntity.getToken();

            if (isFirebaseInitialized) {
                try {
                    Message message = Message.builder()
                            .setToken(token)
                            .setNotification(com.google.firebase.messaging.Notification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build())
                            .putData("noticeId", notice.getId().toString())
                            .putData("examId", notice.getExam() != null ? notice.getExam().getId() : "")
                            .putData("eventType", job.getEventType().name())
                            .putData("targetScreen", "notice_detail")
                            .build();

                    String providerResponseId = FirebaseMessaging.getInstance().send(message);

                    NotificationDelivery delivery = new NotificationDelivery(
                            job, tokenEntity, providerResponseId, com.examradar.backend.domain.enums.DeliveryStatus.ACCEPTED, null
                    );
                    deliveryRepository.save(delivery);

                    tokenEntity.setLastUsedAt(OffsetDateTime.now());
                    deviceTokenRepository.save(tokenEntity);

                } catch (FirebaseMessagingException fme) {
                    allSucceeded = false;
                    String errorCode = fme.getMessagingErrorCode() != null ? fme.getMessagingErrorCode().name() : fme.getMessage();
                    log.error("FCM delivery failure for device [{}]: {}", tokenEntity.getId(), errorCode);

                    if (fme.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED ||
                        fme.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                        tokenEntity.setIsActive(false);
                        deviceTokenRepository.save(tokenEntity);
                        log.info("Deactivated invalid device token [{}]", tokenEntity.getId());

                        NotificationDelivery delivery = new NotificationDelivery(
                                job, tokenEntity, null, com.examradar.backend.domain.enums.DeliveryStatus.INVALID_TOKEN, fme.getMessage()
                        );
                        deliveryRepository.save(delivery);
                    } else {
                        NotificationDelivery delivery = new NotificationDelivery(
                                job, tokenEntity, null, com.examradar.backend.domain.enums.DeliveryStatus.FAILED, fme.getMessage()
                        );
                        deliveryRepository.save(delivery);
                    }
                } catch (Exception ex) {
                    allSucceeded = false;
                    NotificationDelivery delivery = new NotificationDelivery(
                            job, tokenEntity, null, com.examradar.backend.domain.enums.DeliveryStatus.FAILED, ex.getMessage()
                    );
                    deliveryRepository.save(delivery);
                }
            } else {
                // Simulated push delivery for local dev / zero-cost mode
                String simResponseId = "sim-fcm-resp-" + UUID.randomUUID();
                log.info("[SIMULATED FCM] Push delivered to token [{}] -> Title: '{}', Body: '{}'",
                        token.substring(0, Math.min(token.length(), 15)) + "...", title, body);

                NotificationDelivery delivery = new NotificationDelivery(
                        job, tokenEntity, simResponseId, com.examradar.backend.domain.enums.DeliveryStatus.ACCEPTED, null
                );
                deliveryRepository.save(delivery);

                tokenEntity.setLastUsedAt(OffsetDateTime.now());
                deviceTokenRepository.save(tokenEntity);
            }
        }

        if (allSucceeded) {
            job.setStatus(JobStatus.SENT);
            job.setUpdatedAt(OffsetDateTime.now());
            jobRepository.save(job);
            return true;
        } else {
            int retries = job.getRetryCount() + 1;
            job.setRetryCount(retries);
            if (retries >= 3) {
                job.setStatus(JobStatus.FAILED);
                job.setLastError("Max retries exceeded (3 attempts)");
            } else {
                job.setStatus(JobStatus.PENDING);
                job.setScheduledAt(OffsetDateTime.now().plusMinutes(5L * retries)); // Bounded backoff
            }
            job.setUpdatedAt(OffsetDateTime.now());
            jobRepository.save(job);
            return false;
        }
    }

    private String formatNotificationTitle(NotificationEventType type, String examName) {
        return switch (type) {
            case NEW_NOTICE -> "ExamRadar: New " + examName + " Announcement";
            case DEADLINE_7D -> "ExamRadar: 7 Days Left — " + examName;
            case DEADLINE_3D -> "ExamRadar: URGENT (3 Days) — " + examName;
            case DEADLINE_1D -> "ExamRadar: FINAL DAY — " + examName + " Application Closes";
            case DEADLINE_REVISED -> "ExamRadar: Deadline Revised — " + examName;
            case RESULT_OUT -> "ExamRadar: Results Declared — " + examName;
        };
    }

    private String formatNotificationBody(NotificationEventType type, Notice notice) {
        String title = notice.getTitle();
        return switch (type) {
            case NEW_NOTICE -> title;
            case DEADLINE_7D -> "Application window closing in 7 days. Verify your checklist and submit early: " + title;
            case DEADLINE_3D -> "Only 3 days remaining before the official portal closes. Avoid last-minute server congestion: " + title;
            case DEADLINE_1D -> "Official application portal closes today. Complete payment and download confirmation: " + title;
            case DEADLINE_REVISED -> "Official notification date has been updated. Check revised schedule: " + title;
            case RESULT_OUT -> "Official result announcement has been published. Check scorecard and cutoff details: " + title;
        };
    }
}
