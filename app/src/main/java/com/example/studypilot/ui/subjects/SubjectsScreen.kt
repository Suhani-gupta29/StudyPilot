package com.example.studypilot.ui.subjects

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studypilot.ui.analytics.AnalyticsColors
import com.example.studypilot.ui.home.HomeBottomNavigationBar
import com.example.studypilot.ui.mode.StudyMode
import com.example.studypilot.ui.shared.Difficulty
import com.example.studypilot.ui.shared.Priority
import com.example.studypilot.ui.theme.Roboto
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// Colors
private val primaryBlue = Color(0xFF1E88E5)
private val mainBackground = Color(0xFFFFFFFF)
private val gradientTop = Color(0xFFE3F2FD)
private val gradientBottom = Color(0xFFFFFFFF)
private val cardBackground = Color(0xFFFFFFFF)
private val cardBorder = Color(0xFFE0E7F1)
private val textPrimary = Color(0xFF102A43)
private val textSecondary = Color(0xFF627D98)
private val softDivider = Color(0xFFE6ECF5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    viewModel: SubjectsViewModel,
    modeName: String,
    onNavigateToAddSubject: (StudyMode) -> Unit,
    onNavigateToViewNotes: (String, StudyMode) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "StudyPilot",
                            fontFamily = Roboto,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = AnalyticsColors.primaryBlue
                        )
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .border(
                                    width = 1.dp,
                                    color = AnalyticsColors.primaryBlue,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = modeName.uppercase(),
                                color = AnalyticsColors.primaryBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = Roboto
                            )
                        }
                    },
                    modifier = Modifier.background(Color.White),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        scrolledContainerColor = Color.White,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        titleContentColor = AnalyticsColors.primaryBlue,
                        actionIconContentColor = AnalyticsColors.primaryBlue
                    )
                )
                Divider(color = AnalyticsColors.softDivider)
            }
        },
        bottomBar = {
            HomeBottomNavigationBar(
                onNavigateToHome = onNavigateToHome,
                onNavigateToPlanner = onNavigateToPlanner,
                onNavigateToAnalytics = onNavigateToAnalytics,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToSubjects = { },
                activeIndex = 2
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Show mode selector dialog */ },
                containerColor = primaryBlue,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Subject", tint = Color.White)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(gradientTop, gradientBottom)))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 80.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Search Bar
                item {
                    SearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = { viewModel.updateSearchQuery(it) }
                    )
                }

                // Filters
                item {
                    FilterChips(
                        selectedFilter = uiState.selectedFilter,
                        onFilterSelected = { viewModel.updateFilter(it) }
                    )
                }

                // Exam Mode Section
                if (uiState.examSubjects.isNotEmpty()) {
                    item {
                        ModeHeader(
                            mode = StudyMode.EXAM,
                            count = uiState.examSubjects.size,
                            onAddClick = { onNavigateToAddSubject(StudyMode.EXAM) }
                        )
                    }

                    items(viewModel.getFilteredSubjects(uiState.examSubjects)) { subject ->
                        SubjectCard(
                            subject = subject,
                            onViewNotes = { onNavigateToViewNotes(subject.name, StudyMode.EXAM) },
                            onDelete = { viewModel.deleteSubject(subject.name, StudyMode.EXAM) }
                        )
                    }
                }

                // Focus Mode Section
                if (uiState.focusSubjects.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        ModeHeader(
                            mode = StudyMode.FOCUS,
                            count = uiState.focusSubjects.size,
                            onAddClick = { onNavigateToAddSubject(StudyMode.FOCUS) }
                        )
                    }

                    items(viewModel.getFilteredSubjects(uiState.focusSubjects)) { subject ->
                        SubjectCard(
                            subject = subject,
                            onViewNotes = { onNavigateToViewNotes(subject.name, StudyMode.FOCUS) },
                            onDelete = { viewModel.deleteSubject(subject.name, StudyMode.FOCUS) }
                        )
                    }
                }

                // Casual Mode Section
                if (uiState.casualSubjects.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        ModeHeader(
                            mode = StudyMode.CASUAL,
                            count = uiState.casualSubjects.size,
                            onAddClick = { onNavigateToAddSubject(StudyMode.CASUAL) }
                        )
                    }

                    items(viewModel.getFilteredSubjects(uiState.casualSubjects)) { subject ->
                        SubjectCard(
                            subject = subject,
                            onViewNotes = { onNavigateToViewNotes(subject.name, StudyMode.CASUAL) },
                            onDelete = { viewModel.deleteSubject(subject.name, StudyMode.CASUAL) }
                        )
                    }
                }

                // Empty State
                if (uiState.examSubjects.isEmpty() && uiState.focusSubjects.isEmpty() && uiState.casualSubjects.isEmpty() && !uiState.isLoading) {
                    item {
                        EmptyState()
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        placeholder = {
            Text(
                "🔍 Search subjects...",
                color = textSecondary,
                fontFamily = Roboto
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = textSecondary
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = primaryBlue,
            unfocusedBorderColor = cardBorder,
            focusedContainerColor = cardBackground,
            unfocusedContainerColor = cardBackground
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

@Composable
private fun FilterChips(
    selectedFilter: SubjectFilter,
    onFilterSelected: (SubjectFilter) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        item {
            FilterChip(
                selected = selectedFilter == SubjectFilter.ALL,
                onClick = { onFilterSelected(SubjectFilter.ALL) },
                label = { Text("All", fontFamily = Roboto) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = primaryBlue,
                    selectedLabelColor = Color.White
                )
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == SubjectFilter.HIGH_PRIORITY,
                onClick = { onFilterSelected(SubjectFilter.HIGH_PRIORITY) },
                label = { Text("High Priority", fontFamily = Roboto) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = primaryBlue,
                    selectedLabelColor = Color.White
                )
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == SubjectFilter.HARD,
                onClick = { onFilterSelected(SubjectFilter.HARD) },
                label = { Text("Hard", fontFamily = Roboto) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = primaryBlue,
                    selectedLabelColor = Color.White
                )
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == SubjectFilter.MEDIUM,
                onClick = { onFilterSelected(SubjectFilter.MEDIUM) },
                label = { Text("Medium", fontFamily = Roboto) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = primaryBlue,
                    selectedLabelColor = Color.White
                )
            )
        }
        item {
            FilterChip(
                selected = selectedFilter == SubjectFilter.EASY,
                onClick = { onFilterSelected(SubjectFilter.EASY) },
                label = { Text("Easy", fontFamily = Roboto) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = primaryBlue,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun ModeHeader(
    mode: StudyMode,
    count: Int,
    onAddClick: () -> Unit
) {
    val modeIcon = when (mode) {
        StudyMode.EXAM -> "📚"
        StudyMode.FOCUS -> "🎯"
        StudyMode.CASUAL -> "☕"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$modeIcon ${mode.name} MODE ($count subjects)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            fontFamily = Roboto
        )

        IconButton(onClick = onAddClick) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add subject",
                tint = primaryBlue
            )
        }
    }
}

@Composable
private fun SubjectCard(
    subject: SubjectWithStats,
    onViewNotes: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val difficultyColor = when (subject.difficulty) {
        Difficulty.Hard -> Color(0xFFD32F2F)
        Difficulty.Medium -> Color(0xFFFFA000)
        Difficulty.Easy -> Color(0xFF388E3C)
    }

    val difficultyEmoji = when (subject.difficulty) {
        Difficulty.Hard -> "🔴"
        Difficulty.Medium -> "🟡"
        Difficulty.Easy -> "🟢"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.06f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subject.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    fontFamily = Roboto,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = difficultyEmoji,
                        fontSize = 16.sp
                    )
                    Text(
                        text = subject.difficulty.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = difficultyColor,
                        fontFamily = Roboto
                    )
                }
            }

            // Priority
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Priority:",
                    fontSize = 13.sp,
                    color = textSecondary,
                    fontFamily = Roboto
                )
                Text(
                    text = subject.priority.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = textPrimary,
                    fontFamily = Roboto
                )
            }

            // Notes Info
            if (subject.notesCount > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = "Notes",
                        tint = textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${subject.notesCount} notes",
                        fontSize = 12.sp,
                        color = textSecondary,
                        fontFamily = Roboto
                    )
                    subject.lastNoteDate?.let { date ->
                        Text(" • ", fontSize = 12.sp, color = textSecondary)
                        Text(
                            text = "Last: ${formatRelativeTime(date)}",
                            fontSize = 12.sp,
                            color = textSecondary,
                            fontFamily = Roboto
                        )
                    }
                }
            }

            Divider(color = softDivider)

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewNotes,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = primaryBlue
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, primaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = "Notes",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "View Notes",
                        fontFamily = Roboto,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFD32F2F)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Delete",
                        fontFamily = Roboto,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    "Delete Subject?",
                    fontFamily = Roboto,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete '${subject.name}'? This action cannot be undone.",
                    fontFamily = Roboto
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
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
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Outlined.School,
                contentDescription = "No subjects",
                tint = textSecondary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                "No subjects yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = textPrimary,
                fontFamily = Roboto
            )
            Text(
                "Add a subject to get started",
                fontSize = 14.sp,
                color = textSecondary,
                fontFamily = Roboto
            )
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        days == 0L -> "today"
        days == 1L -> "1 day ago"
        days < 7 -> "$days days ago"
        days < 30 -> "${days / 7} weeks ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}
