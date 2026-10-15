package com.examradar.backend.domain;

import com.examradar.backend.domain.enums.DeliveryStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_deliveries")
public class NotificationDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private NotificationJob job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_token_id")
    private DeviceToken deviceToken;

    @Column(name = "provider_response_id", length = 255)
    private String providerResponseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DeliveryStatus status;

    @Column(name = "attempted_at", nullable = false)
    private OffsetDateTime attemptedAt = OffsetDateTime.now();

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    public NotificationDelivery() {}

    public NotificationDelivery(NotificationJob job, DeviceToken deviceToken,
                                String providerResponseId, DeliveryStatus status, String errorDetails) {
        this.job = job;
        this.deviceToken = deviceToken;
        this.providerResponseId = providerResponseId;
        this.status = status;
        this.errorDetails = errorDetails;
        this.attemptedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public NotificationJob getJob() { return job; }
    public void setJob(NotificationJob job) { this.job = job; }

    public DeviceToken getDeviceToken() { return deviceToken; }
    public void setDeviceToken(DeviceToken deviceToken) { this.deviceToken = deviceToken; }

    public String getProviderResponseId() { return providerResponseId; }
    public void setProviderResponseId(String providerResponseId) { this.providerResponseId = providerResponseId; }

    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus status) { this.status = status; }

    public OffsetDateTime getAttemptedAt() { return attemptedAt; }
    public void setAttemptedAt(OffsetDateTime attemptedAt) { this.attemptedAt = attemptedAt; }

    public String getErrorDetails() { return errorDetails; }
    public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }
}
