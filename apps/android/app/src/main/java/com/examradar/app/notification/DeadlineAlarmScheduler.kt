package com.examradar.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.examradar.app.data.model.DateType
import com.examradar.app.data.model.NoticeModel
import com.examradar.app.data.preferences.UserPreferences
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Offline Deadline Alarm Scheduler utilizing Android AlarmManager (RTC_WAKEUP).
 * Accurately fires at T-48h and T-24h before official 5:00 PM IST application cutoffs.
 * 100% offline, zero-cost, persistent through device reboots via DeadlineBootReceiver.
 */
object DeadlineAlarmScheduler {

    private const val TAG = "DeadlineAlarmScheduler"
    val ZONE_IST: ZoneId = ZoneId.of("Asia/Kolkata")
    const val DEFAULT_CUTOFF_HOUR = 17 // 5:00 PM IST
    const val DEFAULT_CUTOFF_MINUTE = 0

    /**
     * Compute epoch millis for a given trigger offset before application cutoff.
     */
    fun computeTriggerEpochMillis(
        deadlineDate: LocalDate,
        hoursBefore: Int,
        cutoffHour: Int = DEFAULT_CUTOFF_HOUR,
        cutoffMinute: Int = DEFAULT_CUTOFF_MINUTE,
        zoneId: ZoneId = ZONE_IST
    ): Long {
        val cutoffInstant = deadlineDate.atTime(cutoffHour, cutoffMinute).atZone(zoneId)
        val triggerInstant = cutoffInstant.minusHours(hoursBefore.toLong())
        return triggerInstant.toInstant().toEpochMilli()
    }

    /**
     * Generate deterministic, non-colliding 31-bit positive request codes.
     */
    fun getRequestCode(noticeId: String, hoursBefore: Int): Int {
        var hash = 17
        hash = 31 * hash + noticeId.hashCode()
        hash = 31 * hash + hoursBefore
        return hash and 0x7FFFFFFF
    }

    /**
     * Returns true if the trigger is in the future relative to the given timestamp.
     */
    fun shouldScheduleTrigger(triggerEpochMillis: Long, currentEpochMillis: Long = System.currentTimeMillis()): Boolean {
        return triggerEpochMillis > currentEpochMillis
    }

    /**
     * Resolve the effective application deadline date for a notice.
     */
    fun resolveDeadlineDate(notice: NoticeModel): LocalDate? {
        return notice.applicationDeadline
            ?: notice.feePaymentDeadline
            ?: notice.importantDates.find { it.dateType == DateType.APPLICATION_DEADLINE }?.eventDate
    }

    /**
     * Schedule T-48h and T-24h offline alarms for a single notice based on user preferences.
     */
    fun scheduleDeadlineAlarms(
        context: Context,
        notice: NoticeModel,
        prefs: UserPreferences,
        currentEpochMillis: Long = System.currentTimeMillis()
    ) {
        if (!prefs.notifyDeadlines) {
            cancelDeadlineAlarms(context, notice.id)
            return
        }

        // If user follows specific exams, ensure notice matches followed exams
        if (prefs.followedExams.isNotEmpty() && !prefs.followedExams.contains(notice.examId)) {
            cancelDeadlineAlarms(context, notice.id)
            return
        }

        val deadline = resolveDeadlineDate(notice) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intervals = mutableListOf<Int>()
        if (prefs.reminder48Hours) intervals.add(48)
        if (prefs.reminder1Day) intervals.add(24)

        for (hoursBefore in intervals) {
            val triggerMillis = computeTriggerEpochMillis(deadline, hoursBefore)

            if (shouldScheduleTrigger(triggerMillis, currentEpochMillis)) {
                val requestCode = getRequestCode(notice.id, hoursBefore)
                val intent = Intent(context, DeadlineAlarmReceiver::class.java).apply {
                    action = DeadlineAlarmReceiver.ACTION_DEADLINE_ALARM
                    putExtra(DeadlineAlarmReceiver.EXTRA_NOTICE_ID, notice.id)
                    putExtra(DeadlineAlarmReceiver.EXTRA_EXAM_NAME, notice.examName)
                    putExtra(DeadlineAlarmReceiver.EXTRA_ORGANIZATION, notice.organization)
                    putExtra(DeadlineAlarmReceiver.EXTRA_HOURS_LEFT, hoursBefore)
                    putExtra(DeadlineAlarmReceiver.EXTRA_DEADLINE_DATE, deadline.toString())
                    putExtra(
                        DeadlineAlarmReceiver.EXTRA_APPLY_URL,
                        notice.officialApplyUrl ?: notice.officialNoticeUrl
                    )
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                scheduleAlarmInternal(alarmManager, triggerMillis, pendingIntent)
                Log.d(TAG, "Scheduled T-${hoursBefore}h alarm for ${notice.examName} at $triggerMillis (code: $requestCode)")
            } else {
                // Interval already expired; ensure pending intent is cancelled
                cancelAlarmInternal(context, alarmManager, notice.id, hoursBefore)
            }
        }
    }

    /**
     * Cancel both T-48h and T-24h alarms for a given notice.
     */
    fun cancelDeadlineAlarms(context: Context, noticeId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        cancelAlarmInternal(context, alarmManager, noticeId, 48)
        cancelAlarmInternal(context, alarmManager, noticeId, 24)
    }

    private fun cancelAlarmInternal(
        context: Context,
        alarmManager: AlarmManager,
        noticeId: String,
        hoursBefore: Int
    ) {
        val requestCode = getRequestCode(noticeId, hoursBefore)
        val intent = Intent(context, DeadlineAlarmReceiver::class.java).apply {
            action = DeadlineAlarmReceiver.ACTION_DEADLINE_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled T-${hoursBefore}h alarm for notice $noticeId")
        }
    }

    /**
     * Iterate through all given notices and schedule approaching alarms.
     */
    fun scheduleAllUpcoming(
        context: Context,
        notices: List<NoticeModel>,
        prefs: UserPreferences,
        currentEpochMillis: Long = System.currentTimeMillis()
    ) {
        if (!prefs.notifyDeadlines) {
            notices.forEach { cancelDeadlineAlarms(context, it.id) }
            return
        }

        notices.forEach { notice ->
            scheduleDeadlineAlarms(context, notice, prefs, currentEpochMillis)
        }
    }

    private fun scheduleAlarmInternal(
        alarmManager: AlarmManager,
        triggerMillis: Long,
        pendingIntent: PendingIntent
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission denied; falling back to setAndAllowWhileIdle: ${e.message}")
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Failed to schedule deadline alarm: ${fallbackEx.message}")
            }
        }
    }
}
