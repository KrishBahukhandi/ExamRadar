package com.examradar.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ApplicationChecklistDto(
    UUID id,
    UUID noticeId,
    String examId,
    String examName,
    String noticeTitle,
    String notes,
    List<ApplicationChecklistItemDto> items,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
