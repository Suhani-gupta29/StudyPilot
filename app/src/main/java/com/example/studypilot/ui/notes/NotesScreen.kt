package com.example.studypilot.ui.notes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studypilot.data.ImportedContent
import com.example.studypilot.data.Note
import com.example.studypilot.data.NotesRepository
import com.example.studypilot.ui.theme.Roboto
import java.text.SimpleDateFormat
import java.util.*

// Colors (unchanged from original)
private val primaryBlue = Color(0xFF1E88E5)
private val gradientTop = Color(0xFFE3F2FD)
private val gradientBottom = Color(0xFFFFFFFF)
private val cardBackground = Color(0xFFFFFFFF)
private val cardBorder = Color(0xFFE0E7F1)
private val textPrimary = Color(0xFF102A43)
private val textSecondary = Color(0xFF627D98)
private val softDivider = Color(0xFFE6ECF5)
private val pinnedYellow = Color(0xFFFFC107)

// Mime types for the file picker
private val IMPORT_MIME_TYPES = arrayOf(
    "application/pdf",
    "image/jpeg",
    "image/jpg",
    "image/png",
    "image/webp"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    userId: String,
    subjectName: String,
    mode: String,
    sessionId: String? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { NotesRepository() }
    val viewModel: NotesViewModel = viewModel(
        factory = NotesViewModelFactory(
            userId = userId,
            subjectName = subjectName,
            mode = mode,
            sessionId = sessionId,
            repository = repository,
            context = context.applicationContext   // ← supply context for import/export services
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { viewModel.importFileToNote(it) }
    }
    val chatFilePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { viewModel.importFileToChat(it) }
    }
    val noteFilePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { viewModel.importFileToNote(it) }
    }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ── Export result snackbar ──
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.exportResult) {
        uiState.exportResult?.let { result ->
            when (result) {
                is ExportResult.Success -> snackbarHostState.showSnackbar(
                    "✅ PDF saved to Downloads/StudyPilot", duration = SnackbarDuration.Long
                )
                is ExportResult.Failure -> snackbarHostState.showSnackbar(
                    "❌ Export failed: ${result.message}", duration = SnackbarDuration.Long
                )
            }
            viewModel.clearExportResult()
        }
    }

    // ── Import error snackbar ──
    LaunchedEffect(uiState.importError) {
        uiState.importError?.let { error ->
            snackbarHostState.showSnackbar("❌ $error", duration = SnackbarDuration.Short)
            viewModel.clearImportError()
        }
    }

    // ── AI Chat Bottom Sheet ──
    if (uiState.isChatOpen) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeChat() },
            sheetState = bottomSheetState,
            containerColor = Color.Transparent,
            dragHandle = null
        ) {
            AiChatPanel(
                subjectName = subjectName,
                messages = uiState.chatMessages,
                currentInput = uiState.currentChatInput,
                isLoading = uiState.isAiLoading,
                errorMessage = uiState.aiError,
                chatAttachments = uiState.chatAttachments,
                onInputChange = { viewModel.updateChatInput(it) },
                onSendMessage = { viewModel.sendChatMessage() },
                onClose = { viewModel.closeChat() },
                onClearError = { viewModel.clearChatError() },
                onAttachFile = { chatFilePickerLauncher.launch(IMPORT_MIME_TYPES) },
                onRemoveAttachment = { viewModel.removeChatAttachment(it) }
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            NotesTopBar(
                subjectName = subjectName,
                mode = mode,
                onBack = onBack,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                showOnlyPinned = uiState.showOnlyPinned,
                onTogglePinnedFilter = { viewModel.toggleShowOnlyPinned() },
                isExporting = uiState.isExporting,
                onExportPdf = { viewModel.exportNotesPdf() },
                onImportFile = { noteFilePickerLauncher.launch(IMPORT_MIME_TYPES) }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 🤖 AI FAB – always visible
                SmallFloatingActionButton(
                    onClick = { viewModel.openChat() },
                    containerColor = Color.White,
                    contentColor = primaryBlue,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Text(text = "🤖", fontSize = 20.sp)
                }

                // ➕ New Note FAB – only on list screen
                if (!uiState.isEditing && uiState.selectedNote == null) {
                    FloatingActionButton(
                        onClick = { viewModel.createNewNote() },
                        containerColor = primaryBlue,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Note", tint = Color.White)
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(gradientTop, gradientBottom)))
                .padding(paddingValues)
        ) {
            // Show import-loading overlay
            if (uiState.isImporting) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = primaryBlue)
                            Text("Importing file…", fontFamily = Roboto, color = textSecondary)
                        }
                    }
                }
            }

            when {
                uiState.isEditing -> {
                    NoteEditor(
                        note = uiState.selectedNote,
                        saveStatus = uiState.saveStatus,
                        editingTitle = uiState.editingTitle,
                        editingContent = uiState.editingContent,
                        importedContents = uiState.importedContents,
                        onTitleChange = { viewModel.updateNoteTitle(it) },
                        onContentChange = { viewModel.updateNoteContent(it) },
                        onSaveAndClose = { viewModel.saveAndClose() },
                        onCancel = { viewModel.cancelEditing() },
                        onImportFile = { noteFilePickerLauncher.launch(IMPORT_MIME_TYPES) },
                        onRemoveImport = { viewModel.removeImportedContent(it) }
                    )
                }
                uiState.selectedNote != null && !uiState.isEditing -> {
                    NoteViewer(
                        note = uiState.selectedNote!!,
                        onEdit = { viewModel.startEditing() },
                        onDelete = { viewModel.deleteNote(uiState.selectedNote!!) },
                        onTogglePin = { viewModel.togglePin(uiState.selectedNote!!) },
                        onClose = { viewModel.cancelEditing() }
                    )
                }
                else -> {
                    NotesList(
                        notes = viewModel.getFilteredNotes(),
                        isLoading = uiState.isLoading,
                        onNoteClick = { viewModel.selectNote(it) },
                        onTogglePin = { viewModel.togglePin(it) },
                        onDeleteNote = { viewModel.deleteNote(it) }
                    )
                }
            }
        }
    }
}

// ── Top Bar (updated with export + import actions) ────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesTopBar(
    subjectName: String,
    mode: String,
    onBack: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    showOnlyPinned: Boolean,
    onTogglePinnedFilter: () -> Unit,
    isExporting: Boolean,
    onExportPdf: () -> Unit,
    onImportFile: () -> Unit
) {
    var isSearchExpanded by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    Column {
        TopAppBar(
            title = {
                if (isSearchExpanded) {
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search notes...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = subjectName,
                            fontFamily = Roboto,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = textPrimary
                        )
                        Text(
                            text = "Notes",
                            fontFamily = Roboto,
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (isSearchExpanded) {
                        isSearchExpanded = false
                        onSearchQueryChange("")
                    } else {
                        onBack()
                    }
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textPrimary)
                }
            },
            actions = {
                // Search toggle
                IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                    Icon(
                        if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (isSearchExpanded) "Close search" else "Search",
                        tint = textSecondary
                    )
                }

                // Pin filter
                IconButton(onClick = onTogglePinnedFilter) {
                    Icon(
                        if (showOnlyPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Filter pinned",
                        tint = if (showOnlyPinned) pinnedYellow else textSecondary
                    )
                }

                // ⋮ overflow menu for Export + Import
                Box {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = textSecondary)
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        // Export PDF
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isExporting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = primaryBlue
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            tint = primaryBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        if (isExporting) "Exporting…" else "Export notes as PDF",
                                        fontFamily = Roboto,
                                        fontSize = 14.sp
                                    )
                                }
                            },
                            onClick = {
                                showMoreMenu = false
                                onExportPdf()
                            },
                            enabled = !isExporting
                        )
                        Divider()
                        // Import file
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AttachFile,
                                        contentDescription = null,
                                        tint = primaryBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("Import PDF / Image", fontFamily = Roboto, fontSize = 14.sp)
                                }
                            },
                            onClick = {
                                showMoreMenu = false
                                onImportFile()
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = gradientTop)
        )
    }
}

// ── Imported Content Chip Strip ────────────────────────────────────────────────

/**
 * Horizontal strip of chips showing attached imported files.
 * Shown in both the NoteEditor and AiChatPanel.
 */
@Composable
fun ImportedContentChips(
    contents: List<ImportedContent>,
    onRemove: (ImportedContent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (contents.isEmpty()) return

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(contents) { content ->
            val isPdf = content.mimeType == "application/pdf"
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        content.fileName.take(20) + if (content.fileName.length > 20) "…" else "",
                        fontFamily = Roboto,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                },
                leadingIcon = {
                    Text(if (isPdf) "📄" else "🖼️", fontSize = 13.sp)
                },
                trailingIcon = {
                    IconButton(
                        onClick = { onRemove(content) },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            modifier = Modifier.size(14.dp),
                            tint = textSecondary
                        )
                    }
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color(0xFFE3F2FD),
                    labelColor = textPrimary
                ),
                border = AssistChipDefaults.assistChipBorder(
                    enabled = true,
                    borderColor = cardBorder
                )
            )
        }
    }
}

// ── NoteEditor (updated with import strip) ────────────────────────────────────

@Composable
private fun NoteEditor(
    note: Note?,
    saveStatus: SaveStatus,
    editingTitle: String,
    editingContent: String,
    importedContents: List<ImportedContent>,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onSaveAndClose: () -> Unit,
    onCancel: () -> Unit,
    onImportFile: () -> Unit,
    onRemoveImport: (ImportedContent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // ── Editor Top Bar ──
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = textSecondary)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Save status indicator
                    when (saveStatus) {
                        SaveStatus.Saving -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = primaryBlue
                            )
                        }
                        SaveStatus.Saved -> {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Saved",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        SaveStatus.Error -> {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Save error",
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        else -> {}
                    }

                    // Attach / import file button
                    IconButton(onClick = onImportFile) {
                        Icon(
                            Icons.Default.AttachFile,
                            contentDescription = "Attach file",
                            tint = primaryBlue
                        )
                    }

                    // Save & close
                    TextButton(
                        onClick = onSaveAndClose,
                        colors = ButtonDefaults.textButtonColors(contentColor = primaryBlue)
                    ) {
                        Text("Done", fontFamily = Roboto, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Imported file chips
        ImportedContentChips(
            contents = importedContents,
            onRemove = onRemoveImport
        )

        if (importedContents.isNotEmpty()) {
            Divider(color = softDivider)
        }

        // Title field
        TextField(
            value = editingTitle,
            onValueChange = onTitleChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder = {
                Text(
                    "Note title",
                    color = textSecondary,
                    fontFamily = Roboto,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 22.sp,
                fontFamily = Roboto,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            ),
            singleLine = true
        )

        Divider(
            color = softDivider,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Content field
        TextField(
            value = editingContent,
            onValueChange = onContentChange,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = {
                Text(
                    "Start writing your note…",
                    color = textSecondary,
                    fontFamily = Roboto
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 16.sp,
                fontFamily = Roboto,
                color = textPrimary
            )
        )
    }
}

// ── NotesList (unchanged from original) ───────────────────────────────────────

@Composable
private fun NotesList(
    notes: List<Note>,
    isLoading: Boolean,
    onNoteClick: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit
) {
    when {
        isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = primaryBlue)
            }
        }
        notes.isEmpty() -> EmptyNotesState()
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { onNoteClick(note) },
                        onTogglePin = { onTogglePin(note) },
                        onDelete = { onDeleteNote(note) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (note.pinned) 4.dp else 1.dp,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = if (note.pinned)
            androidx.compose.foundation.BorderStroke(1.5.dp, pinnedYellow.copy(alpha = 0.5f))
        else
            androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (note.pinned) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = null,
                                tint = pinnedYellow,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                "Pinned",
                                fontSize = 10.sp,
                                color = pinnedYellow,
                                fontFamily = Roboto,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (note.title.isNotBlank()) {
                        Text(
                            text = note.title,
                            fontFamily = Roboto,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (note.content.isNotBlank()) {
                        Text(
                            text = note.content,
                            fontFamily = Roboto,
                            fontSize = 14.sp,
                            color = textSecondary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 20.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (note.pinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.pinned) "Unpin" else "Pin",
                            tint = if (note.pinned) pinnedYellow else textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFD32F2F).copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatRelativeTime(note.updatedAt),
                fontSize = 11.sp,
                color = textSecondary.copy(alpha = 0.7f),
                fontFamily = Roboto
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Note?", fontFamily = Roboto, fontWeight = FontWeight.Bold) },
            text = { Text("This action cannot be undone.", fontFamily = Roboto) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = Color(0xFFD32F2F), fontFamily = Roboto)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", fontFamily = Roboto)
                }
            }
        )
    }
}

// ── NoteViewer (unchanged from original) ─────────────────────────────────────

@Composable
private fun NoteViewer(
    note: Note,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    onClose: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onTogglePin) {
                        Icon(
                            if (note.pinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.pinned) "Unpin" else "Pin",
                            tint = if (note.pinned) pinnedYellow else textSecondary
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFD32F2F)
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = primaryBlue
                        )
                    }
                }
            }
        }

        Divider(color = softDivider)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = formatDateTime(note.createdAt),
                fontSize = 12.sp,
                color = textSecondary,
                fontFamily = Roboto
            )

            if (note.updatedAt != note.createdAt) {
                Text(
                    text = "Last edited: ${formatDateTime(note.updatedAt)}",
                    fontSize = 11.sp,
                    color = textSecondary,
                    fontFamily = Roboto
                )
            }

            Divider(color = softDivider)

            if (note.title.isNotBlank()) {
                Text(
                    text = note.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    fontFamily = Roboto
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = note.content.ifBlank { "No content" },
                fontSize = 16.sp,
                color = if (note.content.isBlank()) textSecondary else textPrimary,
                fontFamily = Roboto
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Note?", fontFamily = Roboto, fontWeight = FontWeight.Bold) },
            text = { Text("This action cannot be undone.", fontFamily = Roboto) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = Color(0xFFD32F2F), fontFamily = Roboto)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", fontFamily = Roboto)
                }
            }
        )
    }
}

// ── Empty state (unchanged) ───────────────────────────────────────────────────

@Composable
private fun EmptyNotesState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Outlined.Note,
                contentDescription = "No notes",
                tint = textSecondary,
                modifier = Modifier.size(64.dp)
            )
            Text(
                "No notes yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = textPrimary,
                fontFamily = Roboto
            )
            Text(
                "Tap + to create your first note",
                fontSize = 14.sp,
                color = textSecondary,
                fontFamily = Roboto
            )
        }
    }
}

// ── Helpers (unchanged) ───────────────────────────────────────────────────────

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / (1000 * 60)
    val hours = diff / (1000 * 60 * 60)
    val days = diff / (1000 * 60 * 60 * 24)

    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hr ago"
        days < 7 -> "$days days ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}