package com.examradar.app.ui.screens.vault

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.examradar.app.data.local.ApplicationVaultEntity
import com.examradar.app.data.model.ExamModel
import com.examradar.app.ui.components.StudyCircleShareSheet
import com.examradar.app.util.VaultStorageHelper
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    onBackClick: () -> Unit,
    onOpenPdf: (filePath: String, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var entryToDelete by remember { mutableStateOf<ApplicationVaultEntity?>(null) }
    var sharingEntry by remember { mutableStateOf<ApplicationVaultEntity?>(null) }

    if (sharingEntry != null) {
        StudyCircleShareSheet(
            title = "Share Application Credentials",
            shareText = VaultStorageHelper.formatShareText(sharingEntry!!),
            onDismiss = { sharingEntry = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Document Locker",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "100% Offline & Stored on Device",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                    IconButton(onClick = { viewModel.openAddSheet() }) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Add Application",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.entries.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openAddSheet() },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Application") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.entries.isEmpty() && uiState.searchQuery.isBlank()) {
                VaultEmptyState(onAddClick = { viewModel.openAddSheet() })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Search bar if entries exist
                    item {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search by exam, roll no, or application ID...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Security & Offline Assurance Banner
                    item {
                        VaultInfoBanner()
                    }

                    // Applications List
                    items(uiState.entries, key = { it.id }) { entry ->
                        VaultEntryCard(
                            entry = entry,
                            onCopyText = { label, text ->
                                copyToClipboard(context, label, text)
                            },
                            onOpenDirections = { center, city ->
                                openMapsDirections(context, center, city)
                            },
                            onOpenAdmitCard = { path, title ->
                                onOpenPdf(path, title)
                            },
                            onOpenApplicationForm = { path, title ->
                                onOpenPdf(path, title)
                            },
                            onShare = {
                                sharingEntry = entry
                            },
                            onEdit = {
                                viewModel.openEditSheet(entry)
                            },
                            onDelete = {
                                entryToDelete = entry
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    // Add/Edit Bottom Sheet
    if (uiState.isAddEditSheetOpen && uiState.editingEntry != null) {
        AddEditVaultEntrySheet(
            initialEntry = uiState.editingEntry!!,
            availableExams = uiState.availableExams,
            onDismiss = { viewModel.dismissSheet() },
            onSave = { id, examId, examName, appNum, rollNum, center, city, date, repTime, shift, admitName, admitPath, appFormName, appFormPath, notes ->
                viewModel.saveEntry(
                    id = id,
                    examId = examId,
                    examName = examName,
                    applicationNumber = appNum,
                    rollNumber = rollNum,
                    examCenterName = center,
                    examCenterCity = city,
                    examDate = date,
                    reportingTime = repTime,
                    shiftDetails = shift,
                    admitCardFileName = admitName,
                    admitCardFilePath = admitPath,
                    applicationFormFileName = appFormName,
                    applicationFormFilePath = appFormPath,
                    notes = notes
                )
            },
            onAttachFile = { uri, docType ->
                viewModel.attachDocument(context, uri, docType)
            }
        )
    }

    // Delete Confirmation Dialog
    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Delete Application Credentials?") },
            text = {
                Text("This will remove ${entryToDelete?.examName} details and any attached offline admit cards from your device.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = entryToDelete
                        entryToDelete = null
                        if (toDelete != null) {
                            viewModel.deleteEntry(toDelete)
                            Toast.makeText(context, "Deleted application details", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun VaultInfoBanner() {
    Surface(
        color = Color(0xFFF0FDF4),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = Color(0xFF16A34A),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Emergency Offline Hall Access",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF166534)
                )
                Text(
                    text = "Stored 100% on your device. Accessible without internet when official portals crash.",
                    fontSize = 11.sp,
                    color = Color(0xFF15803D),
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
fun VaultEntryCard(
    entry: ApplicationVaultEntity,
    onCopyText: (String, String) -> Unit,
    onOpenDirections: (String, String?) -> Unit,
    onOpenAdmitCard: (String, String) -> Unit,
    onOpenApplicationForm: (String, String) -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Exam Name + Status & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.examName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (entry.admitCardFilePath != null) "Admit Card Attached • Verified Offline" else "Credentials Stored",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (entry.admitCardFilePath != null) Color(0xFF16A34A) else Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp), tint = Color(0xFF2563EB))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp), tint = Color(0xFF64748B))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = Color(0xFFDC2626))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Credentials Boxes: Application Number & Roll Number
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Application Number Box
                CredentialTile(
                    title = "APPLICATION / REG ID",
                    value = entry.applicationNumber,
                    modifier = Modifier.weight(1f),
                    onCopy = { onCopyText("Application ID", entry.applicationNumber) }
                )

                // Roll Number Box (if present)
                if (!entry.rollNumber.isNullOrBlank()) {
                    CredentialTile(
                        title = "ROLL NUMBER",
                        value = entry.rollNumber,
                        modifier = Modifier.weight(1f),
                        accentColor = Color(0xFF1E40AF),
                        onCopy = { onCopyText("Roll Number", entry.rollNumber) }
                    )
                }
            }

            // Schedule: Exam Date & Shift
            if (entry.examDate != null || !entry.reportingTime.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            val dateFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")
                            Text(
                                text = buildString {
                                    if (entry.examDate != null) append(entry.examDate.format(dateFormatter))
                                    if (!entry.shiftDetails.isNullOrBlank()) append(" • ${entry.shiftDetails}")
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            if (!entry.reportingTime.isNullOrBlank()) {
                                Text(
                                    text = "Reporting Time: ${entry.reportingTime}",
                                    fontSize = 11.sp,
                                    color = Color(0xFFDC2626),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Exam Center & Maps Action
            if (!entry.examCenterName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = entry.examCenterName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!entry.examCenterCity.isNullOrBlank()) {
                                    Text(
                                        text = entry.examCenterCity,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        FilledTonalButton(
                            onClick = { onOpenDirections(entry.examCenterName, entry.examCenterCity) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Maps", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Attached Documents Action Buttons
            if (entry.admitCardFilePath != null || entry.applicationFormFilePath != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (entry.admitCardFilePath != null) {
                        Button(
                            onClick = { onOpenAdmitCard(entry.admitCardFilePath, "${entry.examName} Admit Card") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Admit Card", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (entry.applicationFormFilePath != null) {
                        OutlinedButton(
                            onClick = { onOpenApplicationForm(entry.applicationFormFilePath, "${entry.examName} Form") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Application PDF", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Notes / Important Instructions
            if (!entry.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Note: ${entry.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF475569),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun CredentialTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF0F172A),
    onCopy: () -> Unit
) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier.clickable { onCopy() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun VaultEmptyState(onAddClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEFF6FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderSpecial,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Emergency Offline Hall Access",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Never panic on exam morning when official portals (UPSC, SSC, NTA) crash.\nSave your Admit Card PDF, Roll No, and Exam Center address offline right here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Your First Application Credentials", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditVaultEntrySheet(
    initialEntry: ApplicationVaultEntity,
    availableExams: List<ExamModel>,
    onDismiss: () -> Unit,
    onSave: (
        id: String,
        examId: String?,
        examName: String,
        appNum: String,
        rollNum: String?,
        center: String?,
        city: String?,
        date: LocalDate?,
        repTime: String?,
        shift: String?,
        admitName: String?,
        admitPath: String?,
        appFormName: String?,
        appFormPath: String?,
        notes: String?
    ) -> Unit,
    onAttachFile: suspend (Uri, String) -> Pair<String, String>?
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedExamId by rememberSaveable { mutableStateOf(initialEntry.examId) }
    var examNameInput by rememberSaveable { mutableStateOf(initialEntry.examName) }
    var applicationNumberInput by rememberSaveable { mutableStateOf(initialEntry.applicationNumber) }
    var rollNumberInput by rememberSaveable { mutableStateOf(initialEntry.rollNumber ?: "") }
    var examCenterNameInput by rememberSaveable { mutableStateOf(initialEntry.examCenterName ?: "") }
    var examCenterCityInput by rememberSaveable { mutableStateOf(initialEntry.examCenterCity ?: "") }
    var examDateInput by rememberSaveable { mutableStateOf(initialEntry.examDate?.toString() ?: "") }
    var reportingTimeInput by rememberSaveable { mutableStateOf(initialEntry.reportingTime ?: "") }
    var shiftDetailsInput by rememberSaveable { mutableStateOf(initialEntry.shiftDetails ?: "") }
    var notesInput by rememberSaveable { mutableStateOf(initialEntry.notes ?: "") }

    var admitCardFileName by rememberSaveable { mutableStateOf(initialEntry.admitCardFileName) }
    var admitCardFilePath by rememberSaveable { mutableStateOf(initialEntry.admitCardFilePath) }
    var applicationFormFileName by rememberSaveable { mutableStateOf(initialEntry.applicationFormFileName) }
    var applicationFormFilePath by rememberSaveable { mutableStateOf(initialEntry.applicationFormFilePath) }

    var isAttachingAdmitCard by remember { mutableStateOf(false) }
    var isAttachingApplicationForm by remember { mutableStateOf(false) }

    // System File Picker for Admit Card PDF
    val admitCardPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isAttachingAdmitCard = true
                val result = onAttachFile(uri, "admit_card")
                isAttachingAdmitCard = false
                if (result != null) {
                    admitCardFileName = result.first
                    admitCardFilePath = result.second
                }
            }
        }
    }

    // System File Picker for Application Form PDF
    val appFormPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isAttachingApplicationForm = true
                val result = onAttachFile(uri, "application_form")
                isAttachingApplicationForm = false
                if (result != null) {
                    applicationFormFileName = result.first
                    applicationFormFilePath = result.second
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (initialEntry.applicationNumber.isBlank()) "Add Exam Credentials" else "Edit Exam Credentials",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Store your roll number, admit card PDF, and exam hall directions for instant offline access.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Select Target Exam Dropdown / Preset Chips
            if (availableExams.isNotEmpty()) {
                Text("Select Exam:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableExams.take(4).forEach { exam ->
                        val isSelected = selectedExamId == exam.id || examNameInput == exam.examName
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedExamId = exam.id
                                examNameInput = exam.examName
                            },
                            label = { Text(exam.examCode, fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Exam Title (Editable)
            OutlinedTextField(
                value = examNameInput,
                onValueChange = { examNameInput = it },
                label = { Text("Exam Name *") },
                placeholder = { Text("e.g. UPSC CSE 2026, SSC CGL Tier 1") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )

            // Application / Registration Number (Required)
            OutlinedTextField(
                value = applicationNumberInput,
                onValueChange = { applicationNumberInput = it },
                label = { Text("Application / Registration ID *") },
                placeholder = { Text("e.g. 1260481923") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
            )

            // Roll Number (Optional)
            OutlinedTextField(
                value = rollNumberInput,
                onValueChange = { rollNumberInput = it },
                label = { Text("Roll Number (if allotted)") },
                placeholder = { Text("e.g. 0823419") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
            )

            // Exam Center & City
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = examCenterNameInput,
                    onValueChange = { examCenterNameInput = it },
                    label = { Text("Exam Center Name") },
                    placeholder = { Text("e.g. KV Sector 4") },
                    singleLine = true,
                    modifier = Modifier.weight(1.3f)
                )
                OutlinedTextField(
                    value = examCenterCityInput,
                    onValueChange = { examCenterCityInput = it },
                    label = { Text("City") },
                    placeholder = { Text("New Delhi") },
                    singleLine = true,
                    modifier = Modifier.weight(0.9f)
                )
            }

            // Exam Date & Reporting Time
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = examDateInput,
                    onValueChange = { examDateInput = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    placeholder = { Text("2026-05-24") },
                    singleLine = true,
                    modifier = Modifier.weight(1.1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = reportingTimeInput,
                    onValueChange = { reportingTimeInput = it },
                    label = { Text("Reporting Time") },
                    placeholder = { Text("08:30 AM") },
                    singleLine = true,
                    modifier = Modifier.weight(0.9f)
                )
            }

            // Shift / Session details
            OutlinedTextField(
                value = shiftDetailsInput,
                onValueChange = { shiftDetailsInput = it },
                label = { Text("Shift Details / Timing") },
                placeholder = { Text("Paper 1 (09:30-11:30) & Paper 2 (14:30-16:30)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Attach Admit Card PDF
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Admit Card Document (PDF)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            text = admitCardFileName ?: "Attach official downloaded hall ticket PDF",
                            fontSize = 11.sp,
                            color = if (admitCardFileName != null) Color(0xFF16A34A) else Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    FilledTonalButton(
                        onClick = { admitCardPicker.launch("application/pdf") },
                        enabled = !isAttachingAdmitCard
                    ) {
                        if (isAttachingAdmitCard) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        } else {
                            Text(if (admitCardFileName != null) "Replace" else "Attach PDF")
                        }
                    }
                }
            }

            // Attach Application Form PDF (Optional)
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Application Form PDF (Optional)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            text = applicationFormFileName ?: "Attach confirmation slip / form",
                            fontSize = 11.sp,
                            color = if (applicationFormFileName != null) Color(0xFF16A34A) else Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    FilledTonalButton(
                        onClick = { appFormPicker.launch("application/pdf") },
                        enabled = !isAttachingApplicationForm
                    ) {
                        if (isAttachingApplicationForm) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        } else {
                            Text(if (applicationFormFileName != null) "Replace" else "Attach PDF")
                        }
                    }
                }
            }

            // Notes / Hall Instructions
            OutlinedTextField(
                value = notesInput,
                onValueChange = { notesInput = it },
                label = { Text("Hall Notes & Mandatory ID Rules") },
                placeholder = { Text("e.g. Carry Original Aadhaar Card + 2 Passport Photos") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }

                val canSave = examNameInput.isNotBlank() && applicationNumberInput.isNotBlank()
                Button(
                    onClick = {
                        val parsedDate = runCatching { LocalDate.parse(examDateInput.trim()) }.getOrNull()
                        onSave(
                            initialEntry.id,
                            selectedExamId,
                            examNameInput,
                            applicationNumberInput,
                            rollNumberInput,
                            examCenterNameInput,
                            examCenterCityInput,
                            parsedDate,
                            reportingTimeInput,
                            shiftDetailsInput,
                            admitCardFileName,
                            admitCardFilePath,
                            applicationFormFileName,
                            applicationFormFilePath,
                            notesInput
                        )
                    },
                    enabled = canSave,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save to Vault", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun openMapsDirections(context: Context, centerName: String, city: String?) {
    val query = if (!city.isNullOrBlank()) "$centerName, $city" else centerName
    val gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(query))
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
    mapIntent.setPackage("com.google.android.apps.maps")
    try {
        context.startActivity(mapIntent)
    } catch (_: Exception) {
        // Fallback to web browser Google Maps
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query))
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}

private fun shareEntryText(context: Context, entry: ApplicationVaultEntity) {
    val text = VaultStorageHelper.formatShareText(entry)
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Application Credentials")
    context.startActivity(shareIntent)
}

