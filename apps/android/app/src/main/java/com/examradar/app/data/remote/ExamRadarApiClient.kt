package com.examradar.app.data.remote

import android.util.Log
import com.examradar.app.data.model.*
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class ExamRadarApiClient(
    private val baseUrl: String = "http://10.0.2.2:8085"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    fun fetchNotices(): List<NoticeModel>? {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/api/v1/notices")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("ExamRadarApiClient", "Server returned HTTP ${response.code}")
                    return null
                }
                val bodyStr = response.body?.string() ?: return null
                val apiResponse = gson.fromJson(bodyStr, NoticeApiResponse::class.java)
                val dtos = apiResponse.data ?: return emptyList()

                dtos.map { dto ->
                    mapDtoToModel(dto)
                }
            }
        } catch (e: Exception) {
            Log.w("ExamRadarApiClient", "Unable to fetch live notices: ${e.message}")
            null
        }
    }

    private fun mapDtoToModel(dto: NoticeRemoteDto): NoticeModel {
        val parsedNoticeType = try {
            NoticeType.valueOf(dto.noticeType ?: "RECRUITMENT")
        } catch (_: Exception) {
            NoticeType.RECRUITMENT
        }

        val parsedVerificationStatus = try {
            VerificationStatus.valueOf(dto.verificationStatus ?: "VERIFIED")
        } catch (_: Exception) {
            VerificationStatus.VERIFIED
        }

        val pubDate = dto.publishedDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val appDeadline = dto.applicationDeadline?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        val dates = dto.importantDates?.mapNotNull { d ->
            val date = d.eventDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return@mapNotNull null
            val dateType = try {
                DateType.valueOf(d.dateType ?: "APPLICATION_START")
            } catch (_: Exception) {
                DateType.APPLICATION_START
            }
            ImportantDateModel(
                dateType = dateType,
                eventDate = date,
                isTentative = d.isTentative ?: false,
                isRevised = d.isRevised ?: false,
                originalDateStr = d.originalDateStr
            )
        } ?: emptyList()

        val examDate = dates.find { it.dateType == DateType.EXAM_DATE }?.eventDate

        return NoticeModel(
            id = dto.id,
            examId = dto.examId ?: "exam-ssc-cgl",
            examName = dto.examName ?: (dto.organization ?: "Government Recruitment"),
            organization = dto.organization ?: "Official Board",
            noticeType = parsedNoticeType,
            title = dto.title,
            summary = dto.summary ?: dto.title,
            officialNoticeUrl = dto.officialNoticeUrl,
            officialApplyUrl = dto.officialApplyUrl,
            status = dto.status ?: "PUBLISHED",
            verificationStatus = parsedVerificationStatus,
            publishedDate = pubDate,
            applicationDeadline = appDeadline,
            examDate = examDate,
            isTentativeDeadline = false,
            lastCheckedTimestamp = System.currentTimeMillis(),
            isDemonstrationFixture = false,
            importantDates = dates,
            eligibilitySummary = "Official verified notice published on ${dto.publishedDate ?: "portal"}."
        )
    }
}
