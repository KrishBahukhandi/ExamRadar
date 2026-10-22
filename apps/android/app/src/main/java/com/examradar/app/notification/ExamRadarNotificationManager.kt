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

            notificationManager.createNotificationChannels(listOf(alertsChannel, deadlinesChannel))
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

        // Action button: View Official PDF/Notice if URL present
        if (!officialNoticeUrl.isNullOrBlank()) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(officialNoticeUrl)).apply {
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
}
