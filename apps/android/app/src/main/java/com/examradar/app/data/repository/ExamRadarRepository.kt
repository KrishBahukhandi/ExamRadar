package com.examradar.app.data.repository

import android.util.Log
import com.examradar.app.data.local.*
import com.examradar.app.data.cache.OfflineDiskCache
import com.examradar.app.data.fixture.DevelopmentNoticeFixtures
import com.examradar.app.data.model.*
import com.examradar.app.data.preferences.UserPreferences
import com.examradar.app.data.preferences.UserPreferencesRepository
import com.examradar.app.data.remote.ExamRadarApiClient
import com.examradar.app.data.remote.UserPreferencesRemoteDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Segregated repository contracts adhering to Interface Segregation Principle (ISP).
 * Enables modular testing, independent ViewModel scoping, and future multi-module migration.
 */
interface NoticeRepository {
    fun getNotices(
        typeFilter: NoticeType? = null,
        categoryFilter: String? = null,
        onlyApproachingDeadlines: Boolean = false,
        searchQuery: String? = null
    ): Flow<List<NoticeModel>>
    fun getNoticeById(noticeId: String): Flow<NoticeModel?>
    fun getCalendarEvents(): Flow<List<CalendarEventModel>>
    fun getSourceFreshness(): Flow<SourceFreshnessInfo>
    suspend fun refreshNotices(): Boolean
}

interface ExamCatalogRepository {
    fun getCategories(): Flow<List<CategoryModel>>
    fun getExams(categoryId: String? = null, searchQuery: String? = null): Flow<List<ExamModel>>
    fun getExamById(examId: String): Flow<ExamModel?>
}

interface UserPreferencesDomainRepository {
    fun getUserPreferences(): Flow<UserPreferences>
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun updateFollowedCategories(categories: Set<String>)
    suspend fun updateFollowedExams(exams: Set<String>)
    suspend fun toggleFollowExam(examId: String)
    suspend fun updateProfile(educationLevel: String, graduationStatus: String, graduationYear: Int)
    suspend fun updateNotificationPreferences(
        notifyNew: Boolean,
        notifyDeadlines: Boolean,
        notifyResults: Boolean,
        r7: Boolean,
        r3: Boolean,
        r1: Boolean,
        push: Boolean
    )
    suspend fun resetAllUserData()
}

interface ChecklistRepository {
    fun getChecklist(examId: String): Flow<ChecklistModel>
    fun toggleChecklistItem(examId: String, itemKey: String)
}

interface AuthRepository {
    suspend fun signUp(email: String, password: String, fullName: String): com.examradar.app.data.remote.AuthResult
    suspend fun signIn(email: String, password: String): com.examradar.app.data.remote.AuthResult
    suspend fun signOut()
}

/**
 * Composite repository contract combining domain sub-repositories.
 * Preserves binary compatibility with existing ViewModel factories.
 */
interface ExamRadarRepository : NoticeRepository, ExamCatalogRepository, UserPreferencesDomainRepository, ChecklistRepository, AuthRepository

class ExamRadarRepositoryImpl(
    private val preferencesRepository: UserPreferencesRepository,
    private val apiClient: ExamRadarApiClient = ExamRadarApiClient(),
    private val authService: com.examradar.app.data.remote.SupabaseAuthService = com.examradar.app.data.remote.SupabaseAuthService(),
    private val diskCache: OfflineDiskCache? = null,
    private val database: ExamRadarDatabase? = null
) : ExamRadarRepository {

    private val liveCategories = MutableStateFlow<List<CategoryModel>>(
        diskCache?.loadCategories()?.takeIf { it.isNotEmpty() } ?: DevelopmentNoticeFixtures.categories
    )
    private val liveExams = MutableStateFlow<List<ExamModel>>(
        diskCache?.loadExams()?.takeIf { it.isNotEmpty() } ?: DevelopmentNoticeFixtures.exams
    )
    private val liveNotices = MutableStateFlow<List<NoticeModel>>(
        diskCache?.loadNotices()?.takeIf { it.isNotEmpty() } ?: DevelopmentNoticeFixtures.sampleNotices
    )

    init {
        CoroutineScope(Dispatchers.IO).launch {
            if (database != null) {
                runCatching {
                    val dbCats = database.categoryDao().getAllCategoriesList()
                    if (dbCats.isNotEmpty()) {
                        liveCategories.value = dbCats.map { it.toDomainModel() }
                    }
                    val dbExams = database.examDao().getAllExamsList()
                    if (dbExams.isNotEmpty()) {
                        liveExams.value = dbExams.map { it.toDomainModel() }
                    }
                    val dbNotices = database.noticeDao().getAllNoticesList()
                    if (dbNotices.isNotEmpty()) {
                        liveNotices.value = dbNotices.map { it.toDomainModel() }
                    }
                }
            }
            refreshNotices()
        }
    }

    override suspend fun refreshNotices(): Boolean =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            var anySuccess = false

            val remoteCategories = apiClient.fetchCategories()
            if (!remoteCategories.isNullOrEmpty()) {
                liveCategories.value = remoteCategories
                diskCache?.saveCategories(remoteCategories)
                database?.categoryDao()?.insertCategories(remoteCategories.map { it.toEntity() })
                anySuccess = true
            }

            val remoteExams = apiClient.fetchExams()
            if (!remoteExams.isNullOrEmpty()) {
                liveExams.value = remoteExams
                diskCache?.saveExams(remoteExams)
                database?.examDao()?.insertExams(remoteExams.map { it.toEntity() })
                anySuccess = true
            }

            val remote = apiClient.fetchNotices()
            if (!remote.isNullOrEmpty()) {
                liveNotices.value = remote
                diskCache?.saveNotices(remote)
                database?.noticeDao()?.insertNotices(remote.map { it.toEntity() })
                anySuccess = true
            }
            anySuccess
        }

    override fun getUserPreferences(): Flow<UserPreferences> =
        preferencesRepository.userPreferences

    override suspend fun setOnboardingCompleted(completed: Boolean) =
        preferencesRepository.setOnboardingCompleted(completed)

    override suspend fun updateFollowedCategories(categories: Set<String>) =
        preferencesRepository.updateFollowedCategories(categories)

    override suspend fun updateFollowedExams(exams: Set<String>) =
        preferencesRepository.updateFollowedExams(exams)

    override suspend fun toggleFollowExam(examId: String) =
        preferencesRepository.toggleFollowExam(examId)

    override suspend fun updateProfile(educationLevel: String, graduationStatus: String, graduationYear: Int) =
        preferencesRepository.updateProfile(educationLevel, graduationStatus, graduationYear)

    override suspend fun updateNotificationPreferences(
        notifyNew: Boolean,
        notifyDeadlines: Boolean,
        notifyResults: Boolean,
        r7: Boolean,
        r3: Boolean,
        r1: Boolean,
        push: Boolean
    ) = preferencesRepository.updateNotificationPreferences(
        notifyNew, notifyDeadlines, notifyResults, r7, r3, r1, push
    )

    override suspend fun resetAllUserData() {
        val token = getValidAuthToken()
        if (token != null) {
            try {
                apiClient.deleteUserData(token)
            } catch (e: Exception) {
                Log.w("ExamRadarRepo", "Cloud user deletion error: ${e.message}")
            }
        }
        diskCache?.clear()
        database?.let { db ->
            runCatching {
                db.categoryDao().clearCategories()
                db.examDao().clearExams()
                db.noticeDao().clearNotices()
            }
        }
        preferencesRepository.clearAllData()
    }

    override fun getCategories(): Flow<List<CategoryModel>> =
        liveCategories

    override fun getExams(categoryId: String?, searchQuery: String?): Flow<List<ExamModel>> =
        combine(preferencesRepository.userPreferences, liveExams, liveNotices) { prefs, allExams, notices ->
            allExams
                .filter { exam ->
                    val matchesCategory = categoryId == null || exam.categoryId == categoryId
                    val matchesSearch = searchQuery.isNullOrBlank() ||
                            exam.examName.contains(searchQuery, ignoreCase = true) ||
                            exam.organization.contains(searchQuery, ignoreCase = true) ||
                            exam.examCode.contains(searchQuery, ignoreCase = true)
                    matchesCategory && matchesSearch
                }
                .map { exam ->
                    val noticeCount = notices.count {
                        it.examId == exam.id ||
                        (exam.id == "exam-ssc-cgl" && it.organization.equals("SSC", ignoreCase = true)) ||
                        (exam.id.contains("ibps") && it.organization.equals("IBPS", ignoreCase = true))
                    }
                    exam.copy(
                        isFollowing = prefs.followedExams.contains(exam.id),
                        activeNoticeCount = if (noticeCount > 0) noticeCount else exam.activeNoticeCount
                    )
                }
        }

    override fun getExamById(examId: String): Flow<ExamModel?> =
        combine(preferencesRepository.userPreferences, liveExams) { prefs, exams ->
            exams.find { it.id == examId }?.copy(isFollowing = prefs.followedExams.contains(examId))
        }

    override fun getNotices(
        typeFilter: NoticeType?,
        categoryFilter: String?,
        onlyApproachingDeadlines: Boolean,
        searchQuery: String?
    ): Flow<List<NoticeModel>> =
        combine(preferencesRepository.userPreferences, liveNotices, liveExams) { prefs, allNotices, allExams ->
            val now = LocalDate.now()
            allNotices
                .filter { notice ->
                    val matchesType = typeFilter == null || notice.noticeType == typeFilter
                    val matchesCategory = categoryFilter == null || run {
                        val exam = allExams.find { it.id == notice.examId }
                        exam?.categoryId == categoryFilter ||
                        (categoryFilter == "cat-ssc" && notice.organization.equals("SSC", ignoreCase = true)) ||
                        (categoryFilter == "cat-banking" && notice.organization.equals("IBPS", ignoreCase = true))
                    }
                    val matchesDeadline = if (onlyApproachingDeadlines) {
                        notice.applicationDeadline != null &&
                                !notice.applicationDeadline.isBefore(now) &&
                                notice.applicationDeadline.isBefore(now.plusDays(10))
                    } else true

                    val matchesSearch = searchQuery.isNullOrBlank() ||
                            notice.title.contains(searchQuery, ignoreCase = true) ||
                            notice.organization.contains(searchQuery, ignoreCase = true) ||
                            notice.examName.contains(searchQuery, ignoreCase = true)

                    matchesType && matchesCategory && matchesDeadline && matchesSearch
                }
                .sortedByDescending { it.publishedDate ?: LocalDate.MIN }
        }

    override fun getNoticeById(noticeId: String): Flow<NoticeModel?> =
        liveNotices.map { list -> list.find { it.id == noticeId } }

    override fun getCalendarEvents(): Flow<List<CalendarEventModel>> =
        combine(preferencesRepository.userPreferences, liveNotices) { prefs, allNotices ->
            val events = mutableListOf<CalendarEventModel>()
            allNotices.forEach { notice ->
                notice.importantDates.forEachIndexed { index, impDate ->
                    events.add(
                        CalendarEventModel(
                            id = "${notice.id}-date-$index",
                            examId = notice.examId,
                            examName = notice.examName,
                            organization = notice.organization,
                            dateType = impDate.dateType,
                            eventDate = impDate.eventDate,
                            isTentative = impDate.isTentative,
                            isRevised = impDate.isRevised,
                            noticeId = notice.id,
                            officialNoticeUrl = notice.officialNoticeUrl
                        )
                    )
                }
            }
            events.sortedBy { it.eventDate }
        }

    override fun getChecklist(examId: String): Flow<ChecklistModel> =
        combine(
            getExamById(examId),
            preferencesRepository.getCompletedChecklistItems(examId)
        ) { exam, completedKeys ->
            val examName = exam?.examName ?: "Examination"
            val standardItems = listOf(
                ChecklistItemModel("item-notification", "Review official notification PDF & rules", completedKeys.contains("item-notification")),
                ChecklistItemModel("item-eligibility", "Verify educational degree & age cut-off criteria", completedKeys.contains("item-eligibility")),
                ChecklistItemModel("item-documents", "Prepare photo, signature & category certificate", completedKeys.contains("item-documents")),
                ChecklistItemModel("item-application", "Complete online registration on official portal", completedKeys.contains("item-application")),
                ChecklistItemModel("item-fee", "Pay applicable online examination fee", completedKeys.contains("item-fee")),
                ChecklistItemModel("item-confirmation", "Save & print application confirmation receipt", completedKeys.contains("item-confirmation"))
            )
            ChecklistModel(examId, examName, standardItems)
        }

    override fun toggleChecklistItem(examId: String, itemKey: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val updatedKeys = preferencesRepository.toggleChecklistItem(examId, itemKey)
            val token = getValidAuthToken()
            if (token != null) {
                // Best-effort sync to cloud
                apiClient.updateChecklistItem(token, examId, itemKey, updatedKeys.contains(itemKey))
            }
        }
    }

    override fun getSourceFreshness(): Flow<SourceFreshnessInfo> =
        liveNotices.map { list ->
            val hasLive = list.any { !it.isDemonstrationFixture }
            val lastSync = diskCache?.getLastSyncTimestamp() ?: 0L
            val isCacheStale = diskCache?.isStale() ?: (!hasLive)
            if (hasLive || lastSync > 0L) {
                SourceFreshnessInfo(
                    totalSources = 3,
                    activeSources = 3,
                    lastSuccessfulSync = if (lastSync > 0L) lastSync else System.currentTimeMillis(),
                    isStale = isCacheStale,
                    statusMessage = if (isCacheStale && lastSync > 0L) {
                        val formattedTime = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(lastSync))
                        "Offline cache active • Last synced $formattedTime"
                    } else {
                        "Live official feeds active: UPSC, SSC & IBPS (${list.size} notices)"
                    }
                )
            } else {
                SourceFreshnessInfo(
                    totalSources = 3,
                    activeSources = 0,
                    lastSuccessfulSync = 0L,
                    isStale = true,
                    statusMessage = "Offline cache active • Reconnecting to official feeds"
                )
            }
        }

    private suspend fun syncCloudState(token: String) {
        try {
            // 1. Restore & merge preferences
            val cloudPrefs = apiClient.fetchUserPreferences(token)
            val localPrefs = preferencesRepository.userPreferences.first()

            val mergedExams = if (!cloudPrefs?.followedExamIds.isNullOrEmpty()) {
                cloudPrefs!!.followedExamIds!!.toSet() + localPrefs.followedExams
            } else {
                localPrefs.followedExams
            }

            val mergedCategories = if (!cloudPrefs?.followedCategoryIds.isNullOrEmpty()) {
                cloudPrefs!!.followedCategoryIds!!.toSet() + localPrefs.followedCategories
            } else {
                localPrefs.followedCategories
            }

            preferencesRepository.updateFollowedExams(mergedExams)
            preferencesRepository.updateFollowedCategories(mergedCategories)

            // Push merged state back to cloud
            apiClient.syncPreferences(
                token = token,
                dto = UserPreferencesRemoteDto(
                    followedExamIds = mergedExams.toList(),
                    followedCategoryIds = mergedCategories.toList(),
                    reminder7Days = cloudPrefs?.reminder7Days ?: localPrefs.reminder7Days,
                    reminder3Days = cloudPrefs?.reminder3Days ?: localPrefs.reminder3Days,
                    reminder1Day = cloudPrefs?.reminder1Day ?: localPrefs.reminder1Day,
                    pushEnabled = cloudPrefs?.pushEnabled ?: localPrefs.pushEnabled,
                    notifyNewNotices = true,
                    notifyDeadlines = true,
                    notifyResults = true
                )
            )

            // 2. Restore cloud checklists
            val cloudChecklists = apiClient.fetchChecklists(token)
            cloudChecklists?.forEach { cl ->
                val examId = cl.examId ?: return@forEach
                val completedKeys = cl.items?.filter { it.isCompleted == true }?.map { it.itemKey }?.toSet() ?: emptySet()
                if (completedKeys.isNotEmpty()) {
                    preferencesRepository.setChecklistItems(examId, completedKeys)
                }
            }
        } catch (e: Exception) {
            Log.w("ExamRadarRepository", "Error during cloud sync: ${e.message}")
        }
    }

    override suspend fun signUp(email: String, password: String, fullName: String): com.examradar.app.data.remote.AuthResult {
        val result = authService.signUp(email, password, fullName)
        if (result is com.examradar.app.data.remote.AuthResult.Success) {
            val token = result.session.accessToken
            preferencesRepository.saveUserSession(
                userId = result.session.user.id,
                email = result.session.user.email,
                fullName = result.session.user.fullName,
                token = token,
                refreshToken = result.session.refreshToken
            )
            syncCloudState(token)
            registerDeviceTokenIfAvailable(token)
        }
        return result
    }

    override suspend fun signIn(email: String, password: String): com.examradar.app.data.remote.AuthResult {
        val result = authService.signIn(email, password)
        if (result is com.examradar.app.data.remote.AuthResult.Success) {
            val token = result.session.accessToken
            preferencesRepository.saveUserSession(
                userId = result.session.user.id,
                email = result.session.user.email,
                fullName = result.session.user.fullName,
                token = token,
                refreshToken = result.session.refreshToken
            )
            syncCloudState(token)
            registerDeviceTokenIfAvailable(token)
        }
        return result
    }

    suspend fun getValidAuthToken(): String? {
        val prefs = preferencesRepository.userPreferences.first()
        val currentToken = prefs.authToken
        val refreshToken = prefs.refreshToken

        if (currentToken.isNullOrBlank()) {
            return null
        }

        if (!refreshToken.isNullOrBlank() && isTokenExpired(currentToken)) {
            val refreshResult = authService.refreshSession(refreshToken)
            if (refreshResult is com.examradar.app.data.remote.AuthResult.Success) {
                val newToken = refreshResult.session.accessToken
                val newRefreshToken = refreshResult.session.refreshToken ?: refreshToken
                preferencesRepository.updateAuthToken(newToken, newRefreshToken)
                return newToken
            }
        }

        return currentToken
    }

    private fun isTokenExpired(jwt: String): Boolean {
        return try {
            val parts = jwt.split(".")
            if (parts.size != 3) return false
            val payloadJson = String(
                android.util.Base64.decode(
                    parts[1],
                    android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
                ),
                Charsets.UTF_8
            )
            val exp = com.google.gson.JsonParser.parseString(payloadJson).asJsonObject.get("exp")?.asLong ?: return false
            val nowSeconds = System.currentTimeMillis() / 1000
            nowSeconds >= (exp - 60)
        } catch (_: Exception) {
            false
        }
    }

    private fun registerDeviceTokenIfAvailable(authToken: String) {
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val fcmToken = task.result
                    CoroutineScope(Dispatchers.IO).launch {
                        apiClient.registerDeviceToken(authToken, fcmToken)
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("ExamRadarRepo", "FirebaseMessaging not initialized: ${e.message}")
        }
    }

    override suspend fun signOut() {
        val token = getValidAuthToken()
        if (token != null) {
            authService.logout(token)
        }
        preferencesRepository.clearUserSession()
    }
}
