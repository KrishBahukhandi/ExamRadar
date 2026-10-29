package com.examradar.app.ui.screens.details

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.examradar.app.data.model.NoticeVersionModel
import com.examradar.app.util.CustomTabsHelper
import com.examradar.app.util.DateFormatter
import com.examradar.app.util.NoticePdfManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoticeDetailUiState(
    val isLoading: Boolean = true,
    val notice: NoticeModel? = null,
    val isFollowing: Boolean = false,
    val versions: List<NoticeVersionModel> = emptyList(),
    val isPdfDownloading: Boolean = false,
    val isPdfCached: Boolean = false,
    val pdfSize: String? = null
)

class NoticeDetailViewModel(
    private val repository: ExamRadarRepository,
    private val noticeId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoticeDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadNotice()
        loadVersions()
    }

    private fun loadNotice() {
        viewModelScope.launch {
            repository.getNoticeById(noticeId).collectLatest { notice ->
                if (notice != null) {
                    repository.getExamById(notice.examId).collectLatest { exam ->
                        _uiState.update { current ->
                            current.copy(
                                notice = notice,
                                isFollowing = exam?.isFollowing ?: false,
                                isLoading = false
                            )
                        }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private fun loadVersions() {
        viewModelScope.launch {
            repository.getNoticeVersions(noticeId).collectLatest { vers ->
                _uiState.update { it.copy(versions = vers) }
            }
        }
    }

    fun checkPdfCache(context: Context) {
        val cached = NoticePdfManager.isPdfCached(context, noticeId)
        val size = if (cached) NoticePdfManager.getCachedPdfSize(context, noticeId) else null
        _uiState.update { it.copy(isPdfCached = cached, pdfSize = size) }
    }

    fun downloadPdf(context: Context) {
        val notice = _uiState.value.notice ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isPdfDownloading = true) }
            try {
                NoticePdfManager.downloadAndCachePdf(
                    context = context,
                    noticeId = notice.id,
                    url = notice.officialNoticeUrl,
                    isDemoFixture = notice.isDemonstrationFixture
                )
                checkPdfCache(context)
            } finally {
                _uiState.update { it.copy(isPdfDownloading = false) }
            }
        }
    }

    fun openPdf(context: Context) {
        NoticePdfManager.openCachedPdf(context, noticeId)
    }

    fun sharePdf(context: Context) {
        val notice = _uiState.value.notice ?: return
        NoticePdfManager.shareCachedPdf(context, noticeId, notice.title)
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

    LaunchedEffect(uiState.notice?.id) {
        viewModel.checkPdfCache(context)
    }

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
                        IconButton(onClick = { shareNotice(context, notice) }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Notice",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
            var selectedLanguage by rememberSaveable { mutableStateOf("en") }
            val hasHindi = !notice.titleHi.isNullOrBlank() || !notice.summaryHi.isNullOrBlank()
            val displayTitle = if (selectedLanguage == "hi" && !notice.titleHi.isNullOrBlank()) notice.titleHi!! else notice.title
            val displaySummary = if (selectedLanguage == "hi" && !notice.summaryHi.isNullOrBlank()) notice.summaryHi!! else notice.summary

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

                // Language Accessibility Toggle (English / हिंदी)
                LanguageToggleBar(
                    selectedLanguage = selectedLanguage,
                    onLanguageSelected = { selectedLanguage = it },
                    hasHindiContent = hasHindi
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Title (Bilingual)
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Official Corrigendum / Amendment Alert Banner
                if (notice.hasCorrigendum) {
                    Spacer(modifier = Modifier.height(10.dp))
                    CorrigendumAlertBanner(
                        summary = notice.corrigendumSummary,
                        versions = uiState.versions
                    )
                }

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
                                text = if (selectedLanguage == "hi") "आधिकारिक प्रकाशन तिथि" else "Official Gazette Publication",
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
                                text = if (selectedLanguage == "hi") "अंतिम सत्यापन" else "Last Verified",
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

                // Summary (Bilingual)
                Text(
                    text = if (selectedLanguage == "hi") "अधिसूचना सारांश (Summary)" else "Notification Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = displaySummary,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Decision Enablers Hub: "Should I Apply?" (Vacancies, Salary, Fee, Age Eligibility Calculator)
                DecisionEnablersHub(notice = notice)

                // Exam Pattern & Selection Stages Snapshot
                if (notice.examStages != null || notice.negativeMarking != null || notice.examMode != null || notice.syllabusUrl != null) {
                    Spacer(modifier = Modifier.height(18.dp))
                    ExamPatternCard(
                        notice = notice,
                        selectedLanguage = selectedLanguage
                    )
                }

                // Avoid Form Rejection: Document & Upload Guidelines Card
                if (notice.photoSpecifications != null || notice.signatureSpecifications != null || notice.feePaymentDeadline != null) {
                    Spacer(modifier = Modifier.height(18.dp))
                    UploadGuidelinesCard(notice = notice)
                }

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

                Spacer(modifier = Modifier.height(14.dp))

                // WhatsApp / Social Share Card for Aspirants & Study Groups
                NoticeShareCard(
                    notice = notice,
                    onShareClick = { shareNotice(context, notice) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Official Notice PDF Hub (Offline Cache & 0ms Local Reader)
                OfflineNoticePdfCard(
                    notice = notice,
                    isCached = uiState.isPdfCached,
                    cachedSize = uiState.pdfSize,
                    isDownloading = uiState.isPdfDownloading,
                    onDownloadClick = { viewModel.downloadPdf(context) },
                    onOpenClick = { viewModel.openPdf(context) },
                    onShareClick = { viewModel.sharePdf(context) }
                )

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

@Composable
fun DecisionEnablersHub(notice: NoticeModel) {
    val hasDecisionMetrics = notice.totalVacancies != null ||
            notice.payScaleSummary != null ||
            notice.applicationFeeGeneral != null ||
            notice.ageMax != null ||
            notice.educationalQualification != null

    if (!hasDecisionMetrics) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Key Decision Enablers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "Should I Apply?",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 1. Vacancy Card
        if (notice.totalVacancies != null) {
            VacancyBreakdownCard(
                totalVacancies = notice.totalVacancies,
                breakdownJson = notice.vacancyBreakdownJson
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 2. Pay Scale & Salary Card
        if (notice.payScaleSummary != null) {
            SalaryAndPayCard(payScale = notice.payScaleSummary)
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 3. Application Fee Card
        if (notice.applicationFeeGeneral != null) {
            ApplicationFeeCard(
                feeGeneral = notice.applicationFeeGeneral,
                feeReserved = notice.applicationFeeReserved,
                feeDetails = notice.feeExemptionDetails
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 4. Interactive Age Eligibility Calculator
        if (notice.ageMin != null && notice.ageMax != null) {
            AgeEligibilityCalculatorCard(
                ageMin = notice.ageMin,
                ageMax = notice.ageMax,
                cutoffDate = notice.ageCutoffDate,
                relaxations = notice.ageRelaxationSummary
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 5. Educational Qualification Card
        if (notice.educationalQualification != null) {
            EducationalQualificationCard(qualification = notice.educationalQualification)
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun VacancyBreakdownCard(
    totalVacancies: Int,
    breakdownJson: String?
) {
    val breakdownMap: Map<String, Any> = remember(breakdownJson) {
        if (!breakdownJson.isNullOrBlank()) {
            runCatching {
                val type = object : TypeToken<Map<String, Any>>() {}.type
                Gson().fromJson<Map<String, Any>>(breakdownJson, type)
            }.getOrDefault(emptyMap())
        } else emptyMap()
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Total Official Vacancies",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "%,d Posts".format(totalVacancies),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (breakdownMap.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Category-Wise Reservation Distribution:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    breakdownMap.entries.take(6).forEach { entry ->
                        val count = (entry.value as? Number)?.toInt() ?: entry.value.toString().toIntOrNull() ?: 0
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = entry.key,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "%,d".format(count),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SalaryAndPayCard(payScale: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0F766E).copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Payments,
                contentDescription = null,
                tint = Color(0xFF0F766E),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Remuneration & Pay Scale",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF0F766E),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = payScale,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF134E4A)
                )
            }
        }
    }
}

@Composable
fun ApplicationFeeCard(
    feeGeneral: Int,
    feeReserved: Int?,
    feeDetails: String?
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF4338CA).copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = Color(0xFF4338CA),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Application Fee",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF312E81)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF4338CA).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (feeGeneral == 0) "Gen: ₹0" else "Gen/OBC: ₹$feeGeneral",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF312E81),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if ((feeReserved ?: 0) == 0) "SC/ST/Fem: ₹0" else "Reserved: ₹$feeReserved",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (!feeDetails.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = feeDetails,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF374151),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun AgeEligibilityCalculatorCard(
    ageMin: Int,
    ageMax: Int,
    cutoffDate: java.time.LocalDate?,
    relaxations: String?
) {
    var selectedCategory by remember { mutableStateOf("General / EWS") }
    var birthYear by remember { mutableIntStateOf(2001) }

    val cutoffYear = cutoffDate?.year ?: 2025
    val calculatedAge = cutoffYear - birthYear

    val categoryRelaxation = when (selectedCategory) {
        "OBC (NCL)" -> 3
        "SC / ST" -> 5
        "PwBD" -> 10
        else -> 0
    }
    val effectiveMaxAge = ageMax + categoryRelaxation
    val isEligible = calculatedAge in ageMin..effectiveMaxAge

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Age Eligibility Calculator",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "$ageMin – $ageMax Years",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (cutoffDate != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Critical Cut-Off Date: as of ${DateFormatter.format(cutoffDate)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Selector Chips
            Text(
                text = "Select Your Category:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("General / EWS", "OBC (NCL)", "SC / ST", "PwBD").forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 10.sp,
                            maxLines = 1,
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Birth Year Stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Birth Year:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (birthYear > 1980) birthYear-- },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = "$birthYear",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = { if (birthYear < 2010) birthYear++ },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = "Increase")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Eligibility Outcome Card
            val resultColor = if (isEligible) Color(0xFF059669) else Color(0xFFDC2626)
            val resultBg = if (isEligible) Color(0xFFECFDF5) else Color(0xFFFEF2F2)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = resultBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isEligible) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = resultColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        val statusHeadline = if (isEligible) {
                            if (calculatedAge > ageMax) {
                                "Eligible via $selectedCategory Relaxation! (+$categoryRelaxation yrs)"
                            } else {
                                "Eligible for this examination!"
                            }
                        } else {
                            if (calculatedAge > effectiveMaxAge) {
                                "Exceeds age limit for $selectedCategory"
                            } else {
                                "Under minimum age requirement"
                            }
                        }

                        Text(
                            text = statusHeadline,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = resultColor
                        )
                        Text(
                            text = "Age on cutoff: $calculatedAge yrs • Max allowed for $selectedCategory: $effectiveMaxAge yrs",
                            style = MaterialTheme.typography.bodySmall,
                            color = resultColor.copy(alpha = 0.9f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (!relaxations.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Official Category Relaxations: $relaxations",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun EducationalQualificationCard(qualification: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF334155).copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = Color(0xFF334155),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Educational Qualification & Degree Criteria",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF334155),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = qualification,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1E293B)
                )
            }
        }
    }
}

@Composable
fun NoticeShareCard(
    notice: NoticeModel,
    onShareClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF0FDF4)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF16A34A)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Share with Study Group",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF14532D)
                    )
                    Text(
                        text = "Formatted card ready for WhatsApp & Telegram groups",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF15803D)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onShareClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF16A34A),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Share Notification Card",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun shareNotice(context: Context, notice: NoticeModel) {
    val deadline = notice.importantDates.find { it.dateType == DateType.APPLICATION_DEADLINE }?.eventDate
    val deadlineStr = if (deadline != null) DateFormatter.format(deadline) else "Check Official Notification"

    val textBuilder = StringBuilder()
    textBuilder.append("📢 *${notice.title}*\n")
    textBuilder.append("🏛️ *Authority:* ${notice.organization}\n")
    if (notice.totalVacancies != null && notice.totalVacancies > 0) {
        val formattedVacancies = java.text.NumberFormat.getIntegerInstance(java.util.Locale("en", "IN")).format(notice.totalVacancies)
        textBuilder.append("🔢 *Total Vacancies:* $formattedVacancies Posts\n")
    }
    if (!notice.payScaleSummary.isNullOrBlank()) {
        textBuilder.append("💰 *Pay Scale:* ${notice.payScaleSummary}\n")
    }
    textBuilder.append("⏳ *Last Date to Apply:* $deadlineStr\n")
    if (!notice.educationalQualification.isNullOrBlank()) {
        textBuilder.append("🎓 *Qualification:* ${notice.educationalQualification}\n")
    }
    if (notice.ageMin != null && notice.ageMax != null) {
        textBuilder.append("🎂 *Age Criteria:* ${notice.ageMin} - ${notice.ageMax} Years\n")
    }
    if (notice.applicationFeeGeneral != null) {
        val feeGeneralStr = if (notice.applicationFeeGeneral == 0) "Free" else "₹${notice.applicationFeeGeneral}"
        val feeReservedStr = if (notice.applicationFeeReserved != null) {
            if (notice.applicationFeeReserved == 0) "Free (Exempt)" else "₹${notice.applicationFeeReserved}"
        } else "Exempt for reserved categories"
        textBuilder.append("💳 *Application Fee:* $feeGeneralStr (Reserved: $feeReservedStr)\n")
    }
    if (!notice.officialApplyUrl.isNullOrBlank()) {
        textBuilder.append("\n🔗 *Apply Online:* ${notice.officialApplyUrl}\n")
    }
    textBuilder.append("📄 *Official PDF Notice:* ${notice.officialNoticeUrl}\n")
    textBuilder.append("\n_Shared via ExamRadar India — Real-Time Govt Exam Notice Tracker_")

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "${notice.organization}: ${notice.examName} Notification")
        putExtra(Intent.EXTRA_TEXT, textBuilder.toString())
    }
    val chooser = Intent.createChooser(sendIntent, "Share Exam Notice via...")
    context.startActivity(chooser)
}

@Composable
fun CorrigendumAlertBanner(
    summary: String?,
    versions: List<NoticeVersionModel>
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFBEB)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFD97706)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(14.dp)
                        )
                    }
                    Text(
                        text = "OFFICIAL CORRIGENDUM ISSUED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E)
                    )
                }

                if (versions.size > 1) {
                    TextButton(
                        onClick = { expanded = !expanded },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (expanded) "Hide History ▲" else "What Changed? (${versions.size}) ▼",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = summary ?: "Official corrigendum notice issued. Important recruitment dates or rules have been revised.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF78350F)
            )

            if (expanded && versions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFFDE68A))
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Official Revision Timeline",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF92400E)
                )

                Spacer(modifier = Modifier.height(6.dp))

                versions.forEach { ver ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (ver.versionNumber > 1) Color(0xFFFEF3C7) else Color(0xFFE2E8F0)
                        ) {
                            Text(
                                text = "v${ver.versionNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (ver.versionNumber > 1) Color(0xFF92400E) else Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ver.changeSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF451A03),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = DateFormatter.formatRelativeTime(ver.recordedAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF92400E).copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UploadGuidelinesCard(notice: NoticeModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8FAFC)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(16.dp)
                        )
                    }
                    Text(
                        text = "Avoid Form Rejection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2)
                ) {
                    Text(
                        text = "Strict Govt Rules",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Photo specification
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Photograph & Live Capture Rules",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = notice.photoSpecifications ?: "20 KB – 50 KB JPG. Plain white background, clear frontal face (80% coverage). Strictly NO glasses, caps, or side angles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Signature specification
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF3E8FF),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Draw,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Signature Upload Criteria",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = notice.signatureSpecifications ?: "10 KB to 20 KB in JPG format. Black ballpoint pen on plain white paper in running handwriting (block capital letters rejected).",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                }
            }

            // Fee window difference callout
            if (notice.feePaymentDeadline != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFFBEB),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "Fee Payment Window Extension",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = "Online registration closes ${DateFormatter.format(notice.applicationDeadline)}, but online fee payment remains open until ${DateFormatter.format(notice.feePaymentDeadline)}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF78350F)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OfflineNoticePdfCard(
    notice: NoticeModel,
    isCached: Boolean,
    cachedSize: String?,
    isDownloading: Boolean,
    onDownloadClick: () -> Unit,
    onOpenClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCached) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCached) Color(0xFFBBF7D0) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isCached) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = if (isCached) Icons.Default.CheckCircle else Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isCached) "Official PDF Cached (${cachedSize ?: "Offline"})" else "Official Notice PDF",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isCached) Color(0xFF14532D) else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isCached) "Ready for instant 0ms offline reading & sharing" else "Cache locally for fast access when govt servers crash",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCached) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isCached) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Read Offline")
                    }

                    OutlinedButton(
                        onClick = onShareClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share PDF")
                    }
                }
            } else {
                Button(
                    onClick = onDownloadClick,
                    enabled = !isDownloading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Downloading PDF...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download PDF for Offline Reading")
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageToggleBar(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    hasHindiContent: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // English Pill
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (selectedLanguage == "en") MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .clickable { onLanguageSelected("en") }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🇬🇧 English",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selectedLanguage == "en") FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedLanguage == "en") MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Hindi Pill
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (selectedLanguage == "hi") MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
                    .clickable { onLanguageSelected("hi") }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🇮🇳 हिंदी",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selectedLanguage == "hi") FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedLanguage == "hi") MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (hasHindiContent) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFDCFCE7),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(
                    text = "Bilingual",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF15803D),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun ExamPatternCard(
    notice: NoticeModel,
    selectedLanguage: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isHindi = selectedLanguage == "hi"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = if (isHindi) "परीक्षा पैटर्न एवं चयन चरण" else "Exam Pattern & Selection Stages",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isHindi) "आधिकारिक चयन प्रक्रिया एवं अंकन योजना" else "Official selection scheme and marking rules",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Badges row: Exam Mode & Negative Marking
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                notice.examMode?.let { mode ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🖥️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = if (isHindi) "परीक्षा मोड" else "Exam Mode",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = mode,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                notice.negativeMarking?.let { marking ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF2F2),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "⚠️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = if (isHindi) "नकारात्मक अंकन" else "Negative Marking",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF991B1B).copy(alpha = 0.7f)
                                )
                                Text(
                                    text = marking,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B)
                                )
                            }
                        }
                    }
                }
            }

            // Stages Pipeline
            notice.examStages?.let { stagesStr ->
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = if (isHindi) "चयन के विभिन्न चरण (Stages)" else "Selection Process Pipeline",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                val stageList = stagesStr.split(Regex("(?=Stage \\d+:|Phase-\\w+:)|, Stage |, Phase |; ")).filter { it.isNotBlank() }
                stageList.forEachIndexed { index, stageText ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stageText.trim().removePrefix(",").trim(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Syllabus Action
            notice.syllabusUrl?.let { syllabusLink ->
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = { CustomTabsHelper.openOfficialUrl(context, syllabusLink) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "विस्तृत आधिकारिक पाठ्यक्रम देखें (Syllabus PDF)" else "View Official Detailed Syllabus (PDF)",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}



