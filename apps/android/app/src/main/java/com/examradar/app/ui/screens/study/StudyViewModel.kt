package com.examradar.app.ui.screens.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.examradar.app.data.local.SyllabusTopicEntity
import com.examradar.app.data.model.ExamModel
import com.examradar.app.data.model.NoticeType
import com.examradar.app.data.repository.ExamRadarRepository
import com.examradar.app.data.study.ExamPatternGuide
import com.examradar.app.data.study.ExamPatternRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class StudyHubMode {
    SYLLABUS,
    PATTERN_AND_CUTOFFS
}

data class StudyUiState(
    val availableExams: List<ExamModel> = emptyList(),
    val selectedExamId: String = "exam-upsc-cse",
    val selectedExam: ExamModel? = null,
    val selectedMode: StudyHubMode = StudyHubMode.SYLLABUS,
    val stages: List<String> = listOf("Prelims", "Mains"),
    val selectedStage: String = "Prelims",
    val topicsBySubject: Map<String, List<SyllabusTopicEntity>> = emptyMap(),
    val completedTopicsCount: Int = 0,
    val totalTopicsCount: Int = 0,
    val progressPercentage: Int = 0,
    val patternGuide: ExamPatternGuide? = null,
    val nextExamDate: LocalDate? = null,
    val daysRemaining: Long? = null,
    val isLoading: Boolean = false
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class StudyViewModel(
    private val repository: ExamRadarRepository
) : ViewModel() {

    private val _selectedExamId = MutableStateFlow("exam-upsc-cse")
    private val _selectedStage = MutableStateFlow("Prelims")
    private val _selectedMode = MutableStateFlow(StudyHubMode.SYLLABUS)

    val uiState: StateFlow<StudyUiState> = combine(
        repository.getExams(),
        repository.getUserPreferences(),
        _selectedExamId,
        _selectedStage,
        _selectedMode
    ) { exams, prefs, selectedId, selectedStage, mode ->
        val effectiveExamId = if (exams.any { it.id == selectedId }) {
            selectedId
        } else {
            prefs.followedExams.firstOrNull() ?: exams.firstOrNull()?.id ?: "exam-upsc-cse"
        }

        val currentExam = exams.find { it.id == effectiveExamId }
        val guide = ExamPatternRepository.getGuide(effectiveExamId)

        Tuple5(exams, effectiveExamId, currentExam, selectedStage, mode to guide)
    }.flatMapLatest { tuple ->
        val exams = tuple.t1
        val examId = tuple.t2
        val currentExam = tuple.t3
        val currentStage = tuple.t4
        val (mode, guide) = tuple.t5

        combine(
            repository.getSyllabusTopics(examId),
            repository.getSyllabusProgress(examId),
            repository.getNotices()
        ) { topics, progress, notices ->
            val stages = topics.map { it.stage }.distinct().ifEmpty { listOf("Prelims", "Mains") }
            val effectiveStage = if (stages.contains(currentStage)) currentStage else stages.first()

            val stageTopics = topics.filter { it.stage == effectiveStage }
            val grouped = stageTopics.groupBy { it.subject }

            val completed = progress.first
            val total = progress.second
            val pct = if (total > 0) ((completed.toFloat() / total) * 100).toInt() else 0

            // Find next exam date from active notices
            val noticeForExam = notices.firstOrNull { it.examId == examId && it.examDate != null && it.examDate.isAfter(LocalDate.now().minusDays(1)) }
            val examDate = noticeForExam?.examDate
            val days = if (examDate != null) ChronoUnit.DAYS.between(LocalDate.now(), examDate).coerceAtLeast(0) else null

            StudyUiState(
                availableExams = exams.filter { it.id in ExamPatternRepository.getAllExamIds() || it.isFollowing },
                selectedExamId = examId,
                selectedExam = currentExam,
                selectedMode = mode,
                stages = stages,
                selectedStage = effectiveStage,
                topicsBySubject = grouped,
                completedTopicsCount = completed,
                totalTopicsCount = total,
                progressPercentage = pct,
                patternGuide = guide,
                nextExamDate = examDate,
                daysRemaining = days,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StudyUiState(isLoading = true)
    )

    fun selectExam(examId: String) {
        _selectedExamId.value = examId
    }

    fun selectStage(stage: String) {
        _selectedStage.value = stage
    }

    fun selectMode(mode: StudyHubMode) {
        _selectedMode.value = mode
    }

    fun toggleTopic(topicId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            repository.setTopicCompleted(topicId, !currentStatus)
        }
    }

    private data class Tuple5<A, B, C, D, E>(val t1: A, val t2: B, val t3: C, val t4: D, val t5: E)

    companion object {
        fun provideFactory(repository: ExamRadarRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return StudyViewModel(repository) as T
                }
            }
    }
}
