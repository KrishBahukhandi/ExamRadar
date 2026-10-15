package com.examradar.backend.collector;

import com.examradar.backend.domain.enums.DateType;
import java.time.LocalDate;

public record ExtractedDateDto(
    DateType dateType,
    LocalDate eventDate,
    boolean isTentative,
    boolean isRevised,
    String originalDateStr,
    String sourceTextSnippet,
    double confidenceScore
) {}
