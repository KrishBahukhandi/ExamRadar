package com.examradar.backend.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_exam_preferences", uniqueConstraints = {
    @UniqueConstraint(name = "uq_user_exam", columnNames = {"user_id", "exam_id"})
})
public class UserExamPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserProfile user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id")
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ExamCategory category;

    @Column(name = "notify_new_notices", nullable = false)
    private Boolean notifyNewNotices = true;

    @Column(name = "notify_deadlines", nullable = false)
    private Boolean notifyDeadlines = true;

    @Column(name = "notify_results", nullable = false)
    private Boolean notifyResults = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public UserExamPreference() {}

    public UserExamPreference(UserProfile user, Exam exam, ExamCategory category) {
        this.user = user;
        this.exam = exam;
        this.category = category;
        this.notifyNewNotices = true;
        this.notifyDeadlines = true;
        this.notifyResults = true;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UserProfile getUser() { return user; }
    public void setUser(UserProfile user) { this.user = user; }

    public Exam getExam() { return exam; }
    public void setExam(Exam exam) { this.exam = exam; }

    public ExamCategory getCategory() { return category; }
    public void setCategory(ExamCategory category) { this.category = category; }

    public Boolean getNotifyNewNotices() { return notifyNewNotices; }
    public void setNotifyNewNotices(Boolean notifyNewNotices) { this.notifyNewNotices = notifyNewNotices; }

    public Boolean getNotifyDeadlines() { return notifyDeadlines; }
    public void setNotifyDeadlines(Boolean notifyDeadlines) { this.notifyDeadlines = notifyDeadlines; }

    public Boolean getNotifyResults() { return notifyResults; }
    public void setNotifyResults(Boolean notifyResults) { this.notifyResults = notifyResults; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
