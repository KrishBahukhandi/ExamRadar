package com.examradar.app.util

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import com.examradar.app.data.model.DateType
import java.time.LocalDate
import java.time.ZoneId

object CalendarSyncHelper {

    /**
     * Opens the native Android Calendar application with pre-populated event fields.
     * Uses the standard Intent.ACTION_INSERT on CalendarContract.Events.CONTENT_URI.
     * Does not require READ_CALENDAR or WRITE_CALENDAR runtime permissions.
     */
    fun syncEventToCalendar(
        context: Context,
        title: String,
        description: String,
        location: String?,
        date: LocalDate,
        dateType: DateType? = null
    ): Boolean {
        return try {
            val startMillis = date.atTime(9, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            val endMillis = date.atTime(18, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            val intent = Intent(Intent.ACTION_INSERT).apply {
                setDataAndType(CalendarContract.Events.CONTENT_URI, "vnd.android.cursor.item/event")
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.Events.DESCRIPTION, description)
                if (!location.isNullOrBlank()) {
                    putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                }
                putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
                putExtra(CalendarContract.Events.HAS_ALARM, 1)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            Toast.makeText(context, "Opening Calendar to add reminder...", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to launch calendar app", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
