package com.examradar.backend.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserProfile user;

    @Column(name = "reminder_7_days", nullable = false)
    private Boolean reminder7Days = true;

    @Column(name = "reminder_3_days", nullable = false)
    private Boolean reminder3Days = true;

    @Column(name = "reminder_1_day", nullable = false)
    private Boolean reminder1Day = true;

    @Column(name = "push_enabled", nullable = false)
    private Boolean pushEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public NotificationPreference() {}

    public NotificationPreference(UserProfile user) {
        this.user = user;
        this.reminder7Days = true;
        this.reminder3Days = true;
        this.reminder1Day = true;
        this.pushEnabled = true;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UserProfile getUser() { return user; }
    public void setUser(UserProfile user) { this.user = user; }

    public Boolean getReminder7Days() { return reminder7Days; }
    public void setReminder7Days(Boolean reminder7Days) { this.reminder7Days = reminder7Days; }

    public Boolean getReminder3Days() { return reminder3Days; }
    public void setReminder3Days(Boolean reminder3Days) { this.reminder3Days = reminder3Days; }

    public Boolean getReminder1Day() { return reminder1Day; }
    public void setReminder1Day(Boolean reminder1Day) { this.reminder1Day = reminder1Day; }

    public Boolean getPushEnabled() { return pushEnabled; }
    public void setPushEnabled(Boolean pushEnabled) { this.pushEnabled = pushEnabled; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
