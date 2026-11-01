package com.examradar.app.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.examradar.app.data.local.ApplicationVaultEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Manages secure offline storage for student admit cards, application PDFs,
 * and shareable credential formatting.
 */
object VaultStorageHelper {

    private const val TAG = "VaultStorageHelper"
    private const val VAULT_DIR_NAME = "vault_documents"

    fun getVaultDir(context: Context): File {
        val dir = File(context.filesDir, VAULT_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun savePdfFromUri(
        context: Context,
        uri: Uri,
        documentType: String
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val originalFileName = getFileNameFromUri(context, uri) ?: "${documentType}_${System.currentTimeMillis()}.pdf"
            val sanitizedName = originalFileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFileName = "${UUID.randomUUID().toString().substring(0, 8)}_$sanitizedName"
            val targetFile = File(getVaultDir(context), targetFileName)

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (targetFile.exists() && targetFile.length() > 0) {
                Pair(originalFileName, targetFile.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy vault document: ${e.message}", e)
            null
        }
    }

    suspend fun deleteDocumentFile(filePath: String?) = withContext(Dispatchers.IO) {
        if (!filePath.isNullOrBlank()) {
            try {
                val file = File(filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not delete vault file: ${e.message}")
            }
        }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            result = cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    /**
     * Formats application credentials into a clean, copyable card for WhatsApp/Telegram sharing.
     */
    fun formatShareText(entry: ApplicationVaultEntity): String {
        val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")
        return buildString {
            append("📋 *ExamRadar Application Details*\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("🎯 *Exam:* ${entry.examName}\n")
            append("🔢 *Application No:* `${entry.applicationNumber}`\n")
            if (!entry.rollNumber.isNullOrBlank()) {
                append("🎫 *Roll No:* `${entry.rollNumber}`\n")
            }
            if (entry.examDate != null) {
                append("📅 *Exam Date:* ${entry.examDate.format(dateFormatter)}\n")
            }
            if (!entry.reportingTime.isNullOrBlank()) {
                append("⏰ *Reporting Time:* ${entry.reportingTime}\n")
            }
            if (!entry.shiftDetails.isNullOrBlank()) {
                append("⌛ *Shift/Session:* ${entry.shiftDetails}\n")
            }
            if (!entry.examCenterName.isNullOrBlank()) {
                append("📍 *Center:* ${entry.examCenterName}")
                if (!entry.examCenterCity.isNullOrBlank()) {
                    append(", ${entry.examCenterCity}")
                }
                append("\n")
            }
            if (!entry.notes.isNullOrBlank()) {
                append("📝 *Instructions:* ${entry.notes}\n")
            }
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("⚡ Stored securely offline with ExamRadar App")
        }
    }
}
