package com.examradar.backend.collector;

import com.examradar.backend.domain.enums.NoticeStatus;
import com.examradar.backend.domain.enums.NoticeType;
import com.examradar.backend.domain.enums.VerificationStatus;
import java.time.LocalDate;
import java.util.List;

public record ExtractedNoticeDto(
    String examId,
    String sourceId,
    NoticeType noticeType,
    String title,
    String summary,
    String officialNoticeUrl,
    String officialApplyUrl,
    NoticeStatus status,
    VerificationStatus verificationStatus,
    LocalDate publishedDate,
    List<ExtractedDateDto> importantDates,
    String rawDocumentContent,
    String contentHash,
    String eligibilitySummary,
    double parserConfidence
) {}
