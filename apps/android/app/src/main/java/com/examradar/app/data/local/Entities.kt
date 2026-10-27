package com.examradar.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.examradar.app.data.model.NoticeType
import com.examradar.app.data.model.VerificationStatus
import java.time.LocalDate

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val slug: String,
    val description: String,
    val examCount: Int = 0
)

@Entity(
    tableName = "exams",
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["examCode"])
    ]
)
data class ExamEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    val organization: String,
    val examName: String,
    val examCode: String,
    val officialPortalUrl: String,
    val description: String,
    val isFollowing: Boolean = false,
    val activeNoticeCount: Int = 0
)

@Entity(
    tableName = "notices",
    indices = [
        Index(value = ["examId"]),
        Index(value = ["noticeType"]),
        Index(value = ["publishedDate"])
    ]
)
data class NoticeEntity(
    @PrimaryKey val id: String,
    val examId: String,
    val examName: String,
    val organization: String,
    val noticeType: NoticeType,
    val title: String,
    val summary: String,
    val officialNoticeUrl: String,
    val officialApplyUrl: String? = null,
    val status: String,
    val verificationStatus: VerificationStatus,
    val publishedDate: LocalDate? = null,
    val applicationDeadline: LocalDate? = null,
    val examDate: LocalDate? = null,
    val isTentativeDeadline: Boolean = false,
    val lastCheckedTimestamp: Long = System.currentTimeMillis(),
    val isDemonstrationFixture: Boolean = false,
    val eligibilitySummary: String? = null,
    val importantDatesJson: String = "[]"
)
