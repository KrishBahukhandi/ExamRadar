package com.examradar.backend.dto;

import java.util.List;
import java.util.UUID;

public record UserProfileDto(
    UUID id,
    String displayName,
    String graduationStatus,
    Integer graduationYear,
    String educationLevel,
    String targetState,
    String languagePreference
) {}
