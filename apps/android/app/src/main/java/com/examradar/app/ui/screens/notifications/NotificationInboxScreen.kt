package com.examradar.app.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examradar.app.data.model.NotificationInboxItemModel
import com.examradar.app.data.repository.ExamRadarRepository
import com.examradar.app.ui.components.EmptyStateView
import com.examradar.app.ui.components.LoadingStateView
import com.examradar.app.util.DateFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationInboxUiState(
    val isLoading: Boolean = true,
    val alerts: List<NotificationInboxItemModel> = emptyList(),
    val selectedFilter: String = "ALL", // "ALL", "DEADLINES", "NOTICES", "CORRECTIONS"
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
) {
    val filteredAlerts: List<NotificationInboxItemModel>
        get() = when (selectedFilter) {
            "DEADLINES" -> alerts.filter {
                it.eventType.contains("DEADLINE", ignoreCase = true) ||
                        it.title.contains("Day", ignoreCase = true) ||
                        it.title.contains("Urgent", ignoreCase = true)
            }
            "NOTICES" -> alerts.filter {
                it.eventType.contains("NEW", ignoreCase = true) ||
                        it.eventType.contains("NOTICE", ignoreCase = true) ||
                        it.eventType.contains("NOTIFICATION", ignoreCase = true)
            }
            "CORRECTIONS" -> alerts.filter {
                it.eventType.contains("CORRECTION", ignoreCase = true)
            }
            else -> alerts
        }
}

class NotificationInboxViewModel(
    private val repository: ExamRadarRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationInboxUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadInbox()
    }

    private fun loadInbox() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getNotificationInbox().collectLatest { items ->
                _uiState.update { it.copy(isLoading = false, alerts = items) }
            }
        }
    }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                repository.refreshNotificationInbox()
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationInboxScreen(
    viewModel: NotificationInboxViewModel,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNoticeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Alerts & Notifications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.alerts.size} updates tracked",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Notification Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading && uiState.alerts.isEmpty()) {
            LoadingStateView(modifier = Modifier.padding(paddingValues))
        } else {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Filter Chips Row
                    NotificationFilterRow(
                        selectedFilter = uiState.selectedFilter,
                        alerts = uiState.alerts,
                        onFilterSelected = { viewModel.setFilter(it) }
                    )

                    val filteredList = uiState.filteredAlerts

                    if (filteredList.isEmpty()) {
                        EmptyStateView(
                            title = "No Alerts Found",
                            message = when (uiState.selectedFilter) {
                                "DEADLINES" -> "No approaching deadlines right now for your followed exams."
                                "NOTICES" -> "No new recruitment notices matching your active criteria."
                                "CORRECTIONS" -> "No active correction windows currently open."
                                else -> "You're all caught up! New exam releases and deadline alerts will appear here."
                            },
                            icon = Icons.Default.NotificationsNone,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredList, key = { it.id }) { alert ->
                                NotificationInboxCard(
                                    alert = alert,
                                    onClick = {
                                        alert.noticeId?.let { noticeId ->
                                            onNoticeClick(noticeId)
                                        }
                                    }
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
fun NotificationFilterRow(
    selectedFilter: String,
    alerts: List<NotificationInboxItemModel>,
    onFilterSelected: (String) -> Unit
) {
    val deadlineCount = alerts.count {
        it.eventType.contains("DEADLINE", ignoreCase = true) ||
                it.title.contains("Day", ignoreCase = true) ||
                it.title.contains("Urgent", ignoreCase = true)
    }
    val noticeCount = alerts.count {
        it.eventType.contains("NEW", ignoreCase = true) ||
                it.eventType.contains("NOTICE", ignoreCase = true) ||
                it.eventType.contains("NOTIFICATION", ignoreCase = true)
    }
    val correctionCount = alerts.count {
        it.eventType.contains("CORRECTION", ignoreCase = true)
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { onFilterSelected("ALL") },
                label = { Text("All (${alerts.size})") }
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == "DEADLINES",
                onClick = { onFilterSelected("DEADLINES") },
                label = { Text("⏳ Deadlines ($deadlineCount)") }
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == "NOTICES",
                onClick = { onFilterSelected("NOTICES") },
                label = { Text("📢 New Notices ($noticeCount)") }
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == "CORRECTIONS",
                onClick = { onFilterSelected("CORRECTIONS") },
                label = { Text("✏️ Correction ($correctionCount)") }
            )
        }
    }
}

@Composable
fun NotificationInboxCard(
    alert: NotificationInboxItemModel,
    onClick: () -> Unit
) {
    val isDeadline = alert.eventType.contains("DEADLINE", ignoreCase = true) ||
            alert.title.contains("Day", ignoreCase = true) ||
            alert.title.contains("Urgent", ignoreCase = true)
    val isCorrection = alert.eventType.contains("CORRECTION", ignoreCase = true)

    val (badgeBg, badgeTextColor, badgeIcon, categoryLabel) = when {
        isDeadline -> Tuple4(
            Color(0xFFFEF2F2),
            Color(0xFFDC2626),
            Icons.Default.HourglassTop,
            "DEADLINE ALERT"
        )
        isCorrection -> Tuple4(
            Color(0xFFF3E8FF),
            Color(0xFF7C3AED),
            Icons.Default.EditCalendar,
            "CORRECTION WINDOW"
        )
        else -> Tuple4(
            Color(0xFFEFF6FF),
            Color(0xFF2563EB),
            Icons.Default.Campaign,
            "OFFICIAL RELEASE"
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = alert.noticeId != null, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Category Badge + Relative Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = badgeTextColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = categoryLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeTextColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }

                Text(
                    text = DateFormatter.formatRelativeTime(alert.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Examination & Authority tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = alert.organization,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = alert.examName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Alert Title
            Text(
                text = alert.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Alert Message
            Text(
                text = alert.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            // Click-through Action Row if noticeId is present
            if (alert.noticeId != null) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "View Notice Details",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
