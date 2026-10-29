package com.examradar.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
    val categories: List<CategoryModel> = emptyList(),
    val freshnessInfo: SourceFreshnessInfo? = null,
    val selectedCategoryFilter: String? = null,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val repository: ExamRadarRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    var isRefreshing by mutableStateOf(false)
        private set

    init {
        loadData()
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing = true
            try {
                repository.refreshNotices()
            } finally {
                isRefreshing = false
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            launch {
                repository.refreshNotices()
            }

            launch {
                repository.getCategories().collectLatest { cats ->
                    _uiState.value = _uiState.value.copy(categories = cats)
                }
            }

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
    onSearchClick: () -> Unit = {},
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
                            text = if (uiState.followedExamsCount > 0) "Tracking ${uiState.followedExamsCount} target examinations" else "Official Government Exam Notices",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Examinations",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
            val listState = rememberLazyListState()
            PullToRefreshBox(
                isRefreshing = viewModel.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                // Source Freshness Banner
                uiState.freshnessInfo?.let { freshness ->
                    item(contentType = "banner") {
                        FreshnessBanner(freshnessInfo = freshness)
                    }
                }

                // Approaching Deadlines Section
                if (uiState.approachingDeadlines.isNotEmpty()) {
                    item(contentType = "deadlines") {
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
                item(contentType = "filters") {
                    Column {
                        Text(
                            text = "Latest Official Announcements",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = uiState.selectedCategoryFilter == null,
                                    onClick = { viewModel.selectCategoryFilter(null) },
                                    label = { Text("All Goals") }
                                )
                            }
                            val displayCategories = if (uiState.categories.isNotEmpty()) {
                                uiState.categories.map { it.id to it.name }
                            } else {
                                listOf(
                                    "cat-upsc" to "UPSC",
                                    "cat-ssc" to "SSC",
                                    "cat-banking" to "Banking",
                                    "cat-state-psc" to "State PSC",
                                    "cat-regulatory" to "Regulatory"
                                )
                            }
                            items(displayCategories) { (catId, catName) ->
                                FilterChip(
                                    selected = uiState.selectedCategoryFilter == catId,
                                    onClick = { viewModel.selectCategoryFilter(catId) },
                                    label = { Text(catName) }
                                )
                            }
                        }
                    }
                }

                // Main Notices List
                if (uiState.notices.isEmpty()) {
                    item(contentType = "empty") {
                        EmptyStateView(
                            title = "No Announcements in Category",
                            message = "There are no current notices for this filter. Check back after official updates."
                        )
                    }
                } else {
                    items(
                        items = uiState.notices,
                        key = { it.id },
                        contentType = { "notice_item" }
                    ) { notice ->
                        NoticeCard(
                            notice = notice,
                            onClick = { onNoticeClick(notice.id) }
                        )
                    }
                }

                // Independence Disclaimer Footer
                item(contentType = "footer") {
                    IndependenceDisclaimerBanner()
                }
            }
            }
        }
    }
}
