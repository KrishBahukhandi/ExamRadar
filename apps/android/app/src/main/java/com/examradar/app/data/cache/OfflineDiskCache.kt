package com.examradar.app.data.cache

import android.content.Context
import android.util.Log
import com.examradar.app.data.model.CategoryModel
import com.examradar.app.data.model.ExamModel
import com.examradar.app.data.model.NoticeModel
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.io.File
import java.lang.reflect.Type
import java.time.LocalDate

/**
 * Offline persistent disk cache for ExamRadar.
 * Persists official notices, exams, categories, and sync metadata directly to internal storage
 * so candidates can review announcements offline or during low connectivity commutes.
 */
class OfflineDiskCache(private val cacheDir: File) {

    constructor(context: Context) : this(File(context.filesDir, "examradar_cache"))

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
        .create()

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    private val noticesFile get() = File(cacheDir, "cached_notices.json")
    private val examsFile get() = File(cacheDir, "cached_exams.json")
    private val categoriesFile get() = File(cacheDir, "cached_categories.json")
    private val metaFile get() = File(cacheDir, "cache_meta.json")

    data class CacheMeta(
        val lastSyncTimestamp: Long = 0L,
        val noticesCount: Int = 0,
        val examsCount: Int = 0
    )

    private val memoryNotices = java.util.concurrent.atomic.AtomicReference<List<NoticeModel>?>()
    private val memoryExams = java.util.concurrent.atomic.AtomicReference<List<ExamModel>?>()
    private val memoryCategories = java.util.concurrent.atomic.AtomicReference<List<CategoryModel>?>()

    fun saveNotices(notices: List<NoticeModel>) {
        memoryNotices.set(notices)
        writeAtomic(noticesFile, gson.toJson(notices))
        updateMeta(noticesCount = notices.size)
    }

    fun loadNotices(): List<NoticeModel>? {
        return memoryNotices.get() ?: readSafely<List<NoticeModel>>(noticesFile, object : TypeToken<List<NoticeModel>>() {}.type)?.also {
            memoryNotices.set(it)
        }
    }

    fun saveExams(exams: List<ExamModel>) {
        memoryExams.set(exams)
        writeAtomic(examsFile, gson.toJson(exams))
        updateMeta(examsCount = exams.size)
    }

    fun loadExams(): List<ExamModel>? {
        return memoryExams.get() ?: readSafely<List<ExamModel>>(examsFile, object : TypeToken<List<ExamModel>>() {}.type)?.also {
            memoryExams.set(it)
        }
    }

    fun saveCategories(categories: List<CategoryModel>) {
        memoryCategories.set(categories)
        writeAtomic(categoriesFile, gson.toJson(categories))
    }

    fun loadCategories(): List<CategoryModel>? {
        return memoryCategories.get() ?: readSafely<List<CategoryModel>>(categoriesFile, object : TypeToken<List<CategoryModel>>() {}.type)?.also {
            memoryCategories.set(it)
        }
    }

    fun getLastSyncTimestamp(): Long {
        return getMeta().lastSyncTimestamp
    }

    fun isStale(): Boolean {
        val last = getLastSyncTimestamp()
        if (last == 0L) return true
        return (System.currentTimeMillis() - last) > (24 * 3600 * 1000L)
    }

    private fun updateMeta(noticesCount: Int? = null, examsCount: Int? = null) {
        val current = getMeta()
        val updated = current.copy(
            lastSyncTimestamp = System.currentTimeMillis(),
            noticesCount = noticesCount ?: current.noticesCount,
            examsCount = examsCount ?: current.examsCount
        )
        writeAtomic(metaFile, gson.toJson(updated))
    }

    fun getMeta(): CacheMeta {
        return readSafely(metaFile, CacheMeta::class.java) ?: CacheMeta()
    }

    fun clear() {
        memoryNotices.set(null)
        memoryExams.set(null)
        memoryCategories.set(null)
        try {
            noticesFile.delete()
            examsFile.delete()
            categoriesFile.delete()
            metaFile.delete()
        } catch (e: Exception) {
            logWarning("Error clearing cache: ${e.message}")
        }
    }

    private fun writeAtomic(targetFile: File, content: String) {
        try {
            val tempFile = File(cacheDir, "${targetFile.name}.tmp")
            tempFile.writeText(content, Charsets.UTF_8)
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
        } catch (e: Exception) {
            logWarning("Failed to write atomic cache file: ${targetFile.name}", e)
        }
    }

    private fun <T> readSafely(file: File, type: Type): T? {
        if (!file.exists() || file.length() == 0L) return null
        return try {
            val content = file.readText(Charsets.UTF_8)
            gson.fromJson(content, type)
        } catch (e: Exception) {
            logWarning("Failed to read cache file: ${file.name}", e)
            null
        }
    }

    private fun logWarning(msg: String, e: Throwable? = null) {
        try {
            if (e != null) Log.w(TAG, msg, e) else Log.w(TAG, msg)
        } catch (_: Throwable) {
            // In pure JUnit test environments where android.util.Log is unmocked
            System.err.println("[$TAG] $msg ${e?.message ?: ""}")
        }
    }

    private class LocalDateAdapter : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
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

    companion object {
        private const val TAG = "OfflineDiskCache"
    }
}
