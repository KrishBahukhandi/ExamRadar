package com.examradar.app.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examradar.app.data.model.DateType
import com.examradar.app.data.model.ImportantDateModel
import com.examradar.app.data.model.NoticeModel
import com.examradar.app.data.repository.ExamRadarRepository
import com.examradar.app.ui.components.*
import com.examradar.app.ui.theme.*
import com.examradar.app.util.CustomTabsHelper
import com.examradar.app.util.DateFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class NoticeDetailUiState(
    val isLoading: Boolean = true,
    val notice: NoticeModel? = null,
    val isFollowing: Boolean = false
)

class NoticeDetailViewModel(
    private val repository: ExamRadarRepository,
    private val noticeId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoticeDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadNotice()
    }

    private fun loadNotice() {
        viewModelScope.launch {
            repository.getNoticeById(noticeId).collectLatest { notice ->
                if (notice != null) {
                    repository.getExamById(notice.examId).collectLatest { exam ->
                        _uiState.value = _uiState.value.copy(
                            notice = notice,
                            isFollowing = exam?.isFollowing ?: false,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun toggleFollow() {
        val notice = _uiState.value.notice ?: return
        viewModelScope.launch {
            repository.toggleFollowExam(notice.examId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeDetailScreen(
    viewModel: NoticeDetailViewModel,
    onBackClick: () -> Unit,
    onOpenChecklist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Official Notice") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    uiState.notice?.let { notice ->
                        IconButton(onClick = { viewModel.toggleFollow() }) {
                            Icon(
                                imageVector = if (uiState.isFollowing) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Follow Exam",
                                tint = if (uiState.isFollowing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingStateView(modifier = Modifier.padding(paddingValues))
        } else if (uiState.notice == null) {
            EmptyStateView(
                title = "Notice Not Found",
                message = "The requested recruitment notification could not be found or has been superseded."
            )
        } else {
            val notice = uiState.notice!!
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                // Development Fixture Banner
                if (notice.isDemonstrationFixture) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(Color(0xFFFEF3C7), RoundedCornerShape(6.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "DEMONSTRATION TEST FIXTURE — NOT LIVE NOTICE",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF92400E),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Organization & Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = notice.organization,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(text = " • ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        NoticeTypeBadge(noticeType = notice.noticeType)
                    }

                    VerificationBadge(status = notice.verificationStatus)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Title
                Text(
                    text = notice.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Verification and Freshness Info Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Official Gazette Publication",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = DateFormatter.format(notice.publishedDate),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Last Verified",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = DateFormatter.formatRelativeTime(notice.lastCheckedTimestamp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Summary
                Text(
                    text = "Notification Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = notice.summary,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Important Dates Section
                Text(
                    text = "Important Dates & Timelines",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                notice.importantDates.forEach { impDate ->
                    ImportantDateRow(importantDate = impDate)
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Eligibility Summary Section
                notice.eligibilitySummary?.let { eligibility ->
                    Text(
                        text = "Eligibility Criteria (Summary)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = eligibility,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Application Checklist Shortcut
                Button(
                    onClick = { onOpenChecklist(notice.examId) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(imageVector = Icons.Default.Checklist, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Application Checklist")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Official External Links
                Row(modifier = Modifier.fillMaxWidth()) {
                    if (notice.officialApplyUrl != null) {
                        Button(
                            onClick = {
                                CustomTabsHelper.openOfficialUrl(context, notice.officialApplyUrl)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Apply Portal")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    OutlinedButton(
                        onClick = {
                            CustomTabsHelper.openOfficialUrl(context, notice.officialNoticeUrl)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Official Notice")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Independence Disclaimer
                IndependenceDisclaimerBanner()
            }
        }
    }
}

@Composable
fun ImportantDateRow(importantDate: ImportantDateModel) {
    val dateLabel = when (importantDate.dateType) {
        DateType.APPLICATION_START -> "Online Registration Opens"
        DateType.APPLICATION_DEADLINE -> "Application & Fee Deadline"
        DateType.CORRECTION_WINDOW -> "Correction Window Opens"
        DateType.EXAM_DATE -> "Scheduled Examination Date"
        DateType.ADMIT_CARD_RELEASE -> "Admit Card Release"
        DateType.RESULT_DATE -> "Result Announcement"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (importantDate.isTentative) {
                Text(
                    text = "Tentative schedule per official calendar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = DateFormatter.format(importantDate.eventDate),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (importantDate.dateType == DateType.APPLICATION_DEADLINE) DeadlineUrgent else MaterialTheme.colorScheme.primary
        )
    }
}
