package com.examradar.app.data.local

import com.examradar.app.data.model.*
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.time.LocalDate

private class EntityLocalDateAdapter : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
    override fun serialize(src: LocalDate?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.toString())
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDate? {
        return try {
            json?.asString?.let { LocalDate.parse(it) }
        } catch (_: Exception) {
            null
        }
    }
}

private val gson: Gson = GsonBuilder()
    .registerTypeAdapter(LocalDate::class.java, EntityLocalDateAdapter())
    .create()

fun CategoryEntity.toDomainModel(): CategoryModel =
    CategoryModel(
        id = id,
        name = name,
        slug = slug,
        description = description,
        examCount = examCount
    )

fun CategoryModel.toEntity(): CategoryEntity =
    CategoryEntity(
        id = id,
        name = name,
        slug = slug,
        description = description,
        examCount = examCount
    )

fun ExamEntity.toDomainModel(): ExamModel =
    ExamModel(
        id = id,
        categoryId = categoryId,
        organization = organization,
        examName = examName,
        examCode = examCode,
        officialPortalUrl = officialPortalUrl,
        description = description,
        isFollowing = isFollowing,
        activeNoticeCount = activeNoticeCount
    )

fun ExamModel.toEntity(): ExamEntity =
    ExamEntity(
        id = id,
        categoryId = categoryId,
        organization = organization,
        examName = examName,
        examCode = examCode,
        officialPortalUrl = officialPortalUrl,
        description = description,
        isFollowing = isFollowing,
        activeNoticeCount = activeNoticeCount
    )

fun NoticeEntity.toDomainModel(): NoticeModel {
    val dates: List<ImportantDateModel> = runCatching {
        val type = object : TypeToken<List<ImportantDateModel>>() {}.type
        gson.fromJson<List<ImportantDateModel>>(importantDatesJson, type) ?: emptyList()
    }.getOrDefault(emptyList())

    return NoticeModel(
        id = id,
        examId = examId,
        examName = examName,
        organization = organization,
        noticeType = noticeType,
        title = title,
        summary = summary,
        officialNoticeUrl = officialNoticeUrl,
        officialApplyUrl = officialApplyUrl,
        status = status,
        verificationStatus = verificationStatus,
        publishedDate = publishedDate,
        applicationDeadline = applicationDeadline,
        examDate = examDate,
        isTentativeDeadline = isTentativeDeadline,
        lastCheckedTimestamp = lastCheckedTimestamp,
        isDemonstrationFixture = isDemonstrationFixture,
        importantDates = dates,
        eligibilitySummary = eligibilitySummary
    )
}

fun NoticeModel.toEntity(): NoticeEntity {
    val datesJson = runCatching { gson.toJson(importantDates) }.getOrDefault("[]")
    return NoticeEntity(
        id = id,
        examId = examId,
        examName = examName,
        organization = organization,
        noticeType = noticeType,
        title = title,
        summary = summary,
        officialNoticeUrl = officialNoticeUrl,
        officialApplyUrl = officialApplyUrl,
        status = status,
        verificationStatus = verificationStatus,
        publishedDate = publishedDate,
        applicationDeadline = applicationDeadline,
        examDate = examDate,
        isTentativeDeadline = isTentativeDeadline,
        lastCheckedTimestamp = lastCheckedTimestamp,
        isDemonstrationFixture = isDemonstrationFixture,
        eligibilitySummary = eligibilitySummary,
        importantDatesJson = datesJson
    )
}
