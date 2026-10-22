package com.examradar.app.notification

import android.content.Context
import androidx.work.*
import com.examradar.app.ExamRadarApp
import com.examradar.app.data.model.DateType
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class DeadlineCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? ExamRadarApp ?: return Result.success()
        val repository = app.repository

        val prefs = repository.getUserPreferences().first()
        if (!prefs.notifyDeadlines) {
            return Result.success()
        }

        val followedExams = prefs.followedExams
        if (followedExams.isEmpty()) {
            return Result.success()
        }

        val notices = repository.getNotices().first()
        val today = LocalDate.now()

        notices.forEach { notice ->
            if (followedExams.contains(notice.examId)) {
                val deadline = notice.applicationDeadline
                    ?: notice.importantDates.find { it.dateType == DateType.APPLICATION_DEADLINE }?.eventDate

                if (deadline != null && !deadline.isBefore(today)) {
                    val daysRemaining = ChronoUnit.DAYS.between(today, deadline)

                    val isInterestedInterval = (daysRemaining == 7L && prefs.reminder7Days) ||
                            (daysRemaining == 3L && prefs.reminder3Days) ||
                            (daysRemaining == 1L && prefs.reminder1Day)

                    if (isInterestedInterval) {
                        ExamRadarNotificationManager.showDeadlineReminderNotification(
                            context = applicationContext,
                            notificationId = (notice.examId + deadline.toString()).hashCode(),
                            examName = notice.examName,
                            daysRemaining = daysRemaining,
                            deadlineDateStr = deadline.toString(),
                            examId = notice.examId
                        )
                    }
                }
            }
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "examradar_deadline_checker"

        fun schedulePeriodicCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<DeadlineCheckWorker>(
                repeatInterval = 12,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
