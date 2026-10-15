package com.examradar.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
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
import com.examradar.app.data.fixture.DevelopmentNoticeFixtures
import com.examradar.app.data.repository.ExamRadarRepository
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val repository: ExamRadarRepository
) : ViewModel() {

    var selectedCategories by mutableStateOf(setOf("cat-upsc", "cat-ssc", "cat-banking"))
        private set

    var selectedExams by mutableStateOf(setOf("exam-upsc-cse", "exam-ssc-cgl", "exam-ibps-po"))
        private set

    var educationLevel by mutableStateOf("GRADUATE_ANY")
        private set

    var graduationStatus by mutableStateOf("GRADUATED")
        private set

    var reminder7Days by mutableStateOf(true)
        private set

    var reminder3Days by mutableStateOf(true)
        private set

    var reminder1Day by mutableStateOf(true)
        private set

    fun toggleCategory(categoryId: String) {
        selectedCategories = if (selectedCategories.contains(categoryId)) {
            selectedCategories - categoryId
        } else {
            selectedCategories + categoryId
        }
    }

    fun toggleExam(examId: String) {
        selectedExams = if (selectedExams.contains(examId)) {
            selectedExams - examId
        } else {
            selectedExams + examId
        }
    }

    fun setEducation(level: String) { educationLevel = level }
    fun setStatus(status: String) { graduationStatus = status }
    fun toggleReminder7(value: Boolean) { reminder7Days = value }
    fun toggleReminder3(value: Boolean) { reminder3Days = value }
    fun toggleReminder1(value: Boolean) { reminder1Day = value }

    fun completeOnboarding(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.updateFollowedCategories(selectedCategories)
            repository.updateFollowedExams(selectedExams)
            repository.updateProfile(educationLevel, graduationStatus, 2024)
            repository.updateNotificationPreferences(
                notifyNew = true,
                notifyDeadlines = true,
                notifyResults = true,
                r7 = reminder7Days,
                r3 = reminder3Days,
                r1 = reminder1Day,
                push = true
            )
            repository.setOnboardingCompleted(true)
            onSuccess()
        }
    }
}

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit
) {
    var currentStep by remember { mutableStateOf(1) }
    val scrollState = rememberScrollState()

    Scaffold(
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(onClick = { currentStep-- }) {
                            Text("Back")
                        }
                    } else {
                        TextButton(onClick = {
                            viewModel.completeOnboarding(onComplete)
                        }) {
                            Text("Skip Setup")
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep < 3) {
                                currentStep++
                            } else {
                                viewModel.completeOnboarding(onComplete)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(if (currentStep == 3) "Start Tracking" else "Next")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            // Header
            Text(
                text = "ExamRadar India",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when (currentStep) {
                    1 -> "Select Target Categories"
                    2 -> "Select Target Examinations"
                    else -> "Education & Reminders"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = when (currentStep) {
                    1 -> "Choose government examination categories relevant to your preparation goals."
                    2 -> "Follow specific recruitment examinations to receive date and deadline alerts."
                    else -> "Personalize eligibility filters and deadline reminder frequencies."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (currentStep) {
                1 -> {
                    // Step 1: Categories
                    DevelopmentNoticeFixtures.categories.forEach { category ->
                        val isSelected = viewModel.selectedCategories.contains(category.id)
                        CategorySelectionCard(
                            title = category.name,
                            description = category.description,
                            isSelected = isSelected,
                            onToggle = { viewModel.toggleCategory(category.id) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
                2 -> {
                    // Step 2: Specific Exams
                    DevelopmentNoticeFixtures.exams.forEach { exam ->
                        val isSelected = viewModel.selectedExams.contains(exam.id)
                        ExamSelectionCard(
                            examName = exam.examName,
                            organization = exam.organization,
                            code = exam.examCode,
                            isSelected = isSelected,
                            onToggle = { viewModel.toggleExam(exam.id) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
                3 -> {
                    // Step 3: Education & Reminders
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Why is this asked? Your educational stream and graduation status are used exclusively to filter candidate notices against official qualification rules. This information is private and optional.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Graduation Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = viewModel.graduationStatus == "GRADUATED",
                            onClick = { viewModel.setStatus("GRADUATED") },
                            label = { Text("Graduated") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = viewModel.graduationStatus == "PURSUING_FINAL_YEAR",
                            onClick = { viewModel.setStatus("PURSUING_FINAL_YEAR") },
                            label = { Text("Final Year Student") }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Deadline Reminder Windows",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ReminderOptionRow(
                        title = "7 Days Before Deadline",
                        description = "Initial alert when closing window approaches",
                        checked = viewModel.reminder7Days,
                        onCheckedChange = { viewModel.toggleReminder7(it) }
                    )
                    ReminderOptionRow(
                        title = "3 Days Before Deadline",
                        description = "Urgent reminder to complete fee payment",
                        checked = viewModel.reminder3Days,
                        onCheckedChange = { viewModel.toggleReminder3(it) }
                    )
                    ReminderOptionRow(
                        title = "1 Day Before Deadline",
                        description = "Final closing day alert",
                        checked = viewModel.reminder1Day,
                        onCheckedChange = { viewModel.toggleReminder1(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun CategorySelectionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(10.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
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
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
        }
    }
}

@Composable
fun ExamSelectionCard(
    examName: String,
    organization: String,
    code: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(10.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = code,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = " • ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = organization,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = examName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
        }
    }
}

@Composable
fun ReminderOptionRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
