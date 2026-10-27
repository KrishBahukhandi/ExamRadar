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
        repository = ExamRadarRepositoryImpl(prefsRepository, apiClient, diskCache = diskCache, database = database)

        // 1. Initialize Android Notification Channels
        ExamRadarNotificationManager.initNotificationChannels(this)

        // 2. Schedule periodic background deadline reminder checks
        DeadlineCheckWorker.schedulePeriodicCheck(this)

        // 3. Register FCM Push token if already authenticated
        CoroutineScope(Dispatchers.IO).launch {
            try {
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
