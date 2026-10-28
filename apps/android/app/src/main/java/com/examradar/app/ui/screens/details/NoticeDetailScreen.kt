package com.examradar.app.ui.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

                // Decision Enablers Hub: "Should I Apply?" (Vacancies, Salary, Fee, Age Eligibility Calculator)
                DecisionEnablersHub(notice = notice)

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
