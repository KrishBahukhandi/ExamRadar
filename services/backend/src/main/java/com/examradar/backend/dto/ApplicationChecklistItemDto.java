package com.examradar.backend.dto;

import java.util.UUID;

public record ApplicationChecklistItemDto(
    UUID id,
    String itemKey,
    String label,
    boolean isCompleted,
    int sortOrder
) {}
