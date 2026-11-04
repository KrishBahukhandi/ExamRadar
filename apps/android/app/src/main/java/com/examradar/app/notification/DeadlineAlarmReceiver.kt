package com.examradar.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver triggered by Android AlarmManager RTC_WAKEUP hardware alarms
 * at T-48h and T-24h before official examination application deadlines.
 * Operates 100% offline with zero network connectivity required.
 */
class DeadlineAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "DeadlineAlarmReceiver received action: $action")

        if (action != ACTION_DEADLINE_ALARM) {
            return
        }

        val noticeId = intent.getStringExtra(EXTRA_NOTICE_ID) ?: return
        val examName = intent.getStringExtra(EXTRA_EXAM_NAME) ?: "Examination"
        val organization = intent.getStringExtra(EXTRA_ORGANIZATION) ?: "Official Authority"
        val hoursLeft = intent.getIntExtra(EXTRA_HOURS_LEFT, 24)
        val deadlineDateStr = intent.getStringExtra(EXTRA_DEADLINE_DATE) ?: "Today"
        val applyUrl = intent.getStringExtra(EXTRA_APPLY_URL)

        Log.i(TAG, "Triggering offline deadline alarm for notice: $noticeId, exam: $examName ($hoursLeft hours left)")

        val notificationId = (noticeId.hashCode() * 31 + hoursLeft) and 0x7FFFFFFF

        ExamRadarNotificationManager.showOfflineDeadlineAlarmNotification(
            context = context,
            notificationId = notificationId,
            noticeId = noticeId,
            examName = examName,
            organization = organization,
            hoursLeft = hoursLeft,
            deadlineDateStr = deadlineDateStr,
            officialApplyUrl = applyUrl
        )

        // Refresh home screen countdown widget
        try {
            com.examradar.app.widget.ExamRadarWidgetProvider.triggerUpdate(context)
        } catch (e: Exception) {
            Log.w(TAG, "Widget update failed from alarm receiver: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "DeadlineAlarmReceiver"

        const val ACTION_DEADLINE_ALARM = "com.examradar.app.action.DEADLINE_ALARM"

        const val EXTRA_NOTICE_ID = "extra_notice_id"
        const val EXTRA_EXAM_NAME = "extra_exam_name"
        const val EXTRA_ORGANIZATION = "extra_organization"
        const val EXTRA_HOURS_LEFT = "extra_hours_left"
        const val EXTRA_DEADLINE_DATE = "extra_deadline_date"
        const val EXTRA_APPLY_URL = "extra_apply_url"
    }
}
