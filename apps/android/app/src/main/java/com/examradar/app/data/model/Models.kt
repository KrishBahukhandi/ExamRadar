package com.examradar.app.data.model

import java.time.LocalDate

enum class NoticeType {
    RECRUITMENT,
    ADMIT_CARD,
    RESULT,
    ANSWER_KEY,
    CORRIGENDUM,
    POSTPONEMENT
}

enum class VerificationStatus {
    VERIFIED,
    AUTOMATED_HIGH_CONFIDENCE,
    QUARANTINED,
    PENDING
}

enum class DateType {
    APPLICATION_START,
    APPLICATION_DEADLINE,
    CORRECTION_WINDOW,
    EXAM_DATE,
    ADMIT_CARD_RELEASE,
    RESULT_DATE
}

data class CategoryModel(
    val id: String,
    val name: String,
    val slug: String,
    val description: String,
    val examCount: Int = 0
)

data class ExamModel(
    val id: String,
    val categoryId: String,
    val organization: String,
    val examName: String,
    val examCode: String,
    val officialPortalUrl: String,
    val description: String,
    val isFollowing: Boolean = false,
    val activeNoticeCount: Int = 0
)

data class ImportantDateModel(
    val dateType: DateType,
    val eventDate: LocalDate,
    val isTentative: Boolean = false,
    val isRevised: Boolean = false,
    val originalDateStr: String? = null
)

data class NoticeModel(
    val id: String,
    val examId: String,
    val examName: String,
    val organization: String,
    val noticeType: NoticeType,
    val title: String,
    val summary: String,
    val officialNoticeUrl: String,
    val officialApplyUrl: String? = null,
    val status: String, // PUBLISHED, VALIDATION_REQUIRED, etc.
    val verificationStatus: VerificationStatus,
    val publishedDate: LocalDate? = null,
    val applicationDeadline: LocalDate? = null,
    val examDate: LocalDate? = null,
    val isTentativeDeadline: Boolean = false,
    val lastCheckedTimestamp: Long = System.currentTimeMillis(),
    val isDemonstrationFixture: Boolean = false,
    val importantDates: List<ImportantDateModel> = emptyList(),
    val eligibilitySummary: String? = null,
    val totalVacancies: Int? = null,
    val vacancyBreakdownJson: String? = null,
    val payScaleSummary: String? = null,
    val applicationFeeGeneral: Int? = null,
    val applicationFeeReserved: Int? = null,
    val feeExemptionDetails: String? = null,
    val ageMin: Int? = null,
    val ageMax: Int? = null,
    val ageCutoffDate: LocalDate? = null,
    val ageRelaxationSummary: String? = null,
    val educationalQualification: String? = null
)

data class CalendarEventModel(
    val id: String,
    val examId: String,
    val examName: String,
    val organization: String,
    val dateType: DateType,
    val eventDate: LocalDate,
    val isTentative: Boolean,
    val isRevised: Boolean,
    val noticeId: String,
    val officialNoticeUrl: String
)

data class ChecklistItemModel(
    val itemKey: String,
    val title: String,
    val isCompleted: Boolean = false
)

data class ChecklistModel(
    val examId: String,
    val examName: String,
    val items: List<ChecklistItemModel>
)

data class SourceFreshnessInfo(
    val totalSources: Int,
    val activeSources: Int,
    val lastSuccessfulSync: Long,
    val isStale: Boolean = false,
    val statusMessage: String = "All official sources operational"
)
