package com.example.studypilot.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.studypilot.ui.theme.Roboto

// ── Colors — matches SubjectsScreen exactly ───────────────────────────────────
private val primaryBlue    = Color(0xFF1E88E5)
private val lightBlue      = Color(0xFFE3F2FD)
private val gradientTop    = Color(0xFFD6ECFB)
private val gradientBottom = Color(0xFFFFFFFF)
private val textPrimary    = Color(0xFF0A2540)
private val textSecondary  = Color(0xFF1E3A5F)
private val softDivider    = Color(0xFFE2EAF4)
private val cardWhite      = Color(0xFFFFFFFF)
private val cardBorder     = Color(0xFFCBD5E1)
private val inputFieldBg   = Color(0xFFF5FAFF)
private val chipUnselectedBg   = Color(0xFFEAF3FB)
private val chipUnselectedText = Color(0xFF0A2540)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDirectoryScreen(
    viewModel: ChatViewModel,
    onNavigateToChat: (Room) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.directoryState.collectAsState()
    val filtered = viewModel.filteredRooms()

    LaunchedEffect(Unit) { viewModel.startListeningToRooms() }
    DisposableEffect(Unit) { onDispose { viewModel.stopListeningToRooms() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(gradientTop, gradientBottom)))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = textPrimary
                                )
                            }
                        },
                        title = {
                            Text(
                                "Study Rooms",
                                fontFamily = Roboto,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = primaryBlue
                            )
                        },
                        actions = {
                            Box(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .border(1.dp, primaryBlue, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "PUBLIC",
                                    fontFamily = Roboto,
                                    color = primaryBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                    )
                    Divider(color = softDivider, thickness = 1.dp)
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.showCreateDialog() },
                    containerColor = primaryBlue,
                    shape = CircleShape,
                    modifier = Modifier.shadow(
                        12.dp, CircleShape,
                        spotColor = primaryBlue.copy(alpha = 0.4f)
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Room", tint = Color.White)
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {

                // ── Search Bar ────────────────────────────────────────────
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    placeholder = {
                        Text(
                            "Search rooms...",
                            fontFamily = Roboto,
                            color = textSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = textSecondary)
                    },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryBlue,
                        unfocusedBorderColor = cardBorder,
                        focusedContainerColor = inputFieldBg,
                        unfocusedContainerColor = inputFieldBg,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    )
                )

                // ── Subject Filter Chips ──────────────────────────────────
                val allFilters = listOf("All") + SUBJECT_TAGS
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    items(allFilters) { filter ->
                        val isSelected = state.selectedSubjectFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onSubjectFilterChange(filter) },
                            label = {
                                Text(
                                    filter,
                                    fontFamily = Roboto,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primaryBlue,
                                selectedLabelColor = Color.White,
                                containerColor = chipUnselectedBg,
                                labelColor = chipUnselectedText
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                selectedBorderColor = primaryBlue,
                                borderColor = cardBorder
                            )
                        )
                    }
                }

                // ── Room List ─────────────────────────────────────────────
                when {
                    state.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = primaryBlue)
                        }
                    }

                    filtered.isEmpty() -> {
                        EmptyRoomsState()
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp,
                                top = 4.dp, bottom = 100.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filtered, key = { it.id }) { room ->
                                RoomCard(room = room, onClick = { onNavigateToChat(room) })
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Create Room Dialog ────────────────────────────────────────────────
    if (state.showCreateDialog) {
        CreateRoomDialog(
            state = state,
            onNameChange = viewModel::onCreateNameChange,
            onDescriptionChange = viewModel::onCreateDescriptionChange,
            onSubjectChange = viewModel::onCreateSubjectChange,
            onCreate = { viewModel.createRoom {} },
            onDismiss = viewModel::hideCreateDialog
        )
    }
}

// ── Empty State ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyRoomsState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("💬", fontSize = 52.sp)
            Text(
                "No rooms found",
                fontFamily = Roboto,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = textPrimary
            )
            Text(
                "Be the first to create a study room!",
                fontFamily = Roboto,
                fontSize = 13.sp,
                color = textSecondary
            )
        }
    }
}

// ── Room Card ─────────────────────────────────────────────────────────────────

@Composable
private fun RoomCard(room: Room, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.06f)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Subject emoji avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(subjectColor(room.subject).copy(alpha = 0.12f))
                    .border(1.dp, subjectColor(room.subject).copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = subjectEmoji(room.subject), fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = room.name,
                    fontFamily = Roboto,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (room.description.isNotBlank()) {
                    Text(
                        text = room.description,
                        fontFamily = Roboto,
                        fontSize = 12.sp,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (room.lastMessage.isNotBlank()) {
                    Text(
                        text = room.lastMessage,
                        fontFamily = Roboto,
                        fontSize = 12.sp,
                        color = textSecondary.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Subject pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(subjectColor(room.subject).copy(alpha = 0.1f))
                        .border(
                            1.dp,
                            subjectColor(room.subject).copy(alpha = 0.3f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = room.subject,
                        fontFamily = Roboto,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = subjectColor(room.subject)
                    )
                }

                // Member count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${room.memberCount}",
                        fontFamily = Roboto,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                }
            }
        }
    }
}

// ── Create Room Dialog ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateRoomDialog(
    state: RoomDirectoryUiState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardWhite),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, cardBorder, RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                // Header
                Text(
                    "Create a Room",
                    fontFamily = Roboto,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = textPrimary
                )
                Text(
                    "Anyone on StudyPilot can discover and join",
                    fontFamily = Roboto,
                    fontSize = 13.sp,
                    color = textSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 20.dp)
                )

                // Room Name field
                OutlinedTextField(
                    value = state.createName,
                    onValueChange = onNameChange,
                    label = { Text("Room Name *", fontFamily = Roboto) },
                    placeholder = { Text("e.g. JEE Mathematics 2026", fontFamily = Roboto) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = state.createError.isNotBlank(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryBlue,
                        unfocusedBorderColor = cardBorder,
                        focusedLabelColor = primaryBlue,
                        focusedContainerColor = inputFieldBg,
                        unfocusedContainerColor = inputFieldBg,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    )
                )
                if (state.createError.isNotBlank()) {
                    Text(
                        state.createError,
                        fontFamily = Roboto,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Description field
                OutlinedTextField(
                    value = state.createDescription,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description (optional)", fontFamily = Roboto) },
                    placeholder = { Text("What is this room about?", fontFamily = Roboto) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryBlue,
                        unfocusedBorderColor = cardBorder,
                        focusedLabelColor = primaryBlue,
                        focusedContainerColor = inputFieldBg,
                        unfocusedContainerColor = inputFieldBg,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Subject label
                Text(
                    "Subject",
                    fontFamily = Roboto,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Subject chips — scrollable row
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SUBJECT_TAGS.forEach { subject ->
                        val isSelected = state.createSubject == subject
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSubjectChange(subject) },
                            label = {
                                Text(
                                    subject,
                                    fontFamily = Roboto,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primaryBlue,
                                selectedLabelColor = Color.White,
                                containerColor = chipUnselectedBg,
                                labelColor = chipUnselectedText
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                selectedBorderColor = primaryBlue,
                                borderColor = cardBorder
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                    ) {
                        Text("Cancel", fontFamily = Roboto)
                    }

                    Button(
                        onClick = onCreate,
                        modifier = Modifier
                            .weight(1f)
                            .shadow(
                                4.dp,
                                RoundedCornerShape(12.dp),
                                spotColor = primaryBlue.copy(alpha = 0.4f)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !state.isCreating,
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                    ) {
                        if (state.isCreating) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Create",
                                fontFamily = Roboto,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun subjectEmoji(subject: String): String = when (subject) {
    "Mathematics"      -> "📐"
    "Physics"          -> "⚛️"
    "Chemistry"        -> "🧪"
    "Biology"          -> "🧬"
    "History"          -> "📜"
    "Geography"        -> "🌍"
    "English"          -> "📖"
    "Computer Science" -> "💻"
    "Economics"        -> "📊"
    else               -> "💬"
}

private fun subjectColor(subject: String): Color = when (subject) {
    "Mathematics"      -> Color(0xFF1E88E5)
    "Physics"          -> Color(0xFF8E24AA)
    "Chemistry"        -> Color(0xFF00897B)
    "Biology"          -> Color(0xFF43A047)
    "History"          -> Color(0xFFE53935)
    "Geography"        -> Color(0xFF039BE5)
    "English"          -> Color(0xFFF4511E)
    "Computer Science" -> Color(0xFF3949AB)
    "Economics"        -> Color(0xFFFFB300)
    else               -> Color(0xFF757575)
}