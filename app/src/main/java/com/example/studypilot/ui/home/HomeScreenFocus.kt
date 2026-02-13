package com.example.studypilot.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studypilot.ui.shared.SessionStatus
import com.example.studypilot.ui.shared.FocusTask
import com.example.studypilot.ui.theme.Roboto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.ui.text.style.TextAlign
import com.example.studypilot.ui.casual.CasualTask

// --- Strict Color System ---
private val primaryBlue = Color(0xFF1E88E5)
private val mainBackground = Color(0xFFFFFFFF)
private val sectionBackground = Color(0xFFF5F9FF)
private val cardBackground = Color(0xFFFFFFFF)
private val softCardHighlight = Color(0xFFF1F7FF)
private val cardBorder = Color(0xFFE0E7F1)
private val softDividerLine = Color(0xFFE6ECF5)
private val textPrimary = Color(0xFF102A43)
private val textSecondary = Color(0xFF627D98)

// Status & Shadow Colors
private val cardShadowColor = Color.Black.copy(alpha = 0.06f)
private val completedGreen = Color(0xFF2E7D32)
private val currentBlue = Color(0xFF1E88E5)
private val upcomingGrey = Color(0xFF90A4AE)

private val gradientTop = Color(0xFFE3F2FD)
private val gradientBottom = Color(0xFFFFFFFF)



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenFocus(
    focusDetails: FocusDetails?,
    sessions: List<FocusSession>?,
    metrics: FocusMetrics?,
    alerts: List<FocusAlert>,
    isSwapMode: Boolean,
    swapSourceIndex: Int?,
    onNavigateToSettings: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToSubjects: () -> Unit,
    onEnterSwapMode: () -> Unit,
    onCancelSwap: () -> Unit,
    onSaveSwap: () -> Unit,
    onSessionClickedInSwapMode: (Int) -> Unit,
    onStartSession: (String, String, Int) -> Unit,
    onNavigateToFocusSetup: () -> Unit,  // ADD THIS
    onTaskCompletionToggled: (String, Boolean) -> Unit
) {
    val activeSession = sessions
        ?.firstOrNull { it.status != SessionStatus.COMPLETED }

    Scaffold(
        topBar = { FocusTopAppBar() },
        bottomBar = { HomeBottomNavigationBar(onNavigateToSettings = onNavigateToSettings, onNavigateToAnalytics = onNavigateToAnalytics, onNavigateToPlanner = onNavigateToPlanner, onNavigateToSubjects = onNavigateToSubjects, activeIndex = 0) },

    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(gradientTop, gradientBottom)
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { ContextHeader() }

                // ONLY CHANGE: Check if there are NO subjects to determine empty state
                val hasSubjects = focusDetails?.subjects?.isNotEmpty() == true

                if (hasSubjects && sessions != null && sessions.isNotEmpty()) {
                    // EXISTING CODE - NO CHANGES
                    item {
                        TodayFocusHeader(
                            isSwapMode = isSwapMode,
                            onCancelSwap = onCancelSwap,
                            onEnterSwapMode = onEnterSwapMode,
                            onSaveSwap = onSaveSwap
                        )
                    }
                    val firstUncompletedIndex =
                        sessions.indexOfFirst { it.status != SessionStatus.COMPLETED }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBackground),
                            border = BorderStroke(1.dp, cardBorder),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                sessions.forEachIndexed { index, session ->
                                    FocusSessionCard(
                                        session = session,
                                        isSwapMode = isSwapMode,
                                        isSourceNode = swapSourceIndex == index,
                                        isCurrent = index == firstUncompletedIndex,
                                        onClick = {
                                            if (isSwapMode) {
                                                onSessionClickedInSwapMode(index)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // EMPTY STATE - Show when no subjects exist
                    item {
                        EmptySessionsCard(onClick = onNavigateToFocusSetup)
                    }
                }

                item {
                    BeginFocusSessionButton(
                        onClick = {
                            activeSession?.let {
                                onStartSession(
                                    it.subjectName,
                                    "FOCUS",
                                    it.duration
                                )
                            }
                        }
                    )
                }

                if (focusDetails != null) {
                    item {
                        TasksPanel(
                            tasks = focusDetails.tasks,
                            onNavigateToFocusSetup = onNavigateToFocusSetup,
                            onTaskCompletionToggled = onTaskCompletionToggled
                        )
                    }
                }

                if (metrics != null) {
                    item { ProgressAndStreakPanel(metrics = metrics) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FocusTopAppBar() {
    Column(modifier = Modifier.background(mainBackground)) {
        TopAppBar(
            title = { Text("StudyPilot", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = primaryBlue, fontFamily = Roboto) },
            actions = {
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .border(1.dp, primaryBlue, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("FOCUS MODE", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = primaryBlue, fontFamily = Roboto)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = mainBackground)
        )
        Divider(color = softDividerLine, thickness = 1.dp)
    }
}

@Composable
private fun ContextHeader() {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("Good Afternoon", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textPrimary, fontFamily = Roboto, lineHeight = 28.sp)
        Text("Focus on what matters today", fontSize = 14.sp, color = textSecondary, fontFamily = Roboto, lineHeight = 19.6.sp)
    }
}

@Composable
private fun TodayFocusHeader(
    isSwapMode: Boolean,
    onCancelSwap: () -> Unit,
    onEnterSwapMode: () -> Unit,
    onSaveSwap: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text("Today’s Focus", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textPrimary, fontFamily = Roboto)
            Text("Flexible for today", fontSize = 12.sp, color = textSecondary, fontFamily = Roboto)
        }
        if (isSwapMode) {
            Row {
                TextButton(onClick = onCancelSwap) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onSaveSwap) {
                    Text("Save")
                }
            }
        } else {
            IconButton(onClick = onEnterSwapMode) {
                Icon(Icons.Rounded.SwapHoriz, contentDescription = "Swap Sessions")
            }
        }
    }
}

@Composable
private fun FocusSessionCard(
    session: FocusSession,
    isSwapMode: Boolean,
    isSourceNode: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val isCompleted = session.status == SessionStatus.COMPLETED
    val animatedCardColor by animateColorAsState(if (isSourceNode) primaryBlue.copy(alpha = 0.1f) else cardBackground)
    val indicatorColor = when {
        isCompleted -> completedGreen
        isCurrent -> currentBlue
        else -> upcomingGrey
    }

    val cardHeight = 74.dp
    val cardBorder = if (isSourceNode) BorderStroke(1.dp, primaryBlue) else BorderStroke(1.dp, cardBorder)
    val elevation = if (isSourceNode) 8.dp else 2.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .shadow(elevation = elevation, shape = RoundedCornerShape(14.dp), spotColor = cardShadowColor, ambientColor = cardShadowColor)
            .clickable(enabled = !isCompleted && isSwapMode, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = animatedCardColor),
        border = cardBorder,
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(indicatorColor, CircleShape)
                    .border(
                        1.5.dp,
                        if (isSourceNode) primaryBlue.copy(alpha = 0.8f) else Color.Transparent,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = session.sessionNumber.toString(),
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Roboto
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(session.subjectName, fontSize = 14.sp, color = textPrimary, fontWeight = FontWeight.Medium, fontFamily = Roboto)
                Text("${session.duration} min", fontSize = 12.sp, color = textSecondary, fontFamily = Roboto)
            }

            if (isCompleted) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = "Completed", tint = completedGreen, modifier = Modifier.padding(end = 12.dp))
            }
        }
    }
}

@Composable
private fun BeginFocusSessionButton(onClick: () -> Unit) {
    Spacer(Modifier.height(4.dp))
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(18.dp), spotColor = primaryBlue.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = listOf(Color(0xFF2196F3), primaryBlue))),
            contentAlignment = Alignment.Center
        ) {
            Text("Begin Focus Session →", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = Roboto)
        }
    }
}

@Composable
private fun TasksPanel(
    tasks: List<FocusTask>,
    onNavigateToFocusSetup: () -> Unit,
    onTaskCompletionToggled: (String, Boolean) -> Unit
) {
    Column {
        Text("Suggested Tasks", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = textPrimary, fontFamily = Roboto)
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = tasks.isEmpty(), onClick = onNavigateToFocusSetup),  // MAKE CLICKABLE WHEN EMPTY
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackground),
            border = BorderStroke(1.dp, cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AssignmentTurnedIn,
                            contentDescription = "Add tasks",
                            tint = textSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            "No tasks. Add a task.",
                            color = textSecondary,
                            fontFamily = Roboto,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.padding(16.dp)) {
                    tasks.forEachIndexed { index, task ->
                        TaskRow(task = task,onTaskCompletionToggled = onTaskCompletionToggled)
                        if (index < tasks.size - 1) {
                            Divider(color = softDividerLine, modifier = Modifier.padding(vertical = 12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: FocusTask,
    onTaskCompletionToggled: (String, Boolean) -> Unit
) {
    // Inline status calculation
    val isCompleted = task.completed
    val isOverdue = if (!task.completed && task.dueDate != null) {
        task.dueDate < System.currentTimeMillis()
    } else false

    val backgroundColor = when {
        isCompleted -> Color(0xFFE8F5E9) // Light green
        isOverdue -> Color(0xFFFFEBEE) // Light red
        else -> Color.Transparent
    }

    val textColor = when {
        isCompleted -> textSecondary
        isOverdue -> Color(0xFFD32F2F)
        else -> textPrimary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp)
    ) {
        androidx.compose.material3.Checkbox(
            checked = task.completed,
            onCheckedChange = { isChecked ->
                onTaskCompletionToggled(task.id, isChecked)
            },
            colors = androidx.compose.material3.CheckboxDefaults.colors(
                checkedColor = completedGreen,
                uncheckedColor = if (isOverdue) Color(0xFFD32F2F) else textSecondary
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.name,
                fontSize = 14.sp,
                color = textColor,
                fontWeight = FontWeight.Medium,
                fontFamily = Roboto,
                textDecoration = if (task.completed) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
            )
            Text(
                text = "${task.type.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }} • ${task.relatedSubject ?: "None"}",
                fontSize = 12.sp,
                color = textSecondary,
                fontFamily = Roboto
            )
        }

        task.dueDate?.let {
            val formattedDate = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(it))
            Text(
                text = if (isOverdue) "Overdue" else "Due $formattedDate",
                fontSize = 12.sp,
                color = if (isOverdue) Color(0xFFD32F2F) else textSecondary,
                fontWeight = if (isOverdue) FontWeight.SemiBold else FontWeight.Normal,
                fontFamily = Roboto
            )
        }
    }
}

@Composable
private fun ProgressAndStreakPanel(metrics: FocusMetrics) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackground),
            border = BorderStroke(1.dp, cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProgressMetric(icon = Icons.Outlined.CheckCircleOutline, value = metrics.completedToday.toString(), label = "Completed")
                FocusProgressDivider()
                ProgressMetric(icon = Icons.Outlined.HourglassTop, value = metrics.pendingToday.toString(), label = "Pending")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null, tint = primaryBlue, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Focus Streak: ${metrics.focusStreak} days", color = primaryBlue, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = Roboto)
        }
    }
}


@Composable
private fun EmptySessionsCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = softCardHighlight),
        border = BorderStroke(1.5.dp, primaryBlue.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddCircleOutline,
                    contentDescription = "Add subjects",
                    tint = primaryBlue,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    "Add subjects to schedule sessions",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = primaryBlue,
                    fontFamily = Roboto,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ProgressMetric(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(imageVector = icon, contentDescription = label, tint = textSecondary, modifier = Modifier.size(24.dp))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = textPrimary, fontFamily = Roboto)
        Text(label, fontSize = 12.sp, color = textSecondary, fontFamily = Roboto)
    }
}

@Composable
private fun FocusProgressDivider() {
    Box(modifier = Modifier.width(1.dp).height(40.dp).background(softDividerLine))
}
