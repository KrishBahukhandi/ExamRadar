package com.examradar.app.ui.screens.vault

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.examradar.app.data.local.ApplicationVaultEntity
import com.examradar.app.data.model.ExamModel
import com.examradar.app.data.repository.ExamRadarRepository
import com.examradar.app.util.VaultStorageHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class VaultUiState(
    val entries: List<ApplicationVaultEntity> = emptyList(),
    val availableExams: List<ExamModel> = emptyList(),
    val followedExamIds: Set<String> = emptySet(),
    val isAddEditSheetOpen: Boolean = false,
    val editingEntry: ApplicationVaultEntity? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

class VaultViewModel(
    private val repository: ExamRadarRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isAddEditSheetOpen = MutableStateFlow(false)
    private val _editingEntry = MutableStateFlow<ApplicationVaultEntity?>(null)

    private val sheetAndSearchFlow = combine(
        _searchQuery,
        _isAddEditSheetOpen,
        _editingEntry
    ) { query, isSheetOpen, editing ->
        Triple(query, isSheetOpen, editing)
    }

    val uiState: StateFlow<VaultUiState> = combine(
        repository.getVaultEntries(),
        repository.getExams(),
        repository.getUserPreferences(),
        sheetAndSearchFlow
    ) { entries, exams, prefs, (query, isSheetOpen, editing) ->
        val filteredEntries = if (query.isBlank()) {
            entries
        } else {
            val q = query.lowercase().trim()
            entries.filter {
                it.examName.lowercase().contains(q) ||
                it.applicationNumber.lowercase().contains(q) ||
                (it.rollNumber?.lowercase()?.contains(q) == true) ||
                (it.examCenterName?.lowercase()?.contains(q) == true)
            }
        }

        VaultUiState(
            entries = filteredEntries,
            availableExams = exams,
            followedExamIds = prefs.followedExams,
            isAddEditSheetOpen = isSheetOpen,
            editingEntry = editing,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VaultUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun openAddSheet(prefilledExamId: String? = null, prefilledExamName: String? = null) {
        val newEntry = ApplicationVaultEntity(
            id = UUID.randomUUID().toString(),
            examId = prefilledExamId,
            examName = prefilledExamName ?: "",
            applicationNumber = ""
        )
        _editingEntry.value = newEntry
        _isAddEditSheetOpen.value = true
    }

    fun openEditSheet(entry: ApplicationVaultEntity) {
        _editingEntry.value = entry
        _isAddEditSheetOpen.value = true
    }

    fun dismissSheet() {
        _isAddEditSheetOpen.value = false
        _editingEntry.value = null
    }

    fun saveEntry(
        id: String,
        examId: String?,
        examName: String,
        applicationNumber: String,
        rollNumber: String?,
        examCenterName: String?,
        examCenterCity: String?,
        examDate: LocalDate?,
        reportingTime: String?,
        shiftDetails: String?,
        admitCardFileName: String?,
        admitCardFilePath: String?,
        applicationFormFileName: String?,
        applicationFormFilePath: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            val entry = ApplicationVaultEntity(
                id = id,
                examId = examId,
                examName = examName.trim(),
                applicationNumber = applicationNumber.trim(),
                rollNumber = rollNumber?.trim()?.takeIf { it.isNotBlank() },
                examCenterName = examCenterName?.trim()?.takeIf { it.isNotBlank() },
                examCenterCity = examCenterCity?.trim()?.takeIf { it.isNotBlank() },
                examDate = examDate,
                reportingTime = reportingTime?.trim()?.takeIf { it.isNotBlank() },
                shiftDetails = shiftDetails?.trim()?.takeIf { it.isNotBlank() },
                admitCardFileName = admitCardFileName,
                admitCardFilePath = admitCardFilePath,
                applicationFormFileName = applicationFormFileName,
                applicationFormFilePath = applicationFormFilePath,
                notes = notes?.trim()?.takeIf { it.isNotBlank() },
                updatedAt = System.currentTimeMillis()
            )
            repository.saveVaultEntry(entry)
            dismissSheet()
        }
    }

    fun deleteEntry(entry: ApplicationVaultEntity) {
        viewModelScope.launch {
            VaultStorageHelper.deleteDocumentFile(entry.admitCardFilePath)
            VaultStorageHelper.deleteDocumentFile(entry.applicationFormFilePath)
            repository.deleteVaultEntry(entry.id)
        }
    }

    suspend fun attachDocument(context: Context, uri: Uri, documentType: String): Pair<String, String>? {
        return VaultStorageHelper.savePdfFromUri(context, uri, documentType)
    }

    companion object {
        fun provideFactory(repository: ExamRadarRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return VaultViewModel(repository) as T
                }
            }
    }
}

