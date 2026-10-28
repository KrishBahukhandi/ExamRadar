package com.examradar.backend.dto;

import com.examradar.backend.domain.enums.NotificationEventType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationInboxDto(
    UUID id,
    UUID noticeId,
    String examId,
    String examName,
    String organization,
    NotificationEventType eventType,
    String title,
    String message,
    OffsetDateTime scheduledAt,
    String status
) {}
