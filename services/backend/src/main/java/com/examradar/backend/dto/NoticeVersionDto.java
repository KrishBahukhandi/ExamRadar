package com.examradar.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NoticeVersionDto(
    UUID id,
    UUID noticeId,
    int versionNumber,
    String contentHash,
    String changeSummary,
    OffsetDateTime recordedAt
) {}
