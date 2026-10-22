package com.examradar.app.data.remote

import android.util.Log
import com.examradar.app.BuildConfig
import com.examradar.app.data.model.*
import com.google.gson.Gson
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class ExamRadarApiClient(
    private val baseUrl: String = BuildConfig.API_BASE_URL
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun fetchCategories(): List<CategoryModel>? {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/api/v1/exams/categories")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("ExamRadarApiClient", "fetchCategories returned HTTP ${response.code}")
                    return null
                }
                val bodyStr = response.body?.string() ?: return null
                val apiResponse = gson.fromJson(bodyStr, CategoryApiResponse::class.java)
                val dtos = apiResponse.data ?: return emptyList()

                dtos.map { dto ->
                    CategoryModel(
                        id = dto.id,
                        name = dto.name,
                        slug = dto.slug,
                        description = dto.description ?: "",
                        examCount = 0
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("ExamRadarApiClient", "Unable to fetch categories: ${e.message}")
            null
        }
    }

    fun fetchExams(categoryId: String? = null, search: String? = null): List<ExamModel>? {
        return try {
            val urlBuilder = "$baseUrl/api/v1/exams".toHttpUrlOrNull()?.newBuilder() ?: return null
            if (!categoryId.isNullOrBlank()) urlBuilder.addQueryParameter("categoryId", categoryId)
            if (!search.isNullOrBlank()) urlBuilder.addQueryParameter("search", search)

            val request = Request.Builder()
                .url(urlBuilder.build())
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("ExamRadarApiClient", "fetchExams returned HTTP ${response.code}")
                    return null
                }
                val bodyStr = response.body?.string() ?: return null
                val apiResponse = gson.fromJson(bodyStr, ExamApiResponse::class.java)
                val dtos = apiResponse.data ?: return emptyList()

                dtos.map { dto ->
                    ExamModel(
                        id = dto.id,
                        categoryId = dto.categoryId ?: "cat-upsc",
                        organization = dto.organization ?: "Official Board",
                        examName = dto.examName,
                        examCode = dto.examCode,
                        officialPortalUrl = dto.officialPortalUrl ?: "https://upsc.gov.in",
                        description = dto.description ?: "",
                        isFollowing = false,
                        activeNoticeCount = 0
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("ExamRadarApiClient", "Unable to fetch exams: ${e.message}")
            null
        }
    }

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

    fun fetchUserPreferences(token: String): UserPreferencesRemoteDto? {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/api/v1/me/preferences")
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("ExamRadarApiClient", "fetchUserPreferences returned HTTP ${response.code}")
                    return null
                }
                val bodyStr = response.body?.string() ?: return null
                val apiResponse = gson.fromJson(bodyStr, UserPreferencesApiResponse::class.java)
                apiResponse.data
            }
        } catch (e: Exception) {
            Log.w("ExamRadarApiClient", "Error fetching user preferences from cloud: ${e.message}")
            null
        }
    }

    fun syncPreferences(token: String, dto: UserPreferencesRemoteDto): Boolean {
        return try {
            val json = gson.toJson(dto)
            val request = Request.Builder()
                .url("$baseUrl/api/v1/me/preferences")
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .put(json.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.w("ExamRadarApiClient", "Error syncing preferences to cloud: ${e.message}")
            false
        }
    }

    fun fetchChecklists(token: String): List<ChecklistRemoteDto>? {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/api/v1/me/checklists")
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("ExamRadarApiClient", "fetchChecklists returned HTTP ${response.code}")
                    return null
                }
                val bodyStr = response.body?.string() ?: return null
                val apiResponse = gson.fromJson(bodyStr, ChecklistApiResponse::class.java)
                apiResponse.data
            }
        } catch (e: Exception) {
            Log.w("ExamRadarApiClient", "Error fetching checklists: ${e.message}")
            null
        }
    }

    fun createChecklist(token: String, examId: String): ChecklistRemoteDto? {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/api/v1/me/checklists?examId=$examId")
                .header("Authorization", "Bearer $token")
                .post("{}".toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val bodyStr = response.body?.string() ?: return null
                val apiResponse = gson.fromJson(bodyStr, ChecklistApiResponse::class.java)
                apiResponse.data?.firstOrNull()
            }
        } catch (e: Exception) {
            Log.w("ExamRadarApiClient", "Error creating cloud checklist: ${e.message}")
            null
        }
    }

    fun updateChecklistItem(token: String, checklistId: String, itemKey: String, isCompleted: Boolean): Boolean {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/api/v1/me/checklists/$checklistId/items/$itemKey?isCompleted=$isCompleted")
                .header("Authorization", "Bearer $token")
                .patch("{}".toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.w("ExamRadarApiClient", "Error updating checklist item in cloud: ${e.message}")
            false
        }
    }

    fun deleteUserData(token: String): Boolean {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/api/v1/me/data")
                .header("Authorization", "Bearer $token")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("ExamRadarApiClient", "Error deleting cloud user data: ${e.message}")
            false
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

    fun registerDeviceToken(authToken: String, fcmToken: String, appVersion: String = "1.0.0"): Boolean {
        return try {
            val payload = mapOf(
                "token" to fcmToken,
                "deviceType" to "ANDROID",
                "appVersion" to appVersion
            )
            val json = gson.toJson(payload)
            val request = Request.Builder()
                .url("$baseUrl/api/v1/devices")
                .header("Authorization", "Bearer $authToken")
                .post(json.toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.w("ExamRadarApiClient", "Unable to register device token: ${e.message}")
            false
        }
    }

    fun unregisterDeviceToken(authToken: String, fcmToken: String): Boolean {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/api/v1/devices/$fcmToken")
                .header("Authorization", "Bearer $authToken")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.w("ExamRadarApiClient", "Unable to unregister device token: ${e.message}")
            false
        }
    }
}
