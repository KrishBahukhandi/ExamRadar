package com.examradar.app.ui.screens.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.collection.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examradar.app.data.model.NoticeModel
import com.examradar.app.data.repository.ExamRadarRepository
import com.examradar.app.util.NoticePdfManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PdfViewerUiState(
    val isLoading: Boolean = true,
    val isDownloading: Boolean = false,
    val notice: NoticeModel? = null,
    val pageCount: Int = 0,
    val currentPage: Int = 1,
    val error: String? = null,
    val isOfflineCached: Boolean = false
)

class PdfViewerViewModel(
    private val repository: ExamRadarRepository,
    private val noticeId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfViewerUiState())
    val uiState = _uiState.asStateFlow()

    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private val pageBitmapCache = LruCache<Int, Bitmap>(12)

    fun loadPdf(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val notice = repository.getNoticeById(noticeId).firstOrNull()
            if (notice == null) {
                _uiState.update { it.copy(isLoading = false, error = "Notice not found") }
                return@launch
            }

            _uiState.update { it.copy(notice = notice) }

            val file = NoticePdfManager.getCachedPdfFile(context, noticeId)
            val isCached = NoticePdfManager.isPdfCached(context, noticeId)

            if (!isCached) {
                _uiState.update { it.copy(isDownloading = true) }
                val downloadResult = NoticePdfManager.downloadAndCachePdf(
                    context = context,
                    noticeId = notice.id,
                    url = notice.officialNoticeUrl,
                    isDemoFixture = notice.isDemonstrationFixture
                )
                _uiState.update { it.copy(isDownloading = false) }

                if (downloadResult.isFailure) {
                    _uiState.update { it.copy(isLoading = false, error = "Could not download official PDF") }
                    return@launch
                }
            }

            initRenderer(file)
        }
    }

    private suspend fun initRenderer(file: File) = withContext(Dispatchers.IO) {
        try {
            closeRenderer()
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            fileDescriptor = pfd
            val renderer = PdfRenderer(pfd)
            pdfRenderer = renderer
            val count = renderer.pageCount
            _uiState.update {
                it.copy(
                    isLoading = false,
                    pageCount = count,
                    isOfflineCached = true
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isLoading = false, error = "Unable to render PDF document: ${e.message}")
            }
        }
    }

    suspend fun renderPage(pageIndex: Int, targetWidth: Int = 1080): Bitmap? = withContext(Dispatchers.IO) {
        if (pageIndex < 0 || pageIndex >= (_uiState.value.pageCount)) return@withContext null
        pageBitmapCache.get(pageIndex)?.let { return@withContext it }

        val renderer = pdfRenderer ?: return@withContext null
        try {
            val page = renderer.openPage(pageIndex)
            val aspectRatio = page.height.toFloat() / page.width.toFloat()
            val height = (targetWidth * aspectRatio).toInt().coerceAtLeast(100)
            val bitmap = Bitmap.createBitmap(targetWidth, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(AndroidColor.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            pageBitmapCache.put(pageIndex, bitmap)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    fun updateCurrentPage(page: Int) {
        _uiState.update { it.copy(currentPage = page.coerceIn(1, (it.pageCount).coerceAtLeast(1))) }
    }

    private fun closeRenderer() {
        try {
            pdfRenderer?.close()
            fileDescriptor?.close()
        } catch (_: Exception) {}
        pdfRenderer = null
        fileDescriptor = null
        pageBitmapCache.evictAll()
    }

    override fun onCleared() {
        super.onCleared()
        closeRenderer()
    }
}
