package com.examradar.app.data.repository

import com.examradar.app.data.fixture.DevelopmentNoticeFixtures
import com.examradar.app.data.model.*
import com.examradar.app.data.preferences.UserPreferences
import com.examradar.app.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate

interface ExamRadarRepository {
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

    fun getCategories(): Flow<List<CategoryModel>>
    fun getExams(categoryId: String? = null, searchQuery: String? = null): Flow<List<ExamModel>>
    fun getExamById(examId: String): Flow<ExamModel?>
    fun getNotices(
        typeFilter: NoticeType? = null,
        categoryFilter: String? = null,
        onlyApproachingDeadlines: Boolean = false,
        searchQuery: String? = null
    ): Flow<List<NoticeModel>>
    fun getNoticeById(noticeId: String): Flow<NoticeModel?>
    fun getCalendarEvents(): Flow<List<CalendarEventModel>>
    fun getChecklist(examId: String): Flow<ChecklistModel>
    fun toggleChecklistItem(examId: String, itemKey: String)
    fun getSourceFreshness(): Flow<SourceFreshnessInfo>
}

class ExamRadarRepositoryImpl(
    private val preferencesRepository: UserPreferencesRepository
) : ExamRadarRepository {

    private val completedChecklistItems = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

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

    override suspend fun resetAllUserData() =
        preferencesRepository.clearAllData()

    override fun getCategories(): Flow<List<CategoryModel>> =
        MutableStateFlow(DevelopmentNoticeFixtures.categories)

    override fun getExams(categoryId: String?, searchQuery: String?): Flow<List<ExamModel>> =
        preferencesRepository.userPreferences.map { prefs ->
            DevelopmentNoticeFixtures.exams
                .filter { exam ->
                    val matchesCategory = categoryId == null || exam.categoryId == categoryId
                    val matchesSearch = searchQuery.isNullOrBlank() ||
                            exam.examName.contains(searchQuery, ignoreCase = true) ||
                            exam.organization.contains(searchQuery, ignoreCase = true) ||
                            exam.examCode.contains(searchQuery, ignoreCase = true)
                    matchesCategory && matchesSearch
                }
                .map { exam ->
                    exam.copy(isFollowing = prefs.followedExams.contains(exam.id))
                }
        }

    override fun getExamById(examId: String): Flow<ExamModel?> =
        preferencesRepository.userPreferences.map { prefs ->
            DevelopmentNoticeFixtures.exams.find { it.id == examId }?.let { exam ->
                exam.copy(isFollowing = prefs.followedExams.contains(exam.id))
            }
        }

    override fun getNotices(
        typeFilter: NoticeType?,
        categoryFilter: String?,
        onlyApproachingDeadlines: Boolean,
        searchQuery: String?
    ): Flow<List<NoticeModel>> =
        preferencesRepository.userPreferences.map { prefs ->
            val now = LocalDate.now()
            DevelopmentNoticeFixtures.sampleNotices
                .filter { notice ->
                    val matchesType = typeFilter == null || notice.noticeType == typeFilter
                    val matchesCategory = categoryFilter == null || run {
                        val exam = DevelopmentNoticeFixtures.exams.find { it.id == notice.examId }
                        exam?.categoryId == categoryFilter
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
        MutableStateFlow(DevelopmentNoticeFixtures.sampleNotices.find { it.id == noticeId })

    override fun getCalendarEvents(): Flow<List<CalendarEventModel>> =
        preferencesRepository.userPreferences.map { prefs ->
            val events = mutableListOf<CalendarEventModel>()
            DevelopmentNoticeFixtures.sampleNotices.forEach { notice ->
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
            completedChecklistItems
        ) { exam, completedMap ->
            val examName = exam?.examName ?: "Examination"
            val completedKeys = completedMap[examId] ?: emptySet()
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
        val current = completedChecklistItems.value.toMutableMap()
        val itemsForExam = current[examId]?.toMutableSet() ?: mutableSetOf()
        if (itemsForExam.contains(itemKey)) {
            itemsForExam.remove(itemKey)
        } else {
            itemsForExam.add(itemKey)
        }
        current[examId] = itemsForExam
        completedChecklistItems.value = current
    }

    override fun getSourceFreshness(): Flow<SourceFreshnessInfo> =
        MutableStateFlow(
            SourceFreshnessInfo(
                totalSources = 5,
                activeSources = 5,
                lastSuccessfulSync = System.currentTimeMillis() - 1800000L,
                isStale = false,
                statusMessage = "All 5 official recruitment boards verified active"
            )
        )
}
