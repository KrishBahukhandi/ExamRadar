package com.examradar.backend.dto;

import com.examradar.backend.domain.enums.DateType;
import java.time.LocalDate;
import java.util.UUID;

public record CalendarItemDto(
    UUID dateId,
    UUID noticeId,
    String examId,
    String examName,
    String organization,
    DateType dateType,
    LocalDate eventDate,
    boolean isTentative,
    boolean isRevised,
    String title,
    String officialNoticeUrl,
    String officialApplyUrl
) {}
