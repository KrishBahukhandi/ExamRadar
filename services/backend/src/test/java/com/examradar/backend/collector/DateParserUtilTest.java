package com.examradar.backend.collector;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DateParserUtilTest {

    @Test
    void testParseDateDmyWithDots() {
        Optional<LocalDate> date = DateParserUtil.parseDate("15.11.2025");
        assertTrue(date.isPresent());
        assertEquals(LocalDate.of(2025, 11, 15), date.get());
    }

    @Test
    void testParseDateDmyWithHyphens() {
        Optional<LocalDate> date = DateParserUtil.parseDate("30-09-2026");
        assertTrue(date.isPresent());
        assertEquals(LocalDate.of(2026, 9, 30), date.get());
    }

    @Test
    void testParseDateDmyWithSlashes() {
        Optional<LocalDate> date = DateParserUtil.parseDate("22/01/2026");
        assertTrue(date.isPresent());
        assertEquals(LocalDate.of(2026, 1, 22), date.get());
    }

    @Test
    void testParseDateFullTextMonth() {
        Optional<LocalDate> date = DateParserUtil.parseDate("30 September 2026");
        assertTrue(date.isPresent());
        assertEquals(LocalDate.of(2026, 9, 30), date.get());
    }

    @Test
    void testParseDateShortTextMonth() {
        Optional<LocalDate> date = DateParserUtil.parseDate("15 Nov 2025");
        assertTrue(date.isPresent());
        assertEquals(LocalDate.of(2025, 11, 15), date.get());
    }

    @Test
    void testExtractDeadlineFromText() {
        String noticeText = "Candidates are advised to complete registration. Last date: 28-10-2026 up to 5:00 PM.";
        Optional<LocalDate> deadline = DateParserUtil.extractDeadlineFromText(noticeText);
        assertTrue(deadline.isPresent());
        assertEquals(LocalDate.of(2026, 10, 28), deadline.get());
    }

    @Test
    void testExtractDeadlineClosingDateKeyword() {
        String noticeText = "Closing date for online payment of examination fee is 15 November 2026.";
        Optional<LocalDate> deadline = DateParserUtil.extractDeadlineFromText(noticeText);
        assertTrue(deadline.isPresent());
        assertEquals(LocalDate.of(2026, 11, 15), deadline.get());
    }
}
