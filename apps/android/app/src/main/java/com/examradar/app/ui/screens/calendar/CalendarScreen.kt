package com.examradar.app.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examradar.app.data.model.CalendarEventModel
import com.examradar.app.data.model.DateType
import com.examradar.app.data.repository.ExamRadarRepository
import com.examradar.app.ui.components.EmptyStateView
import com.examradar.app.ui.components.LoadingStateView
import com.examradar.app.ui.theme.*
import com.examradar.app.util.DateFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

data class CalendarUiState(
    val isLoading: Boolean = true,
    val events: List<CalendarEventModel> = emptyList(),
    val isCalendarView: Boolean = false
)

class CalendarViewModel(
    private val repository: ExamRadarRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadEvents()
    }

    private fun loadEvents() {
        viewModelScope.launch {
            repository.getCalendarEvents().collectLatest { events ->
                _uiState.value = _uiState.value.copy(events = events, isLoading = false)
            }
        }
    }

    fun toggleViewMode() {
        _uiState.value = _uiState.value.copy(isCalendarView = !_uiState.value.isCalendarView)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onNoticeClick: (String) -> Unit,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Examination Calendar",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            imageVector = if (uiState.isCalendarView) Icons.AutoMirrored.Filled.List else Icons.Default.CalendarMonth,
                            contentDescription = "Toggle View"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingStateView(modifier = Modifier.padding(paddingValues))
        } else if (uiState.events.isEmpty()) {
            EmptyStateView(
                title = "No Scheduled Dates",
                message = "There are no verified exam or application dates found for your selected goals.",
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            // Group events by Month & Year - remembered to avoid re-allocation on scroll
            val groupedEvents = remember(uiState.events) {
                val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
                uiState.events.groupBy { it.eventDate.format(monthFormatter) }
            }

            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                groupedEvents.forEach { (month, events) ->
                    item(contentType = "month_header") {
                        Text(
                            text = month,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(
                        items = events,
                        key = { it.id },
                        contentType = { "calendar_card" }
                    ) { event ->
                        CalendarEventCard(
                            event = event,
                            onClick = { onNoticeClick(event.noticeId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarEventCard(
    event: CalendarEventModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val day = remember(event.eventDate) { event.eventDate.format(DateTimeFormatter.ofPattern("dd")) }
    val monthShort = remember(event.eventDate) { event.eventDate.format(DateTimeFormatter.ofPattern("MMM")) }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date Column Box
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        if (event.dateType == DateType.APPLICATION_DEADLINE) DeadlineUrgentBg
                        else MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (event.dateType == DateType.APPLICATION_DEADLINE) DeadlineUrgent
                        else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = monthShort,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (event.dateType == DateType.APPLICATION_DEADLINE) DeadlineUrgent
                        else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when (event.dateType) {
                            DateType.APPLICATION_START -> "Registration Opens"
                            DateType.APPLICATION_DEADLINE -> "Application Deadline"
                            DateType.CORRECTION_WINDOW -> "Correction Window"
                            DateType.EXAM_DATE -> "Exam Date"
                            DateType.ADMIT_CARD_RELEASE -> "Admit Card"
                            DateType.RESULT_DATE -> "Result Release"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (event.dateType == DateType.APPLICATION_DEADLINE) DeadlineUrgent
                        else MaterialTheme.colorScheme.primary
                    )

                    if (event.isTentative) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFECEFF1), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(text = "Tentative", fontSize = 10.sp, color = Color(0xFF455A64))
                        }
                    }

                    if (event.isRevised) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(StatusRevisedBg, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(text = "Revised", fontSize = 10.sp, color = StatusRevised)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = event.examName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = event.organization,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
