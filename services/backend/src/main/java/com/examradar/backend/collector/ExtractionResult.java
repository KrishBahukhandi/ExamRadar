package com.examradar.backend.collector;

import java.util.List;

public record ExtractionResult(
    String sourceId,
    boolean isSuccessful,
    boolean contentChanged,
    String contentHash,
    int httpStatus,
    long responseTimeMs,
    List<ExtractedNoticeDto> notices,
    String errorMessage
) {
    public static ExtractionResult success(String sourceId, boolean contentChanged, String contentHash,
                                           int httpStatus, long latency, List<ExtractedNoticeDto> notices) {
        return new ExtractionResult(sourceId, true, contentChanged, contentHash, httpStatus, latency, notices, null);
    }

    public static ExtractionResult noChange(String sourceId, String contentHash, int httpStatus, long latency) {
        return new ExtractionResult(sourceId, true, false, contentHash, httpStatus, latency, List.of(), null);
    }

    public static ExtractionResult failure(String sourceId, int httpStatus, long latency, String errorMessage) {
        return new ExtractionResult(sourceId, false, false, null, httpStatus, latency, List.of(), errorMessage);
    }
}
