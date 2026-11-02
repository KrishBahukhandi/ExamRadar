package com.examradar.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examradar.app.data.model.*
import com.examradar.app.data.repository.ExamRadarRepository
import com.examradar.app.ui.components.*
import com.examradar.app.util.ShareHelper
import kotlinx.coroutines.flow.*
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

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: ExamRadarRepository
) : ViewModel() {

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)

    var isRefreshing by mutableStateOf(false)
        private set

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getCategories(),
        repository.getSourceFreshness(),
        repository.getUserPreferences(),
        repository.getNotices(onlyApproachingDeadlines = true),
        _selectedCategoryFilter.flatMapLatest { cat ->
            repository.getNotices(categoryFilter = cat).map { notices -> cat to notices }
        }
    ) { categories, freshness, prefs, deadlines, catAndNotices ->
        val (selectedCat, notices) = catAndNotices
        HomeUiState(
            isLoading = false,
            followedExamsCount = prefs.followedExams.size,
            approachingDeadlines = deadlines,
            notices = notices,
            categories = categories,
            freshnessInfo = freshness,
            selectedCategoryFilter = selectedCat
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    init {
        viewModelScope.launch {
            repository.refreshNotices()
        }
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

    fun selectCategoryFilter(categoryId: String?) {
        _selectedCategoryFilter.value = categoryId
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNoticeClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onCalendarClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onVaultClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var sharingNotice by remember { mutableStateOf<NoticeModel?>(null) }

    if (sharingNotice != null) {
        StudyCircleShareSheet(
            title = "Share Notice to Study Group",
            shareText = ShareHelper.formatNoticeShareCard(sharingNotice!!),
            onDismiss = { sharingNotice = null }
        )
    }

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
                    IconButton(onClick = onCalendarClick) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Examination Calendar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onVaultClick) {
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = "Offline Document Locker",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onNotificationsClick) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notification Alerts",
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

                // Offline Document Locker Quick Access Card
                item(contentType = "vault_banner") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onVaultClick() },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderSpecial,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Application & Document Locker",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Store Admit Cards, Roll Nos & Exam Centers offline",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
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
                                    onClick = { onNoticeClick(urgentNotice.id) },
                                    onShareClick = { sharingNotice = urgentNotice }
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
                            onClick = { onNoticeClick(notice.id) },
                            onShareClick = { sharingNotice = notice }
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
