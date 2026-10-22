package com.examradar.app.ui.screens.discovery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.examradar.app.data.model.CategoryModel
import com.examradar.app.data.model.ExamModel
import com.examradar.app.data.repository.ExamRadarRepository
import com.examradar.app.ui.components.EmptyStateView
import com.examradar.app.ui.components.LoadingStateView
import com.examradar.app.util.CustomTabsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class DiscoveryUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val categories: List<CategoryModel> = emptyList(),
    val exams: List<ExamModel> = emptyList()
)

class DiscoveryViewModel(
    private val repository: ExamRadarRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoveryUiState())
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

    private fun loadData() {
        viewModelScope.launch {
            launch {
                repository.getCategories().collectLatest { categories ->
                    _uiState.value = _uiState.value.copy(categories = categories)
                }
            }
            refreshExams()
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        refreshExams()
    }

    fun selectCategory(categoryId: String?) {
        _uiState.value = _uiState.value.copy(selectedCategory = categoryId)
        refreshExams()
    }

    fun toggleFollowExam(examId: String) {
        viewModelScope.launch {
            repository.toggleFollowExam(examId)
        }
    }

    private fun refreshExams() {
        viewModelScope.launch {
            repository.getExams(
                categoryId = _uiState.value.selectedCategory,
                searchQuery = _uiState.value.searchQuery
            ).collectLatest { exams ->
                _uiState.value = _uiState.value.copy(exams = exams, isLoading = false)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    viewModel: DiscoveryViewModel,
    onExamClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Discover Examinations",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("Search by exam, organization, or code (e.g. CSE, CGL)") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            )

            // Category Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedCategory == null,
                        onClick = { viewModel.selectCategory(null) },
                        label = { Text("All Categories") }
                    )
                }
                items(uiState.categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = uiState.selectedCategory == category.id,
                        onClick = { viewModel.selectCategory(category.id) },
                        label = { Text(category.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            PullToRefreshBox(
                isRefreshing = viewModel.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (uiState.isLoading) {
                    LoadingStateView()
                } else if (uiState.exams.isEmpty()) {
                    EmptyStateView(
                        title = "No Examinations Found",
                        message = "No examinations match your search query. Try broadening your terms."
                    )
                } else {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.exams,
                            key = { it.id },
                            contentType = { "exam_card" }
                        ) { exam ->
                            ExamDiscoveryCard(
                                exam = exam,
                                onClick = { onExamClick(exam.id) },
                                onToggleFollow = { viewModel.toggleFollowExam(exam.id) },
                                onOpenPortal = {
                                    CustomTabsHelper.openOfficialUrl(context, exam.officialPortalUrl)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExamDiscoveryCard(
    exam: ExamModel,
    onClick: () -> Unit,
    onToggleFollow: () -> Unit,
    onOpenPortal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exam.organization,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = exam.examName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = onToggleFollow) {
                    Icon(
                        imageVector = if (exam.isFollowing) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (exam.isFollowing) "Unfollow" else "Follow",
                        tint = if (exam.isFollowing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = exam.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "${exam.activeNoticeCount} Active Notices",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                OutlinedButton(
                    onClick = onOpenPortal,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Official Portal", fontSize = 11.sp)
                }
            }
        }
    }
}
