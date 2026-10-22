package com.examradar.backend.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "exams")
public class Exam {

    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private ExamCategory category;

    @Column(nullable = false, length = 128)
    private String organization;

    @Column(name = "exam_name", nullable = false, length = 255)
    private String examName;

    @Column(name = "exam_code", length = 64)
    private String examCode;

    @Column(name = "official_portal_url", nullable = false, length = 512)
    private String officialPortalUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public Exam() {}

    public Exam(String id, ExamCategory category, String organization, String examName, String examCode, String officialPortalUrl, String description) {
        this(id, category, organization, examName, examCode, officialPortalUrl, description, true);
    }

    public Exam(String id, ExamCategory category, String organization, String examName, String examCode, String officialPortalUrl, String description, Boolean isActive) {
        this.id = id;
        this.category = category;
        this.organization = organization;
        this.examName = examName;
        this.examCode = examCode;
        this.officialPortalUrl = officialPortalUrl;
        this.description = description;
        this.isActive = isActive != null ? isActive : true;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public ExamCategory getCategory() { return category; }
    public void setCategory(ExamCategory category) { this.category = category; }

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }

    public String getExamName() { return examName; }
    public void setExamName(String examName) { this.examName = examName; }

    public String getExamCode() { return examCode; }
    public void setExamCode(String examCode) { this.examCode = examCode; }

    public String getOfficialPortalUrl() { return officialPortalUrl; }
    public void setOfficialPortalUrl(String officialPortalUrl) { this.officialPortalUrl = officialPortalUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
