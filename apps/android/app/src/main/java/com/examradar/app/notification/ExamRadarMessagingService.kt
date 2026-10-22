package com.examradar.app.notification

import android.util.Log
import com.examradar.app.ExamRadarApp
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ExamRadarMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "New Firebase Cloud Messaging device token received: $token")

        val app = application as? ExamRadarApp ?: return
        val repository = app.repository

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = repository.getUserPreferences().first()
                val authToken = prefs.authToken
                if (!authToken.isNullOrBlank()) {
                    app.apiClient.registerDeviceToken(authToken, token)
                    Log.i(TAG, "FCM token registered with ExamRadar cloud backend")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error registering FCM token: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val title = data["title"]
            ?: remoteMessage.notification?.title
            ?: "ExamRadar Alert"

        val body = data["body"]
            ?: remoteMessage.notification?.body
            ?: "Official examination announcement published."

        val noticeId = data["notice_id"]
        val officialUrl = data["official_url"]
        val notificationType = data["type"] ?: "NOTICE"

        val notificationId = (noticeId?.hashCode() ?: System.currentTimeMillis().toInt())

        when (notificationType) {
            "DEADLINE" -> {
                val examName = data["exam_name"] ?: "Upcoming Examination"
                val daysRemaining = data["days_remaining"]?.toLongOrNull() ?: 1L
                val deadlineDate = data["deadline_date"] ?: "soon"
                val examId = data["exam_id"]

                ExamRadarNotificationManager.showDeadlineReminderNotification(
                    context = applicationContext,
                    notificationId = notificationId,
                    examName = examName,
                    daysRemaining = daysRemaining,
                    deadlineDateStr = deadlineDate,
                    examId = examId
                )
            }
            else -> {
                ExamRadarNotificationManager.showNoticeNotification(
                    context = applicationContext,
                    notificationId = notificationId,
                    title = title,
                    body = body,
                    noticeId = noticeId,
                    officialNoticeUrl = officialUrl
                )
            }
        }
    }

    companion object {
        private const val TAG = "ExamRadarMessaging"
    }
}
