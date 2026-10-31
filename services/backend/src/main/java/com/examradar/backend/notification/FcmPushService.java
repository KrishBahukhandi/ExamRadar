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
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Executes push notification jobs via Firebase Cloud Messaging (FCM).
 * Operates in production mode when valid FCM credentials are provided,
 * and gracefully falls back to simulation mode in local/CI environments or when placeholder keys exist.
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

    @Value("${examradar.notification.batch-size:100}")
    private int batchSize = 100;

    private volatile boolean isFirebaseInitialized = false;

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
    public synchronized void initFirebase() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                isFirebaseInitialized = true;
                return;
            }

            byte[] credentialBytes = null;

            if (fcmCredentialsJson != null && !fcmCredentialsJson.isBlank()) {
                credentialBytes = fcmCredentialsJson.getBytes(StandardCharsets.UTF_8);
            } else if (fcmCredentialsPath != null && !fcmCredentialsPath.isBlank()) {
                if (fcmCredentialsPath.startsWith("classpath:")) {
                    try (InputStream is = getClass().getClassLoader().getResourceAsStream(fcmCredentialsPath.substring(10))) {
                        if (is != null) {
                            credentialBytes = is.readAllBytes();
                        }
                    }
                } else {
                    File file = new File(fcmCredentialsPath);
                    if (file.exists() && file.isFile()) {
                        try (InputStream is = new FileInputStream(file)) {
                            credentialBytes = is.readAllBytes();
                        }
                    }
                }
            } else {
                try (InputStream is = getClass().getClassLoader().getResourceAsStream("firebase-service-account.json")) {
                    if (is != null) {
                        credentialBytes = is.readAllBytes();
                    }
                }
            }

            if (credentialBytes != null && credentialBytes.length > 0) {
                String credStr = new String(credentialBytes, StandardCharsets.UTF_8);
                if (credStr.contains("REPLACE_WITH_ACTUAL") || credStr.contains("placeholder")) {
                    log.info("FCM credentials contain placeholder/template values. FcmPushService running in simulated mode.");
                    isFirebaseInitialized = false;
                    return;
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(credentialBytes)))
                        .build();
                FirebaseApp.initializeApp(options);
                isFirebaseInitialized = true;
                log.info("Firebase Cloud Messaging initialized successfully for ExamRadar.");
            } else {
                log.info("FCM credentials not provided. FcmPushService running in simulated mode for local/zero-cost operation.");
                isFirebaseInitialized = false;
            }
        } catch (Exception e) {
            log.warn("Could not initialize Firebase Admin SDK: {}. Falling back to simulation mode.", e.getMessage());
            isFirebaseInitialized = false;
        }
    }

    public boolean isFirebaseInitialized() {
        return isFirebaseInitialized;
    }

    /**
     * Processes all pending notification jobs due for execution with bounded batch sizing.
     */
    @Transactional
    public int processDueNotificationJobs() {
        OffsetDateTime now = OffsetDateTime.now();
        List<NotificationJob> dueJobs = jobRepository.findByStatusAndScheduledAtLessThanEqual(JobStatus.PENDING, now);

        if (dueJobs == null || dueJobs.isEmpty()) {
            return 0;
        }

        int effectiveBatchSize = Math.max(batchSize, 1);
        List<NotificationJob> batchToProcess = dueJobs.size() > effectiveBatchSize
                ? dueJobs.subList(0, effectiveBatchSize)
                : dueJobs;

        log.info("Processing {} due notification jobs (bounded from {} pending)", batchToProcess.size(), dueJobs.size());
        int successCount = 0;

        for (NotificationJob job : batchToProcess) {
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
        if (job == null) {
            return false;
        }

        UserProfile user = job.getUser();
        Notice notice = job.getNotice();

        if (user == null || notice == null) {
            log.error("Invalid notification job [{}]: user or notice is null. Marking as FAILED.", job.getId());
            job.setStatus(JobStatus.FAILED);
            job.setLastError("Malformed job: user or notice is null");
            job.setUpdatedAt(OffsetDateTime.now());
            jobRepository.save(job);
            return false;
        }

        job.setStatus(JobStatus.PROCESSING);
        job.setUpdatedAt(OffsetDateTime.now());
        jobRepository.save(job);

        String examName = (notice.getExam() != null && notice.getExam().getExamName() != null)
                ? notice.getExam().getExamName()
                : "Government Exam";

        List<DeviceToken> deviceTokens = deviceTokenRepository.findByUserIdAndIsActiveTrue(user.getId());
        if (deviceTokens == null || deviceTokens.isEmpty()) {
            log.info("No active devices for user [{}]. Marking job as SENT.", user.getId());
            job.setStatus(JobStatus.SENT);
            job.setLastError(null);
            job.setUpdatedAt(OffsetDateTime.now());
            jobRepository.save(job);
            return true;
        }

        String title = formatNotificationTitle(job.getEventType(), examName);
        String body = formatNotificationBody(job.getEventType(), notice);

        boolean allSucceeded = true;
        String lastErrorMessage = null;
        List<NotificationDelivery> deliveriesToSave = new ArrayList<>();
        List<DeviceToken> tokensToUpdate = new ArrayList<>();

        for (DeviceToken tokenEntity : deviceTokens) {
            String token = tokenEntity.getToken();
            if (token == null || token.isBlank()) {
                continue;
            }

            if (isFirebaseInitialized) {
                try {
                    Message message = Message.builder()
                            .setToken(token)
                            .setNotification(com.google.firebase.messaging.Notification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build())
                            .putData("noticeId", notice.getId() != null ? notice.getId().toString() : "")
                            .putData("examId", notice.getExam() != null ? notice.getExam().getId() : "")
                            .putData("eventType", job.getEventType() != null ? job.getEventType().name() : "")
                            .putData("targetScreen", "notice_detail")
                            .build();

                    String providerResponseId = FirebaseMessaging.getInstance().send(message);

                    NotificationDelivery delivery = new NotificationDelivery(
                            job, tokenEntity, providerResponseId, com.examradar.backend.domain.enums.DeliveryStatus.ACCEPTED, null
                    );
                    deliveriesToSave.add(delivery);

                    tokenEntity.setLastUsedAt(OffsetDateTime.now());
                    tokensToUpdate.add(tokenEntity);

                } catch (FirebaseMessagingException fme) {
                    allSucceeded = false;
                    String errorCode = fme.getMessagingErrorCode() != null ? fme.getMessagingErrorCode().name() : fme.getMessage();
                    lastErrorMessage = errorCode;
                    log.error("FCM delivery failure for device [{}]: {}", tokenEntity.getId(), errorCode);

                    if (fme.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED ||
                        fme.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                        tokenEntity.setIsActive(false);
                        tokensToUpdate.add(tokenEntity);
                        log.info("Deactivated invalid device token [{}]", tokenEntity.getId());

                        NotificationDelivery delivery = new NotificationDelivery(
                                job, tokenEntity, null, com.examradar.backend.domain.enums.DeliveryStatus.INVALID_TOKEN, fme.getMessage()
                        );
                        deliveriesToSave.add(delivery);
                    } else {
                        NotificationDelivery delivery = new NotificationDelivery(
                                job, tokenEntity, null, com.examradar.backend.domain.enums.DeliveryStatus.FAILED, fme.getMessage()
                        );
                        deliveriesToSave.add(delivery);
                    }
                } catch (Exception ex) {
                    allSucceeded = false;
                    lastErrorMessage = ex.getMessage();
                    NotificationDelivery delivery = new NotificationDelivery(
                            job, tokenEntity, null, com.examradar.backend.domain.enums.DeliveryStatus.FAILED, ex.getMessage()
                    );
                    deliveriesToSave.add(delivery);
                }
            } else {
                // Simulated push delivery for local dev / zero-cost mode
                String simResponseId = "sim-fcm-resp-" + UUID.randomUUID();
                log.info("[SIMULATED FCM] Push delivered to token [{}] -> Title: '{}', Body: '{}'",
                        token.substring(0, Math.min(token.length(), 15)) + "...", title, body);

                NotificationDelivery delivery = new NotificationDelivery(
                        job, tokenEntity, simResponseId, com.examradar.backend.domain.enums.DeliveryStatus.ACCEPTED, null
                );
                deliveriesToSave.add(delivery);

                tokenEntity.setLastUsedAt(OffsetDateTime.now());
                tokensToUpdate.add(tokenEntity);
            }
        }

        // Persist deliveries and token state updates
        for (NotificationDelivery delivery : deliveriesToSave) {
            deliveryRepository.save(delivery);
        }
        for (DeviceToken token : tokensToUpdate) {
            deviceTokenRepository.save(token);
        }

        if (allSucceeded) {
            job.setStatus(JobStatus.SENT);
            job.setLastError(null);
            job.setUpdatedAt(OffsetDateTime.now());
            jobRepository.save(job);
            return true;
        } else {
            int retries = job.getRetryCount() + 1;
            job.setRetryCount(retries);
            if (retries >= 3) {
                job.setStatus(JobStatus.FAILED);
                job.setLastError("Max retries exceeded (3 attempts)" + (lastErrorMessage != null ? ": " + lastErrorMessage : ""));
            } else {
                job.setStatus(JobStatus.PENDING);
                job.setLastError(lastErrorMessage != null ? lastErrorMessage : "Delivery partially failed");
                job.setScheduledAt(OffsetDateTime.now().plusMinutes(5L * retries)); // Bounded backoff
            }
            job.setUpdatedAt(OffsetDateTime.now());
            jobRepository.save(job);
            return false;
        }
    }

    private String formatNotificationTitle(NotificationEventType type, String examName) {
        if (type == null) {
            return "ExamRadar: Announcement — " + examName;
        }
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
        String title = (notice != null && notice.getTitle() != null && !notice.getTitle().isBlank())
                ? notice.getTitle()
                : "Official recruitment announcement published";

        if (type == null) {
            return title;
        }

        return switch (type) {
            case NEW_NOTICE -> title;
            case DEADLINE_7D -> "Application window closing in 7 days. Verify your checklist and submit early: " + title;
            case DEADLINE_3D -> "Only 3 days remaining before the official portal closes. Avoid last-minute server congestion: " + title;
            case DEADLINE_1D -> "Official application portal closes today. Complete payment and download confirmation: " + title;
            case DEADLINE_REVISED -> "Official notification date has been updated. Check revised schedule: " + title;
            case RESULT_OUT -> "Official result announcement has been published. Check scorecard and cutoff details: " + title;
        };
    }

    /**
     * Broadcasts a high-priority notification to an FCM topic (e.g. topic "notices_all" or "exam_upsc_cse").
     * Supports simultaneous distribution to thousands of active client devices in a single call.
     */
    public String broadcastToTopic(String topic, String title, String body, java.util.Map<String, String> data) {
        String cleanTopic = topic.replaceAll("[^a-zA-Z0-9-_.~%]", "_");
        if (isFirebaseInitialized) {
            try {
                Message.Builder builder = Message.builder()
                        .setTopic(cleanTopic)
                        .setNotification(com.google.firebase.messaging.Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build());

                if (data != null && !data.isEmpty()) {
                    builder.putAllData(data);
                }

                String messageId = FirebaseMessaging.getInstance().send(builder.build());
                log.info("Successfully broadcast FCM message [{}] to topic [{}]", messageId, cleanTopic);
                return messageId;
            } catch (Exception e) {
                log.error("Error broadcasting to FCM topic [{}]: {}", cleanTopic, e.getMessage());
                return null;
            }
        } else {
            String simId = "simulated-topic-" + UUID.randomUUID();
            log.info("[SIMULATED FCM TOPIC] Broadcast to [{}]: title='{}', body='{}' (msgId={})",
                    cleanTopic, title, body, simId);
            return simId;
        }
    }
}
