package com.examradar.backend.dto;

import com.examradar.backend.domain.enums.DateType;
import java.time.LocalDate;
import java.util.UUID;

public record NoticeDateDto(
    UUID id,
    DateType dateType,
    LocalDate eventDate,
    boolean isTentative,
    boolean isRevised,
    String originalDateStr,
    String sourceTextSnippet
) {}
