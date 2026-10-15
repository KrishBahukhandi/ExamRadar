package com.examradar.backend.dto;

public record ExamDto(
    String id,
    String categoryId,
    String categoryName,
    String organization,
    String examName,
    String examCode,
    String officialPortalUrl,
    String description,
    boolean isActive
) {}
