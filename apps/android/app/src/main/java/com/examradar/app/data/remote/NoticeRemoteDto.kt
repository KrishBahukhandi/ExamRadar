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
