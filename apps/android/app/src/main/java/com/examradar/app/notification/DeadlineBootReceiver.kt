package com.examradar.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.examradar.app.ExamRadarApp
import com.examradar.app.data.local.ExamRadarDatabase
import com.examradar.app.data.local.toDomainModel
import com.examradar.app.data.model.NoticeModel
import com.examradar.app.data.preferences.UserPreferences
import com.examradar.app.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Reschedules all active T-48h and T-24h offline alarms whenever the device is rebooted
 * or when the application package is upgraded.
 * Protected by android.permission.RECEIVE_BOOT_COMPLETED.
 */
class DeadlineBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "DeadlineBootReceiver triggered with action: $action")

        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON"
        )

        if (action !in validActions) {
            return
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val app = appContext as? ExamRadarApp

                val notices: List<NoticeModel>
                val prefs: UserPreferences

                if (app != null) {
                    val repo = app.repository
                    notices = repo.getNotices().first()
                    prefs = repo.getUserPreferences().first()
                } else {
                    // Fallback direct disk read if application singleton is not fully ready
                    val prefsRepo = UserPreferencesRepository(appContext)
                    val db = ExamRadarDatabase.getInstance(appContext)
                    notices = db.noticeDao().getAllNoticesList().map { it.toDomainModel() }
                    prefs = prefsRepo.userPreferences.first()
                }

                Log.i(TAG, "Re-registering offline deadline alarms for ${notices.size} notices on boot")
                DeadlineAlarmScheduler.scheduleAllUpcoming(appContext, notices, prefs)
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling deadline alarms after boot: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "DeadlineBootReceiver"
    }
}
