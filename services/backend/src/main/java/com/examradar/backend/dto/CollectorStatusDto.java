package com.examradar.backend.dto;

import java.time.OffsetDateTime;

/**
 * Live status, execution metrics, and health snapshot for official exam collectors.
 */
public record CollectorStatusDto(
        boolean autoScrapeEnabled,
        boolean autoPushEnabled,
        boolean isIngestionRunning,
        OffsetDateTime lastIngestionStartTime,
        OffsetDateTime lastIngestionEndTime,
        Long lastIngestionDurationMs,
        int lastIngestionNewNotices,
        int lastIngestionRevisedNotices,
        long enabledSourcesCount,
        long totalSourcesCount,
        long pendingNotificationJobsCount,
        long freeMemoryMb,
        long totalMemoryMb,
        long maxMemoryMb
) {}
