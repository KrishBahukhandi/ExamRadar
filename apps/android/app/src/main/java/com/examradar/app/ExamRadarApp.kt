package com.examradar.app

import android.app.Application
import android.util.Log
import com.examradar.app.data.cache.OfflineDiskCache
import com.examradar.app.data.preferences.UserPreferencesRepository
import com.examradar.app.data.remote.ExamRadarApiClient
import com.examradar.app.data.repository.ExamRadarRepository
import com.examradar.app.data.repository.ExamRadarRepositoryImpl
import com.examradar.app.notification.DeadlineCheckWorker
import com.examradar.app.notification.ExamRadarNotificationManager
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ExamRadarApp : Application() {

    val apiClient: ExamRadarApiClient = ExamRadarApiClient()

    lateinit var repository: ExamRadarRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val prefsRepository = UserPreferencesRepository(applicationContext)
        val diskCache = OfflineDiskCache(applicationContext)
        val database = com.examradar.app.data.local.ExamRadarDatabase.getInstance(applicationContext)
        repository = ExamRadarRepositoryImpl(
            preferencesRepository = prefsRepository,
            apiClient = apiClient,
            diskCache = diskCache,
            database = database,
            context = applicationContext
        )

        // 1. Initialize Android Notification Channels
        ExamRadarNotificationManager.initNotificationChannels(this)

        // 2. Schedule periodic background deadline reminder checks
        DeadlineCheckWorker.schedulePeriodicCheck(this)

        // 3. Schedule Smart Offline Hardware Deadline Alarms (T-48h and T-24h)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notices = repository.getNotices().first()
                val prefs = repository.getUserPreferences().first()
                com.examradar.app.notification.DeadlineAlarmScheduler.scheduleAllUpcoming(
                    context = this@ExamRadarApp,
                    notices = notices,
                    prefs = prefs
                )
            } catch (e: Exception) {
                Log.w("ExamRadarApp", "Failed to schedule offline deadline alarms on launch: ${e.message}")
            }
        }

        // 4. Update Home Screen Countdown & Radar Widgets
        com.examradar.app.widget.ExamRadarWidgetProvider.triggerUpdate(this)

        // 5. Subscribe to all-notices broadcast topic and register FCM Push token
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseMessaging.getInstance().subscribeToTopic("notices_all")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.i("ExamRadarApp", "Subscribed to global FCM topic: notices_all")
                        }
                    }

                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        Log.i("ExamRadarApp", "FCM Device Token: $token")
                        CoroutineScope(Dispatchers.IO).launch {
                            val prefs = repository.getUserPreferences().first()
                            val authToken = prefs.authToken
                            if (!authToken.isNullOrBlank()) {
                                apiClient.registerDeviceToken(authToken, token)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("ExamRadarApp", "Firebase not available or disabled: ${e.message}")
            }
        }
    }
}
