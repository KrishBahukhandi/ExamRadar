package com.examradar.app

import com.examradar.app.util.DateFormatter
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class DateFormatterTest {

    @Test
    fun testFormat_validDate() {
        val date = LocalDate.of(2025, 11, 15)
        assertEquals("15 Nov 2025", DateFormatter.format(date))
    }

    @Test
    fun testFormat_nullDate() {
        assertEquals("Date TBD", DateFormatter.format(null))
    }

    @Test
    fun testDeadlineCountdown_today() {
        val today = LocalDate.now()
        assertEquals("Closing Today!", DateFormatter.getDeadlineCountdown(today))
    }

    @Test
    fun testDeadlineCountdown_tomorrow() {
        val tomorrow = LocalDate.now().plusDays(1)
        assertEquals("Closing Tomorrow!", DateFormatter.getDeadlineCountdown(tomorrow))
    }

    @Test
    fun testDeadlineCountdown_past() {
        val past = LocalDate.now().minusDays(2)
        assertEquals("Closed", DateFormatter.getDeadlineCountdown(past))
    }

    @Test
    fun testDeadlineCountdown_future() {
        val future = LocalDate.now().plusDays(5)
        assertEquals("5 days left", DateFormatter.getDeadlineCountdown(future))
    }

    @Test
    fun testIsUrgentDeadline() {
        assertTrue(DateFormatter.isUrgentDeadline(LocalDate.now().plusDays(2)))
        assertTrue(DateFormatter.isUrgentDeadline(LocalDate.now()))
        assertFalse(DateFormatter.isUrgentDeadline(LocalDate.now().plusDays(5)))
        assertFalse(DateFormatter.isUrgentDeadline(LocalDate.now().minusDays(1)))
    }
}
