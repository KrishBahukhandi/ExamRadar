package com.examradar.backend.domain;

import com.examradar.backend.domain.enums.DateType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notice_dates")
public class NoticeDate {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    @Enumerated(EnumType.STRING)
    @Column(name = "date_type", nullable = false, length = 32)
    private DateType dateType;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "event_time")
    private OffsetDateTime eventTime;

    @Column(name = "is_tentative", nullable = false)
    private Boolean isTentative = false;

    @Column(name = "is_revised", nullable = false)
    private Boolean isRevised = false;

    @Column(name = "original_date_str", length = 128)
    private String originalDateStr;

    @Column(name = "source_text_snippet", columnDefinition = "TEXT")
    private String sourceTextSnippet;

    @Column(name = "confidence_score", precision = 3, scale = 2)
    private BigDecimal confidenceScore = BigDecimal.valueOf(1.0);

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public NoticeDate() {}

    public NoticeDate(Notice notice, DateType dateType, LocalDate eventDate, Boolean isTentative,
                      Boolean isRevised, String originalDateStr, String sourceTextSnippet) {
        this.notice = notice;
        this.dateType = dateType;
        this.eventDate = eventDate;
        this.isTentative = isTentative;
        this.isRevised = isRevised;
        this.originalDateStr = originalDateStr;
        this.sourceTextSnippet = sourceTextSnippet;
        this.confidenceScore = BigDecimal.valueOf(1.0);
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Notice getNotice() { return notice; }
    public void setNotice(Notice notice) { this.notice = notice; }

    public DateType getDateType() { return dateType; }
    public void setDateType(DateType dateType) { this.dateType = dateType; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    public OffsetDateTime getEventTime() { return eventTime; }
    public void setEventTime(OffsetDateTime eventTime) { this.eventTime = eventTime; }

    public Boolean getIsTentative() { return isTentative; }
    public void setIsTentative(Boolean tentative) { isTentative = tentative; }

    public Boolean getIsRevised() { return isRevised; }
    public void setIsRevised(Boolean revised) { isRevised = revised; }

    public String getOriginalDateStr() { return originalDateStr; }
    public void setOriginalDateStr(String originalDateStr) { this.originalDateStr = originalDateStr; }

    public String getSourceTextSnippet() { return sourceTextSnippet; }
    public void setSourceTextSnippet(String sourceTextSnippet) { this.sourceTextSnippet = sourceTextSnippet; }

    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
