package com.examradar.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examradar.app.data.model.*
import com.examradar.app.data.repository.ExamRadarRepository
import com.examradar.app.ui.components.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val followedExamsCount: Int = 0,
    val approachingDeadlines: List<NoticeModel> = emptyList(),
    val notices: List<NoticeModel> = emptyList(),
    val freshnessInfo: SourceFreshnessInfo? = null,
    val selectedCategoryFilter: String? = null,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val repository: ExamRadarRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            launch {
                repository.getSourceFreshness().collectLatest { freshness ->
                    _uiState.value = _uiState.value.copy(freshnessInfo = freshness)
                }
            }

            launch {
                repository.getUserPreferences().collectLatest { prefs ->
                    _uiState.value = _uiState.value.copy(followedExamsCount = prefs.followedExams.size)
                }
            }

            launch {
                repository.getNotices(onlyApproachingDeadlines = true).collectLatest { deadlines ->
                    _uiState.value = _uiState.value.copy(approachingDeadlines = deadlines)
                }
            }

            launch {
                repository.getNotices(categoryFilter = _uiState.value.selectedCategoryFilter).collectLatest { notices ->
                    _uiState.value = _uiState.value.copy(
                        notices = notices,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun selectCategoryFilter(categoryId: String?) {
        _uiState.value = _uiState.value.copy(selectedCategoryFilter = categoryId, isLoading = true)
        viewModelScope.launch {
            repository.getNotices(categoryFilter = categoryId).collectLatest { notices ->
                _uiState.value = _uiState.value.copy(
                    notices = notices,
                    isLoading = false
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNoticeClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ExamRadar India",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tracking ${uiState.followedExamsCount} target examinations",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNotificationsClick) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
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
        if (uiState.isLoading) {
            LoadingStateView(modifier = Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Source Freshness Banner
                uiState.freshnessInfo?.let { freshness ->
                    item {
                        FreshnessBanner(freshnessInfo = freshness)
                    }
                }

                // Approaching Deadlines Section
                if (uiState.approachingDeadlines.isNotEmpty()) {
                    item {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Approaching Application Deadlines",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            uiState.approachingDeadlines.forEach { urgentNotice ->
                                NoticeCard(
                                    notice = urgentNotice,
                                    onClick = { onNoticeClick(urgentNotice.id) }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }

                // Category Filter Chips
                item {
                    Column {
                        Text(
                            text = "Latest Official Announcements",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = uiState.selectedCategoryFilter == null,
                                onClick = { viewModel.selectCategoryFilter(null) },
                                label = { Text("All Goals") }
                            )
                            FilterChip(
                                selected = uiState.selectedCategoryFilter == "cat-upsc",
                                onClick = { viewModel.selectCategoryFilter("cat-upsc") },
                                label = { Text("UPSC") }
                            )
                            FilterChip(
                                selected = uiState.selectedCategoryFilter == "cat-ssc",
                                onClick = { viewModel.selectCategoryFilter("cat-ssc") },
                                label = { Text("SSC") }
                            )
                            FilterChip(
                                selected = uiState.selectedCategoryFilter == "cat-banking",
                                onClick = { viewModel.selectCategoryFilter("cat-banking") },
                                label = { Text("Banking") }
                            )
                            FilterChip(
                                selected = uiState.selectedCategoryFilter == "cat-regulatory",
                                onClick = { viewModel.selectCategoryFilter("cat-regulatory") },
                                label = { Text("Regulatory") }
                            )
                        }
                    }
                }

                // Main Notices List
                if (uiState.notices.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "No Announcements in Category",
                            message = "There are no current notices for this filter. Check back after official updates."
                        )
                    }
                } else {
                    items(uiState.notices, key = { it.id }) { notice ->
                        NoticeCard(
                            notice = notice,
                            onClick = { onNoticeClick(notice.id) }
                        )
                    }
                }

                // Independence Disclaimer Footer
                item {
                    IndependenceDisclaimerBanner()
                }
            }
        }
    }
}
