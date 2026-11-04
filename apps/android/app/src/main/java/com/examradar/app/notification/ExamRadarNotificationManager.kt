package com.examradar.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.examradar.app.MainActivity
import com.examradar.app.R

object ExamRadarNotificationManager {

    const val CHANNEL_ALERTS_ID = "examradar_official_alerts"
    const val CHANNEL_ALERTS_NAME = "Official Examination Announcements"
    const val CHANNEL_ALERTS_DESC = "Instant verified announcements from official exam gazettes and portals (SSC, IBPS, UPSC)"

    const val CHANNEL_DEADLINES_ID = "examradar_deadlines"
    const val CHANNEL_DEADLINES_NAME = "Application Deadlines & Countdown"
    const val CHANNEL_DEADLINES_DESC = "Critical 7-day, 3-day, and 1-day reminders before registration and fee deadlines close"

    const val CHANNEL_DEADLINE_ALARMS_ID = "examradar_deadline_alarms"
    const val CHANNEL_DEADLINE_ALARMS_NAME = "Urgent Application Deadlines (Offline Alarms)"
    const val CHANNEL_DEADLINE_ALARMS_DESC = "Hardware clock alarms at T-48h and T-24h to prevent missing application deadlines under server traffic spikes"

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                CHANNEL_ALERTS_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_ALERTS_DESC
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            val deadlinesChannel = NotificationChannel(
                CHANNEL_DEADLINES_ID,
                CHANNEL_DEADLINES_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DEADLINES_DESC
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            val deadlineAlarmsChannel = NotificationChannel(
                CHANNEL_DEADLINE_ALARMS_ID,
                CHANNEL_DEADLINE_ALARMS_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DEADLINE_ALARMS_DESC
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 1000)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(listOf(alertsChannel, deadlinesChannel, deadlineAlarmsChannel))
        }
    }

    fun showNoticeNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        noticeId: String?,
        officialNoticeUrl: String? = null
    ) {
        initNotificationChannels(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        // Tap action: Open MainActivity deep link to notice detail
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            if (!noticeId.isNullOrBlank()) {
                data = Uri.parse("https://app.examradar.in/notices/$noticeId")
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Action button: View Official PDF/Notice if valid HTTP/HTTPS URL present
        if (!officialNoticeUrl.isNullOrBlank()) {
            val parsedUri = Uri.parse(officialNoticeUrl)
            val scheme = parsedUri.scheme?.lowercase()
            if (scheme == "https" || scheme == "http") {
                val webIntent = Intent(Intent.ACTION_VIEW, parsedUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val webPendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId + 10000,
                    webIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(
                    android.R.drawable.ic_menu_view,
                    "Official Notice",
                    webPendingIntent
                )
            }
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Handled when permission is revoked
        }
    }

    fun showDeadlineReminderNotification(
        context: Context,
        notificationId: Int,
        examName: String,
        daysRemaining: Long,
        deadlineDateStr: String,
        examId: String?
    ) {
        initNotificationChannels(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val title = when (daysRemaining) {
            1L -> "⚠️ Final 24 Hours: $examName"
            3L -> "⏳ 3 Days Remaining: $examName"
            7L -> "📅 1 Week Remaining: $examName"
            else -> "⏰ Application Deadline Approaching: $examName"
        }

        val body = "Registration and fee submission closes on $deadlineDateStr. Ensure all checklist steps are complete."

        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_DEADLINES_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Handled when permission is revoked
        }
    }

    fun showOfflineDeadlineAlarmNotification(
        context: Context,
        notificationId: Int,
        noticeId: String,
        examName: String,
        organization: String,
        hoursLeft: Int,
        deadlineDateStr: String,
        officialApplyUrl: String? = null
    ) {
        initNotificationChannels(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val title = if (hoursLeft <= 24) {
            "🚨 FINAL 24 HOURS: $examName ($organization)"
        } else {
            "⚠️ 48 Hours Left: $examName ($organization)"
        }

        val body = if (hoursLeft <= 24) {
            "Application & fee window closes tomorrow ($deadlineDateStr) at 5:00 PM. Server queues freeze on closing day—submit now!"
        } else {
            "Application deadline closes on $deadlineDateStr at 5:00 PM. Official servers will slow down soon. Complete registration now."
        }

        // Tap action: Open MainActivity deep link to notice detail
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            if (noticeId.isNotBlank()) {
                data = Uri.parse("https://app.examradar.in/notices/$noticeId")
            }
            putExtra("notice_id", noticeId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_DEADLINE_ALARMS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText("$body\n\n💡 Tip: Keep scanned photo, signature, and payment card ready.")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 1000))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Action 1: "Apply on Portal" (if officialApplyUrl is available)
        if (!officialApplyUrl.isNullOrBlank()) {
            val parsedUri = Uri.parse(officialApplyUrl)
            val scheme = parsedUri.scheme?.lowercase()
            if (scheme == "https" || scheme == "http") {
                val webIntent = Intent(Intent.ACTION_VIEW, parsedUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val webPendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId + 20000,
                    webIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(
                    android.R.drawable.ic_menu_send,
                    "Apply on Portal",
                    webPendingIntent
                )
            }
        }

        // Action 2: "View Notice"
        builder.addAction(
            android.R.drawable.ic_menu_info_details,
            "View Details",
            pendingIntent
        )

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Handled when permission is revoked
        }
    }
}
