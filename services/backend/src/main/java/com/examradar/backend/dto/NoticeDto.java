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
    OffsetDateTime updatedAt
) {}
