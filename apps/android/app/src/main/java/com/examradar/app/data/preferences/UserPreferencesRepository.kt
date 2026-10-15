package com.examradar.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "examradar_user_prefs")

data class UserPreferences(
    val isOnboardingCompleted: Boolean = false,
    val followedCategories: Set<String> = emptySet(),
    val followedExams: Set<String> = emptySet(),
    val educationLevel: String = "GRADUATE_ANY",
    val graduationStatus: String = "GRADUATED",
    val graduationYear: Int = 2024,
    val notifyNewNotices: Boolean = true,
    val notifyDeadlines: Boolean = true,
    val notifyResults: Boolean = true,
    val reminder7Days: Boolean = true,
    val reminder3Days: Boolean = true,
    val reminder1Day: Boolean = true,
    val pushEnabled: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val FOLLOWED_CATEGORIES = stringSetPreferencesKey("followed_categories")
        val FOLLOWED_EXAMS = stringSetPreferencesKey("followed_exams")
        val EDUCATION_LEVEL = stringPreferencesKey("education_level")
        val GRADUATION_STATUS = stringPreferencesKey("graduation_status")
        val GRADUATION_YEAR = intPreferencesKey("graduation_year")
        val NOTIFY_NEW_NOTICES = booleanPreferencesKey("notify_new_notices")
        val NOTIFY_DEADLINES = booleanPreferencesKey("notify_deadlines")
        val NOTIFY_RESULTS = booleanPreferencesKey("notify_results")
        val REMINDER_7D = booleanPreferencesKey("reminder_7d")
        val REMINDER_3D = booleanPreferencesKey("reminder_3d")
        val REMINDER_1D = booleanPreferencesKey("reminder_1d")
        val PUSH_ENABLED = booleanPreferencesKey("push_enabled")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            isOnboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            followedCategories = prefs[Keys.FOLLOWED_CATEGORIES] ?: setOf("cat-upsc", "cat-ssc", "cat-banking"),
            followedExams = prefs[Keys.FOLLOWED_EXAMS] ?: setOf("exam-upsc-cse", "exam-ssc-cgl", "exam-ibps-po"),
            educationLevel = prefs[Keys.EDUCATION_LEVEL] ?: "GRADUATE_ANY",
            graduationStatus = prefs[Keys.GRADUATION_STATUS] ?: "GRADUATED",
            graduationYear = prefs[Keys.GRADUATION_YEAR] ?: 2024,
            notifyNewNotices = prefs[Keys.NOTIFY_NEW_NOTICES] ?: true,
            notifyDeadlines = prefs[Keys.NOTIFY_DEADLINES] ?: true,
            notifyResults = prefs[Keys.NOTIFY_RESULTS] ?: true,
            reminder7Days = prefs[Keys.REMINDER_7D] ?: true,
            reminder3Days = prefs[Keys.REMINDER_3D] ?: true,
            reminder1Day = prefs[Keys.REMINDER_1D] ?: true,
            pushEnabled = prefs[Keys.PUSH_ENABLED] ?: true
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun updateFollowedCategories(categories: Set<String>) {
        context.dataStore.edit { it[Keys.FOLLOWED_CATEGORIES] = categories }
    }

    suspend fun updateFollowedExams(exams: Set<String>) {
        context.dataStore.edit { it[Keys.FOLLOWED_EXAMS] = exams }
    }

    suspend fun toggleFollowExam(examId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FOLLOWED_EXAMS] ?: emptySet()
            val updated = if (current.contains(examId)) current - examId else current + examId
            prefs[Keys.FOLLOWED_EXAMS] = updated
        }
    }

    suspend fun updateProfile(educationLevel: String, graduationStatus: String, graduationYear: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EDUCATION_LEVEL] = educationLevel
            prefs[Keys.GRADUATION_STATUS] = graduationStatus
            prefs[Keys.GRADUATION_YEAR] = graduationYear
        }
    }

    suspend fun updateNotificationPreferences(
        notifyNew: Boolean,
        notifyDeadlines: Boolean,
        notifyResults: Boolean,
        r7: Boolean,
        r3: Boolean,
        r1: Boolean,
        push: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFY_NEW_NOTICES] = notifyNew
            prefs[Keys.NOTIFY_DEADLINES] = notifyDeadlines
            prefs[Keys.NOTIFY_RESULTS] = notifyResults
            prefs[Keys.REMINDER_7D] = r7
            prefs[Keys.REMINDER_3D] = r3
            prefs[Keys.REMINDER_1D] = r1
            prefs[Keys.PUSH_ENABLED] = push
        }
    }

    suspend fun clearAllData() {
        context.dataStore.edit { it.clear() }
    }
}
