package com.examradar.backend.dto;

import java.util.List;

public record UserPreferencesDto(
    List<String> followedExamIds,
    List<String> followedCategoryIds,
    boolean reminder7Days,
    boolean reminder3Days,
    boolean reminder1Day,
    boolean pushEnabled,
    boolean notifyNewNotices,
    boolean notifyDeadlines,
    boolean notifyResults
) {}
