package com.examradar.app.data.remote

data class NoticeApiResponse(
    val success: Boolean,
    val message: String?,
    val data: List<NoticeRemoteDto>?
)

data class NoticeDateRemoteDto(
    val id: String?,
    val dateType: String?,
    val eventDate: String?,
    val isTentative: Boolean?,
    val isRevised: Boolean?,
    val originalDateStr: String?,
    val sourceTextSnippet: String?
)

data class NoticeRemoteDto(
    val id: String,
    val examId: String?,
    val examName: String?,
    val organization: String?,
    val sourceId: String?,
    val sourceName: String?,
    val noticeType: String?,
    val title: String,
    val summary: String?,
    val officialNoticeUrl: String,
    val officialApplyUrl: String?,
    val status: String?,
    val publishedDate: String?,
    val verificationStatus: String?,
    val applicationDeadline: String?,
    val importantDates: List<NoticeDateRemoteDto>?
)

data class CategoryApiResponse(
    val success: Boolean,
    val message: String?,
    val data: List<CategoryRemoteDto>?
)

data class CategoryRemoteDto(
    val id: String,
    val name: String,
    val slug: String,
    val description: String?,
    val displayOrder: Int?
)

data class ExamApiResponse(
    val success: Boolean,
    val message: String?,
    val data: List<ExamRemoteDto>?
)

data class ExamRemoteDto(
    val id: String,
    val categoryId: String?,
    val categoryName: String?,
    val organization: String?,
    val examName: String,
    val examCode: String,
    val officialPortalUrl: String?,
    val description: String?,
    val isActive: Boolean?
)

data class UserPreferencesApiResponse(
    val success: Boolean,
    val message: String?,
    val data: UserPreferencesRemoteDto?
)

data class UserPreferencesRemoteDto(
    val followedExamIds: List<String>?,
    val followedCategoryIds: List<String>?,
    val reminder7Days: Boolean?,
    val reminder3Days: Boolean?,
    val reminder1Day: Boolean?,
    val pushEnabled: Boolean?,
    val notifyNewNotices: Boolean?,
    val notifyDeadlines: Boolean?,
    val notifyResults: Boolean?
)

data class ChecklistApiResponse(
    val success: Boolean,
    val message: String?,
    val data: List<ChecklistRemoteDto>?
)

data class ChecklistItemRemoteDto(
    val id: String?,
    val itemKey: String,
    val title: String,
    val isCompleted: Boolean?,
    val displayOrder: Int?
)

data class ChecklistRemoteDto(
    val id: String,
    val userId: String?,
    val examId: String?,
    val examName: String?,
    val title: String?,
    val notes: String?,
    val items: List<ChecklistItemRemoteDto>?
)
