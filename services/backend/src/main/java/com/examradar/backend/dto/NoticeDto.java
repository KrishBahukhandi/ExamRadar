package com.examradar.backend.dto;

import com.examradar.backend.domain.enums.NoticeStatus;
import com.examradar.backend.domain.enums.NoticeType;
import com.examradar.backend.domain.enums.VerificationStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record NoticeDto(
    UUID id,
    String examId,
    String examName,
    String organization,
    String sourceId,
    String sourceName,
    NoticeType noticeType,
    String title,
    String summary,
    String officialNoticeUrl,
    String officialApplyUrl,
    NoticeStatus status,
    LocalDate publishedDate,
    VerificationStatus verificationStatus,
    LocalDate applicationDeadline,
    List<NoticeDateDto> importantDates,
    OffsetDateTime lastCheckedAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    Integer totalVacancies,
    String vacancyBreakdownJson,
    String payScaleSummary,
    Integer applicationFeeGeneral,
    Integer applicationFeeReserved,
    String feeExemptionDetails,
    Integer ageMin,
    Integer ageMax,
    LocalDate ageCutoffDate,
    String ageRelaxationSummary,
    String educationalQualification,
    Boolean hasCorrigendum,
    String corrigendumSummary,
    String photoSpecifications,
    String signatureSpecifications,
    LocalDate feePaymentDeadline,
    String titleHi,
    String summaryHi,
    String examStages,
    String negativeMarking,
    String examMode,
    String syllabusUrl
) {
    // Backward compatibility constructor for existing tests/callers (pre-decision-enablers)
    public NoticeDto(
            UUID id,
            String examId,
            String examName,
            String organization,
            String sourceId,
            String sourceName,
            NoticeType noticeType,
            String title,
            String summary,
            String officialNoticeUrl,
            String officialApplyUrl,
            NoticeStatus status,
            LocalDate publishedDate,
            VerificationStatus verificationStatus,
            LocalDate applicationDeadline,
            List<NoticeDateDto> importantDates,
            OffsetDateTime lastCheckedAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this(
                id, examId, examName, organization, sourceId, sourceName, noticeType,
                title, summary, officialNoticeUrl, officialApplyUrl, status,
                publishedDate, verificationStatus, applicationDeadline, importantDates,
                lastCheckedAt, createdAt, updatedAt,
                null, null, null, null, null, null, null, null, null, null, null,
                false, null, null, null, null,
                null, null, null, null, null, null
        );
    }
}
