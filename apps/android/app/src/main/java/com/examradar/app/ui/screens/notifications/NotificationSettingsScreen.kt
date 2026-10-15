package com.examradar.app.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examradar.app.data.preferences.UserPreferences
import com.examradar.app.data.repository.ExamRadarRepository
import com.examradar.app.ui.screens.onboarding.ReminderOptionRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationSettingsViewModel(
    private val repository: ExamRadarRepository
) : ViewModel() {

    private val _prefs = MutableStateFlow(UserPreferences())
    val prefs = _prefs.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getUserPreferences().collectLatest {
                _prefs.value = it
            }
        }
    }

    fun updateSettings(
        notifyNew: Boolean = _prefs.value.notifyNewNotices,
        notifyDeadlines: Boolean = _prefs.value.notifyDeadlines,
        notifyResults: Boolean = _prefs.value.notifyResults,
        r7: Boolean = _prefs.value.reminder7Days,
        r3: Boolean = _prefs.value.reminder3Days,
        r1: Boolean = _prefs.value.reminder1Day,
        push: Boolean = _prefs.value.pushEnabled
    ) {
        viewModelScope.launch {
            repository.updateNotificationPreferences(notifyNew, notifyDeadlines, notifyResults, r7, r3, r1, push)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    viewModel: NotificationSettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.prefs.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Alerts") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Master Push Switch
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Push Notifications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Receive alerts for deadlines and recruitment notices",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = prefs.pushEnabled,
                        onCheckedChange = { viewModel.updateSettings(push = it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Notice Types",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            ReminderOptionRow(
                title = "New Recruitment Notifications",
                description = "Notices for newly announced government exams",
                checked = prefs.notifyNewNotices,
                onCheckedChange = { viewModel.updateSettings(notifyNew = it) }
            )
            ReminderOptionRow(
                title = "Application Deadline Reminders",
                description = "Countdown alerts before closing dates",
                checked = prefs.notifyDeadlines,
                onCheckedChange = { viewModel.updateSettings(notifyDeadlines = it) }
            )
            ReminderOptionRow(
                title = "Results & Answer Keys",
                description = "Scorecards, cut-offs and official keys",
                checked = prefs.notifyResults,
                onCheckedChange = { viewModel.updateSettings(notifyResults = it) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Deadline Reminder Intervals",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            ReminderOptionRow(
                title = "7 Days Prior",
                description = "Advance notice to review eligibility & documents",
                checked = prefs.reminder7Days,
                onCheckedChange = { viewModel.updateSettings(r7 = it) }
            )
            ReminderOptionRow(
                title = "3 Days Prior",
                description = "Urgent reminder to complete registration",
                checked = prefs.reminder3Days,
                onCheckedChange = { viewModel.updateSettings(r3 = it) }
            )
            ReminderOptionRow(
                title = "1 Day Prior (Closing Eve)",
                description = "Final alert to prevent server slowdown rush",
                checked = prefs.reminder1Day,
                onCheckedChange = { viewModel.updateSettings(r1 = it) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Spam & Idempotency Guarantee
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Zero Spam Guarantee: ExamRadar uses cryptographic idempotency hashing. You will never receive duplicate notifications for the same notice revision or unverified rumors.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
