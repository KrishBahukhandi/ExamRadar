package com.examradar.backend.collector;

import java.time.LocalDate
;import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateParserUtil {

    private static final Pattern DATE_PATTERN_DMY = Pattern.compile(
        "\\b(\\d{1,2})[./-](\\d{1,2})[./-](\\d{4})\\b"
    );

    private static final Pattern DATE_PATTERN_TEXT = Pattern.compile(
        "\\b(\\d{1,2})(?:st|nd|rd|th)?\\s+(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)[,\\s]+(\\d{4})\\b",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DEADLINE_KEYWORD_PATTERN = Pattern.compile(
        "(?:last\\s+date|closing(?:\\s+date)?(?:\\s+on)?|closes(?:\\s+on)?|up\\s+to|apply\\s+till|deadline)[:\\s]+([^\\n,.<]{5,35})",
        Pattern.CASE_INSENSITIVE
    );

    private static final DateTimeFormatter TEXT_MONTH_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("d MMMM yyyy")
            .toFormatter(Locale.ENGLISH);

    private static final DateTimeFormatter SHORT_TEXT_MONTH_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("d MMM yyyy")
            .toFormatter(Locale.ENGLISH);

    /**
     * Attempts to parse a date string formatted as dd/MM/yyyy, dd.MM.yyyy, dd-MM-yyyy or dd Month yyyy.
     */
    public static Optional<LocalDate> parseDate(String input) {
        if (input == null || input.isBlank()) return Optional.empty();

        // Check standard numerical pattern dd/MM/yyyy or dd.MM.yyyy or dd-MM-yyyy
        Matcher dmyMatcher = DATE_PATTERN_DMY.matcher(input);
        if (dmyMatcher.find()) {
            try {
                int day = Integer.parseInt(dmyMatcher.group(1));
                int month = Integer.parseInt(dmyMatcher.group(2));
                int year = Integer.parseInt(dmyMatcher.group(3));
                return Optional.of(LocalDate.of(year, month, day));
            } catch (Exception ignored) {}
        }

        // Check text month pattern: e.g. "30 September 2026" or "15 Nov 2025"
        Matcher textMatcher = DATE_PATTERN_TEXT.matcher(input);
        if (textMatcher.find()) {
            String day = textMatcher.group(1);
            String month = textMatcher.group(2);
            String year = textMatcher.group(3);
            String candidate = day + " " + month + " " + year;
            try {
                return Optional.of(LocalDate.parse(candidate, TEXT_MONTH_FORMATTER));
            } catch (Exception e1) {
                try {
                    return Optional.of(LocalDate.parse(candidate, SHORT_TEXT_MONTH_FORMATTER));
                } catch (Exception ignored) {}
            }
        }

        return Optional.empty();
    }

    /**
     * Searches for deadline keywords and extracts the accompanying date.
     */
    public static Optional<LocalDate> extractDeadlineFromText(String text) {
        if (text == null) return Optional.empty();

        // 1. Try keyword followed directly by date snippet
        Matcher matcher = DEADLINE_KEYWORD_PATTERN.matcher(text);
        if (matcher.find()) {
            String snippet = matcher.group(1);
            Optional<LocalDate> parsed = parseDate(snippet);
            if (parsed.isPresent()) return parsed;
        }

        // 2. Scan sentences/clauses containing deadline keywords
        String[] clauses = text.split("[.\\n;]");
        for (String clause : clauses) {
            String lower = clause.toLowerCase();
            if (lower.contains("last date") || lower.contains("closing") || lower.contains("closes") ||
                lower.contains("deadline") || lower.contains("apply till")) {
                Optional<LocalDate> parsed = parseDate(clause);
                if (parsed.isPresent()) return parsed;
            }
        }

        return Optional.empty();
    }
}
