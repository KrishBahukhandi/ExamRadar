package com.examradar.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.examradar.app.MainActivity
import com.examradar.app.R
import com.examradar.app.data.local.ExamRadarDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class ExamRadarWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Immediate synchronous visual setup to prevent launcher bind timeout
        for (appWidgetId in appWidgetIds) {
            try {
                val initialViews = RemoteViews(context.packageName, R.layout.widget_exam_countdown)
                initialViews.setTextViewText(R.id.widget_target_exam, "ExamRadar")
                initialViews.setTextViewText(R.id.widget_notice_title, "Tracking official notices...")
                appWidgetManager.updateAppWidget(appWidgetId, initialViews)
            } catch (_: Exception) {}
        }

        // Asynchronous Room DB hydration and pending intent binding
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun triggerUpdate(context: Context) {
            try {
                val intent = Intent(context, ExamRadarWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                        ComponentName(context, ExamRadarWidgetProvider::class.java)
                    )
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            } catch (_: Exception) {}
        }

        suspend fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) = withContext(Dispatchers.IO) {
            val views = RemoteViews(context.packageName, R.layout.widget_exam_countdown)
            val db = ExamRadarDatabase.getInstance(context)

            val exams = runCatching { db.examDao().getAllExamsList() }.getOrDefault(emptyList())
            val notices = runCatching { db.noticeDao().getAllNoticesList() }.getOrDefault(emptyList())

            val followedExams = exams.filter { it.isFollowing }.ifEmpty { exams }
            val followedIds = followedExams.map { it.id }.toSet()

            // 1. Calculate Imminent Countdown
            val today = LocalDate.now()
            val upcomingNoticeWithExamDate = notices
                .filter { it.examId in followedIds && it.examDate != null && !it.examDate.isBefore(today) }
                .minByOrNull { it.examDate!! }

            val primaryExam = followedExams.firstOrNull { it.id == upcomingNoticeWithExamDate?.examId }
                ?: followedExams.firstOrNull()

            val examDate = upcomingNoticeWithExamDate?.examDate
                ?: LocalDate.of(2026, 5, 24) // Default fallback to UPSC CSE Prelims 2026
            val daysRemaining = ChronoUnit.DAYS.between(today, examDate).coerceAtLeast(0)

            val examTitle = primaryExam?.examCode ?: primaryExam?.examName ?: "UPSC CSE"
            val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

            views.setTextViewText(R.id.widget_days_count, daysRemaining.toString())
            views.setTextViewText(R.id.widget_target_exam, examTitle)
            views.setTextViewText(
                R.id.widget_exam_date,
                if (upcomingNoticeWithExamDate?.examDate != null) {
                    "Exam: ${examDate.format(dateFormatter)}"
                } else {
                    "Scheduled 2026"
                }
            )

            // 2. Populate Latest Breaking Notice
            val latestNotice = notices
                .filter { it.examId in followedIds }
                .maxByOrNull { it.publishedDate ?: LocalDate.MIN }
                ?: notices.firstOrNull()

            if (latestNotice != null) {
                views.setTextViewText(R.id.widget_notice_type, "${latestNotice.noticeType} • VERIFIED")
                views.setTextViewText(R.id.widget_notice_title, latestNotice.title)
                views.setTextViewText(R.id.widget_notice_org, "${latestNotice.organization} • Official Portal")
            } else {
                views.setTextViewText(R.id.widget_notice_type, "OFFICIAL NOTICE")
                views.setTextViewText(R.id.widget_notice_title, "Track 100% verified notifications")
                views.setTextViewText(R.id.widget_notice_org, "ExamRadar India")
            }

            // 3. Set Up PendingIntents for Deep Linking
            // Tapping header -> open home
            val homeIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val homePendingIntent = PendingIntent.getActivity(
                context,
                0,
                homeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_header, homePendingIntent)

            // Tapping countdown box -> open Study Hub / study screen deep link
            val studyIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("examradar://study")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val studyPendingIntent = PendingIntent.getActivity(
                context,
                1,
                studyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_countdown_container, studyPendingIntent)

            // Tapping notice box -> open Notice Detail deep link
            val noticeId = latestNotice?.id
            val noticeIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = if (noticeId != null) Uri.parse("examradar://notice/$noticeId") else Uri.parse("examradar://notices")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val noticePendingIntent = PendingIntent.getActivity(
                context,
                2,
                noticeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_notice_container, noticePendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

