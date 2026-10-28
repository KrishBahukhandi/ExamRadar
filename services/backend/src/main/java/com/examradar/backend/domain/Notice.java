package com.examradar.backend.domain;

import com.examradar.backend.domain.enums.NoticeStatus;
import com.examradar.backend.domain.enums.NoticeType;
import com.examradar.backend.domain.enums.VerificationStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notices")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @Enumerated(EnumType.STRING)
    @Column(name = "notice_type", nullable = false, length = 32)
    private NoticeType noticeType;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "official_notice_url", nullable = false, length = 1024)
    private String officialNoticeUrl;

    @Column(name = "official_apply_url", length = 1024)
    private String officialApplyUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NoticeStatus status = NoticeStatus.DETECTED;

    @Column(name = "published_date")
    private LocalDate publishedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 32)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "last_verified_at")
    private OffsetDateTime lastVerifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Column(name = "total_vacancies")
    private Integer totalVacancies;

    @Column(name = "vacancy_breakdown_json", columnDefinition = "TEXT")
    private String vacancyBreakdownJson;

    @Column(name = "pay_scale_summary", length = 255)
    private String payScaleSummary;

    @Column(name = "application_fee_general")
    private Integer applicationFeeGeneral;

    @Column(name = "application_fee_reserved")
    private Integer applicationFeeReserved;

    @Column(name = "fee_exemption_details", columnDefinition = "TEXT")
    private String feeExemptionDetails;

    @Column(name = "age_min")
    private Integer ageMin;

    @Column(name = "age_max")
    private Integer ageMax;

    @Column(name = "age_cutoff_date")
    private LocalDate ageCutoffDate;

    @Column(name = "age_relaxation_summary", columnDefinition = "TEXT")
    private String ageRelaxationSummary;

    @Column(name = "educational_qualification", columnDefinition = "TEXT")
    private String educationalQualification;

    public Notice() {}

    public Notice(Exam exam, Source source, NoticeType noticeType, String title, String summary,
                  String officialNoticeUrl, String officialApplyUrl, NoticeStatus status,
                  LocalDate publishedDate, VerificationStatus verificationStatus) {
        this.exam = exam;
        this.source = source;
        this.noticeType = noticeType;
        this.title = title;
        this.summary = summary;
        this.officialNoticeUrl = officialNoticeUrl;
        this.officialApplyUrl = officialApplyUrl;
        this.status = status;
        this.publishedDate = publishedDate;
        this.verificationStatus = verificationStatus;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Exam getExam() { return exam; }
    public void setExam(Exam exam) { this.exam = exam; }

    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }

    public NoticeType getNoticeType() { return noticeType; }
    public void setNoticeType(NoticeType noticeType) { this.noticeType = noticeType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getOfficialNoticeUrl() { return officialNoticeUrl; }
    public void setOfficialNoticeUrl(String officialNoticeUrl) { this.officialNoticeUrl = officialNoticeUrl; }

    public String getOfficialApplyUrl() { return officialApplyUrl; }
    public void setOfficialApplyUrl(String officialApplyUrl) { this.officialApplyUrl = officialApplyUrl; }

    public NoticeStatus getStatus() { return status; }
    public void setStatus(NoticeStatus status) { this.status = status; }

    public LocalDate getPublishedDate() { return publishedDate; }
    public void setPublishedDate(LocalDate publishedDate) { this.publishedDate = publishedDate; }

    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }

    public OffsetDateTime getLastVerifiedAt() { return lastVerifiedAt; }
    public void setLastVerifiedAt(OffsetDateTime lastVerifiedAt) { this.lastVerifiedAt = lastVerifiedAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getTotalVacancies() { return totalVacancies; }
    public void setTotalVacancies(Integer totalVacancies) { this.totalVacancies = totalVacancies; }

    public String getVacancyBreakdownJson() { return vacancyBreakdownJson; }
    public void setVacancyBreakdownJson(String vacancyBreakdownJson) { this.vacancyBreakdownJson = vacancyBreakdownJson; }

    public String getPayScaleSummary() { return payScaleSummary; }
    public void setPayScaleSummary(String payScaleSummary) { this.payScaleSummary = payScaleSummary; }

    public Integer getApplicationFeeGeneral() { return applicationFeeGeneral; }
    public void setApplicationFeeGeneral(Integer applicationFeeGeneral) { this.applicationFeeGeneral = applicationFeeGeneral; }

    public Integer getApplicationFeeReserved() { return applicationFeeReserved; }
    public void setApplicationFeeReserved(Integer applicationFeeReserved) { this.applicationFeeReserved = applicationFeeReserved; }

    public String getFeeExemptionDetails() { return feeExemptionDetails; }
    public void setFeeExemptionDetails(String feeExemptionDetails) { this.feeExemptionDetails = feeExemptionDetails; }

    public Integer getAgeMin() { return ageMin; }
    public void setAgeMin(Integer ageMin) { this.ageMin = ageMin; }

    public Integer getAgeMax() { return ageMax; }
    public void setAgeMax(Integer ageMax) { this.ageMax = ageMax; }

    public LocalDate getAgeCutoffDate() { return ageCutoffDate; }
    public void setAgeCutoffDate(LocalDate ageCutoffDate) { this.ageCutoffDate = ageCutoffDate; }

    public String getAgeRelaxationSummary() { return ageRelaxationSummary; }
    public void setAgeRelaxationSummary(String ageRelaxationSummary) { this.ageRelaxationSummary = ageRelaxationSummary; }

    public String getEducationalQualification() { return educationalQualification; }
    public void setEducationalQualification(String educationalQualification) { this.educationalQualification = educationalQualification; }
}
