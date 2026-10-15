package com.examradar.backend.dto;

public record ExamCategoryDto(
    String id,
    String name,
    String slug,
    String description,
    int displayOrder
) {}
