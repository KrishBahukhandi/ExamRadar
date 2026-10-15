package com.examradar.backend.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "eligibility_rules")
public class EligibilityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id")
    private Notice notice;

    @Column(name = "rule_type", nullable = false, length = 32)
    private String ruleType; // AGE_LIMIT, EDUCATION, ATTEMPTS, NATIONALITY

    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Column(name = "cut_off_date")
    private LocalDate cutOffDate;

    @Column(name = "required_degree", length = 255)
    private String requiredDegree;

    @Column(name = "rule_description", nullable = false, columnDefinition = "TEXT")
    private String ruleDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_version_id")
    private NoticeVersion sourceVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public EligibilityRule() {}

    public EligibilityRule(Exam exam, Notice notice, String ruleType, Integer minAge, Integer maxAge,
                           LocalDate cutOffDate, String requiredDegree, String ruleDescription, NoticeVersion sourceVersion) {
        this.exam = exam;
        this.notice = notice;
        this.ruleType = ruleType;
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.cutOffDate = cutOffDate;
        this.requiredDegree = requiredDegree;
        this.ruleDescription = ruleDescription;
        this.sourceVersion = sourceVersion;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Exam getExam() { return exam; }
    public void setExam(Exam exam) { this.exam = exam; }

    public Notice getNotice() { return notice; }
    public void setNotice(Notice notice) { this.notice = notice; }

    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }

    public Integer getMinAge() { return minAge; }
    public void setMinAge(Integer minAge) { this.minAge = minAge; }

    public Integer getMaxAge() { return maxAge; }
    public void setMaxAge(Integer maxAge) { this.maxAge = maxAge; }

    public LocalDate getCutOffDate() { return cutOffDate; }
    public void setCutOffDate(LocalDate cutOffDate) { this.cutOffDate = cutOffDate; }

    public String getRequiredDegree() { return requiredDegree; }
    public void setRequiredDegree(String requiredDegree) { this.requiredDegree = requiredDegree; }

    public String getRuleDescription() { return ruleDescription; }
    public void setRuleDescription(String ruleDescription) { this.ruleDescription = ruleDescription; }

    public NoticeVersion getSourceVersion() { return sourceVersion; }
    public void setSourceVersion(NoticeVersion sourceVersion) { this.sourceVersion = sourceVersion; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
