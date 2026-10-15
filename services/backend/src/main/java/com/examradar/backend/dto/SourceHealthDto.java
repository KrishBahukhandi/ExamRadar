package com.examradar.backend.dto;

import java.time.OffsetDateTime;

public record SourceHealthDto(
    String sourceId,
    String organization,
    String sourceName,
    String officialBaseUrl,
    boolean isEnabled,
    OffsetDateTime lastAttemptedFetch,
    OffsetDateTime lastSuccessfulFetch,
    int consecutiveFailures,
    String lastErrorMessage,
    String contentHash
) {}
