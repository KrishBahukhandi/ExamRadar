package com.examradar.app.data.preferences

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
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
    val pushEnabled: Boolean = true,
    val isLoggedIn: Boolean = false,
    val userId: String? = null,
    val userEmail: String? = null,
    val userFullName: String? = null,
    val authToken: String? = null,
    val refreshToken: String? = null
)

class UserPreferencesRepository(private val context: Context) {

    private val securePrefs by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "examradar_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e("UserPreferencesRepo", "Failed to initialize EncryptedSharedPreferences", e)
            null
        }
    }

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
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_FULL_NAME = stringPreferencesKey("user_full_name")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val secureToken = try {
            securePrefs?.getString("auth_token", null)
        } catch (e: Exception) {
            Log.w("UserPreferencesRepo", "Error reading encrypted token", e)
            null
        }

        val secureRefreshToken = try {
            securePrefs?.getString("refresh_token", null)
        } catch (e: Exception) {
            Log.w("UserPreferencesRepo", "Error reading encrypted refresh token", e)
            null
        }

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
            pushEnabled = prefs[Keys.PUSH_ENABLED] ?: true,
            isLoggedIn = prefs[Keys.IS_LOGGED_IN] ?: false,
            userId = prefs[Keys.USER_ID],
            userEmail = prefs[Keys.USER_EMAIL],
            userFullName = prefs[Keys.USER_FULL_NAME],
            authToken = secureToken,
            refreshToken = secureRefreshToken
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

    suspend fun saveUserSession(userId: String, email: String, fullName: String, token: String, refreshToken: String? = null) {
        var encryptedSuccessfully = false
        try {
            if (securePrefs != null) {
                val editor = securePrefs?.edit()?.putString("auth_token", token)
                if (refreshToken != null) {
                    editor?.putString("refresh_token", refreshToken)
                }
                editor?.apply()
                encryptedSuccessfully = true
            }
        } catch (e: Exception) {
            Log.e("UserPreferencesRepo", "Failed to store encrypted auth token", e)
        }
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = true
            prefs[Keys.USER_ID] = userId
            prefs[Keys.USER_EMAIL] = email
            prefs[Keys.USER_FULL_NAME] = fullName
            if (encryptedSuccessfully) {
                prefs.remove(Keys.AUTH_TOKEN)
                prefs.remove(Keys.REFRESH_TOKEN)
            } else {
                prefs[Keys.AUTH_TOKEN] = token
                if (refreshToken != null) prefs[Keys.REFRESH_TOKEN] = refreshToken
            }
        }
    }

    suspend fun updateAuthToken(token: String, refreshToken: String? = null) {
        try {
            val editor = securePrefs?.edit()?.putString("auth_token", token)
            if (refreshToken != null) editor?.putString("refresh_token", refreshToken)
            editor?.apply()
        } catch (e: Exception) {
            Log.e("UserPreferencesRepo", "Failed to write encrypted auth token", e)
        }
        // Purge any legacy plaintext tokens from DataStore so secrets are never in plaintext
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.AUTH_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
        }
    }

    suspend fun clearUserSession() {
        try {
            securePrefs?.edit()?.remove("auth_token")?.remove("refresh_token")?.apply()
        } catch (e: Exception) {
            Log.e("UserPreferencesRepo", "Failed to remove encrypted auth token", e)
        }
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = false
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USER_EMAIL)
            prefs.remove(Keys.USER_FULL_NAME)
            prefs.remove(Keys.AUTH_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
        }
    }

    fun getCompletedChecklistItems(examId: String): Flow<Set<String>> =
        context.dataStore.data.map { prefs ->
            prefs[stringSetPreferencesKey("checklist_$examId")] ?: emptySet()
        }

    suspend fun toggleChecklistItem(examId: String, itemKey: String): Set<String> {
        val key = stringSetPreferencesKey("checklist_$examId")
        var updated = emptySet<String>()
        context.dataStore.edit { prefs ->
            val current = prefs[key] ?: emptySet()
            updated = if (current.contains(itemKey)) current - itemKey else current + itemKey
            prefs[key] = updated
        }
        return updated
    }

    suspend fun setChecklistItems(examId: String, items: Set<String>) {
        val key = stringSetPreferencesKey("checklist_$examId")
        context.dataStore.edit { prefs ->
            prefs[key] = items
        }
    }

    suspend fun clearAllData() {
        try {
            securePrefs?.edit()?.clear()?.apply()
        } catch (e: Exception) {
            Log.e("UserPreferencesRepo", "Failed to clear encrypted prefs", e)
        }
        context.dataStore.edit { it.clear() }
    }
}
