package com.examradar.app.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateFormatter {
    private val displayFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    fun format(date: LocalDate?): String {
        return date?.format(displayFormatter) ?: "Date TBD"
    }

    fun getDeadlineCountdown(deadline: LocalDate?): String? {
        if (deadline == null) return null
        val today = LocalDate.now()
        val daysRemaining = ChronoUnit.DAYS.between(today, deadline)

        return when {
            daysRemaining < 0 -> "Closed"
            daysRemaining == 0L -> "Closing Today!"
            daysRemaining == 1L -> "Closing Tomorrow!"
            else -> "$daysRemaining days left"
        }
    }

    fun isUrgentDeadline(deadline: LocalDate?): Boolean {
        if (deadline == null) return false
        val days = ChronoUnit.DAYS.between(LocalDate.now(), deadline)
        return days in 0..3
    }

    fun isApproachingDeadline(deadline: LocalDate?): Boolean {
        if (deadline == null) return false
        val days = ChronoUnit.DAYS.between(LocalDate.now(), deadline)
        return days in 4..7
    }

    fun formatRelativeTime(timestamp: Long): String {
        val diffMinutes = (System.currentTimeMillis() - timestamp) / 60000
        return when {
            diffMinutes < 1 -> "Just now"
            diffMinutes < 60 -> "$diffMinutes min ago"
            diffMinutes < 1440 -> "${diffMinutes / 60}h ago"
            else -> "${diffMinutes / 1440}d ago"
        }
    }
}
