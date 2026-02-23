package com.example.studypilot.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studypilot.data.Note
import com.example.studypilot.data.NotesRepository
import com.example.studypilot.ui.theme.Roboto
import java.text.SimpleDateFormat
import java.util.*

// Colors
private val primaryBlue = Color(0xFF1E88E5)
private val gradientTop = Color(0xFFE3F2FD)
private val gradientBottom = Color(0xFFFFFFFF)
private val cardBackground = Color(0xFFFFFFFF)
private val cardBorder = Color(0xFFE0E7F1)
private val textPrimary = Color(0xFF102A43)
private val textSecondary = Color(0xFF627D98)
private val softDivider = Color(0xFFE6ECF5)
private val pinnedYellow = Color(0xFFFFC107)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    userId: String,
    subjectName: String,
    mode: String,
    sessionId: String? = null,
    onBack: () -> Unit
) {
    val repository = remember { NotesRepository() }
    val viewModel: NotesViewModel = viewModel(
        factory = NotesViewModelFactory(
            userId = userId,
            subjectName = subjectName,
            mode = mode,
            sessionId = sessionId,
            repository = repository
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // AI Chat Bottom Sheet
    if (uiState.isChatOpen) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeChat() },
            sheetState = sheetState,
            containerColor = Color.Transparent,
            dragHandle = null
        ) {
            AiChatPanel(
                subjectName = subjectName,
                messages = uiState.chatMessages,
                currentInput = uiState.currentChatInput,
                isLoading = uiState.isAiLoading,
                errorMessage = uiState.aiError,
                onInputChange = { viewModel.updateChatInput(it) },
                onSendMessage = { viewModel.sendChatMessage() },
                onClose = { viewModel.closeChat() },
                onClearError = { viewModel.clearChatError() }
            )
        }
    }

    Scaffold(
        topBar = {
            NotesTopBar(
                subjectName = subjectName,
                mode = mode,
                onBack = onBack,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                showOnlyPinned = uiState.showOnlyPinned,
                onTogglePinnedFilter = { viewModel.toggleShowOnlyPinned() }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 🤖 AI FAB - always visible (list + editor + viewer)
                SmallFloatingActionButton(
                    onClick = { viewModel.openChat() },
                    containerColor = Color.White,
                    contentColor = primaryBlue,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    Text(
                        text = "🤖",
                        fontSize = 20.sp
                    )
                }

                // ➕ New Note FAB - only on list screen
                if (!uiState.isEditing && uiState.selectedNote == null) {
                    FloatingActionButton(
                        onClick = { viewModel.createNewNote() },
                        containerColor = primaryBlue,
                        shape = CircleShape
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "New Note",
                            tint = Color.White
                        )
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
            when {
                uiState.isEditing -> {
                    NoteEditor(
                        note = uiState.selectedNote,
                        saveStatus = uiState.saveStatus,
                        editingTitle = uiState.editingTitle,
                        editingContent = uiState.editingContent,
                        onTitleChange = { viewModel.updateNoteTitle(it) },
                        onContentChange = { viewModel.updateNoteContent(it) },
                        onSaveAndClose = { viewModel.saveAndClose() },
                        onCancel = { viewModel.cancelEditing() }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesTopBar(
    subjectName: String,
    mode: String,
    onBack: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    showOnlyPinned: Boolean,
    onTogglePinnedFilter: () -> Unit
) {
    var isSearchExpanded by remember { mutableStateOf(false) }

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
                            fontSize = 19.sp,      // was 18.sp
                            color = textPrimary
                        )
                        Text(
                            text = "Notes",
                            fontFamily = Roboto,
                            fontSize = 12.sp,
                            color = textSecondary,
                            letterSpacing = 0.4.sp  // adds subtle refinement
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = textPrimary
                    )
                }
            },
            actions = {
                if (isSearchExpanded) {
                    IconButton(onClick = {
                        isSearchExpanded = false
                        onSearchQueryChange("")
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close search")
                    }
                } else {
                    IconButton(onClick = { isSearchExpanded = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }

                IconButton(onClick = onTogglePinnedFilter) {
                    Icon(
                        if (showOnlyPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Filter pinned",
                        tint = if (showOnlyPinned) pinnedYellow else textSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .border(
                            width = 1.dp,
                            color = primaryBlue,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${mode.uppercase()} MODE",
                        color = primaryBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = Roboto
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White,
                scrolledContainerColor = Color.White,
                navigationIconContentColor = textPrimary,
                titleContentColor = textPrimary,
                actionIconContentColor = primaryBlue   // makes search/pin icons pick up theme color
            )
        )
        Divider(color = softDivider)
    }
}

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
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = primaryBlue)
            }
        }
        notes.isEmpty() -> {
            EmptyNotesState()
        }
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
                elevation = if (note.pinned) 4.dp else 2.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (note.pinned) Color(0xFFFFFDE7) else cardBackground
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (note.pinned) pinnedYellow.copy(alpha = 0.3f) else cardBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = formatDateTime(note.createdAt),
                    fontSize = 11.sp,
                    color = textSecondary,
                    fontFamily = Roboto,
                    modifier = Modifier.weight(1f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (note.pinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.pinned) "Unpin" else "Pin",
                            tint = if (note.pinned) pinnedYellow else textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (note.title.isNotBlank()) {
                Text(
                    text = note.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    fontFamily = Roboto,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = note.content.ifBlank {
                    if (note.title.isBlank()) "Empty note" else "No content"
                },
                fontSize = 13.sp,
                color = if (note.content.isBlank()) textSecondary else textPrimary,
                fontFamily = Roboto,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (note.updatedAt != note.createdAt) {
                Text(
                    text = "Edited ${formatRelativeTime(note.updatedAt)}",
                    fontSize = 10.sp,
                    color = textSecondary,
                    fontFamily = Roboto
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text("Delete Note?", fontFamily = Roboto, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("This action cannot be undone.", fontFamily = Roboto)
            },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditor(
    note: Note?,
    saveStatus: SaveStatus,
    editingTitle: String,
    editingContent: String,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onSaveAndClose: () -> Unit,
    onCancel: () -> Unit
) {
    if (note == null) return

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
                TextButton(onClick = onCancel) {
                    Text("Cancel", fontFamily = Roboto, color = textSecondary)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (saveStatus) {
                        SaveStatus.Saving -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = primaryBlue
                            )
                            Text(
                                "Saving...",
                                fontSize = 12.sp,
                                color = textSecondary,
                                fontFamily = Roboto
                            )
                        }
                        SaveStatus.Saved -> {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Saved",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Saved",
                                fontSize = 12.sp,
                                color = Color(0xFF4CAF50),
                                fontFamily = Roboto
                            )
                        }
                        SaveStatus.Error -> {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Error",
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Error",
                                fontSize = 12.sp,
                                color = Color(0xFFD32F2F),
                                fontFamily = Roboto
                            )
                        }
                        else -> {}
                    }
                }

                TextButton(onClick = onSaveAndClose) {
                    Text(
                        "Done",
                        fontFamily = Roboto,
                        color = primaryBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Divider(color = softDivider)

        TextField(
            value = editingTitle,
            onValueChange = onTitleChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = {
                Text(
                    "Note title...",
                    color = textSecondary,
                    fontFamily = Roboto,
                    fontSize = 20.sp,
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
                fontSize = 20.sp,
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

        TextField(
            value = editingContent,
            onValueChange = onContentChange,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = {
                Text(
                    "Start writing your note...",
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
            title = {
                Text("Delete Note?", fontFamily = Roboto, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("This action cannot be undone.", fontFamily = Roboto)
            },
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

@Composable
private fun EmptyNotesState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
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