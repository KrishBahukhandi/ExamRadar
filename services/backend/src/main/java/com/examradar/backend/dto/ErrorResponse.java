package com.examradar.backend.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record ErrorResponse(
    boolean success,
    int status,
    String error,
    String message,
    String path,
    Map<String, String> validationErrors,
    OffsetDateTime timestamp
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(false, status, error, message, path, null, OffsetDateTime.now());
    }

    public static ErrorResponse validation(int status, String message, String path, Map<String, String> validationErrors) {
        return new ErrorResponse(false, status, "Validation Failed", message, path, validationErrors, OffsetDateTime.now());
    }
}
