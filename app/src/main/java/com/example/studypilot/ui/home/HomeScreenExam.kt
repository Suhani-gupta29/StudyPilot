package com.example.studypilot.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.LocalFireDepartment
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studypilot.R
import com.example.studypilot.ui.shared.SessionStatus
import com.example.studypilot.ui.shared.StudySession
import com.example.studypilot.ui.theme.Roboto

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

private val cardShadowColor = Color.Black.copy(alpha = 0.06f)
private val completedGreen = Color(0xFF2E7D32)
private val currentBlue = Color(0xFF1E88E5)
private val upcomingGrey = Color(0xFF90A4AE)
private val urgencyNormalBg = Color(0xFFE3F2FD)
private val urgencyWarningBg = Color(0xFFFFF4E5)
private val urgencyCriticalBg = Color(0xFFFDECEA)
private val alertBackground = Color(0xFFEEF5FF)

private val gradientTop = Color(0xFFE3F2FD)
private val gradientBottom = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenExam(
    examDetails: ExamDetails?,
    sessions: List<StudySession>?,
    metrics: AccountabilityMetrics?,
    alerts: List<AlertMessage>,
    studyStreak: Int?,
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
    // ── Subject change params ──────────────────────────────────────────────
    isSubjectChangeMode: Boolean,
    subjectChangeSessionIndex: Int?,
    onEnterSubjectChangeMode: (Int) -> Unit,
    onCancelSubjectChange: () -> Unit,
    onChangeSessionSubject: (Int, String) -> Unit,
    // ── Exam-over subject reset dialog ─────────────────────────────────────
    showExamOverDialog: Boolean,
    onSaveExamSubjectReset: (List<String>) -> Unit,
    onDismissExamOverDialog: () -> Unit,
    onSkipCatchupSession: (Int) -> Unit = {}
) {
    val activeSession = sessions?.firstOrNull { it.status != SessionStatus.COMPLETED }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    // Show exam-over dialog
    if (showExamOverDialog && examDetails != null) {
        val subjectNames = examDetails.subjects.map { it.name }
        if (subjectNames.isNotEmpty()) {
            ExamSubjectResetDialog(
                examName = examDetails.examName,
                subjectNames = subjectNames,
                onSave = onSaveExamSubjectReset,
                onDismiss = onDismissExamOverDialog
            )
        }
    }

    // Show subject change bottom sheet when edit icon is tapped
    if (isSubjectChangeMode && subjectChangeSessionIndex != null) {
        val session = sessions?.getOrNull(subjectChangeSessionIndex)
        val subjectNames = examDetails?.subjects?.map { it.name } ?: emptyList()
        if (session != null && subjectNames.isNotEmpty()) {
            SubjectChangeBottomSheet(
                sessionNumber = session.sessionNumber,
                currentSubject = session.subject,
                availableSubjects = subjectNames,
                onSubjectSelected = { newSubject ->
                    onChangeSessionSubject(subjectChangeSessionIndex, newSubject)
                },
                onDismiss = onCancelSubjectChange
            )
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { ExamTopAppBar(scrollBehavior = scrollBehavior) },
        bottomBar = {
            HomeBottomNavigationBar(
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToAnalytics = onNavigateToAnalytics,
                onNavigateToPlanner = onNavigateToPlanner,
                onNavigateToSubjects = onNavigateToSubjects,
                activeIndex = 0
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = listOf(gradientTop, gradientBottom)))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (examDetails != null) {
                    item { ExamStatusCard(details = examDetails) }
                }
                if (sessions != null && sessions.isNotEmpty()) {
                    item {
                        TodayStudyPlanHeader(
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
                                    SessionCard(
                                        session = session,
                                        isSwapMode = isSwapMode,
                                        isSourceNode = swapSourceIndex == index,
                                        isCurrent = index == firstUncompletedIndex,
                                        showEditSubjectButton = !isSwapMode &&
                                                session.status == SessionStatus.UPCOMING &&
                                                !session.isRedistributed,
                                        onClick = {
                                            if (isSwapMode) onSessionClickedInSwapMode(index)
                                        },
                                        onEditSubjectClicked = {
                                            onEnterSubjectChangeMode(index)
                                        },
                                        onSkipCatchup = if (session.isRedistributed &&
                                            session.status != SessionStatus.COMPLETED) {
                                            { onSkipCatchupSession(index) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    StartSessionButton(
                        enabled = activeSession != null,
                        onClick = {
                            activeSession?.let {
                                onStartSession(it.subject, "EXAM", it.durationMinutes)
                            }
                        }
                    )
                }
                if (metrics != null) {
                    item { AccountabilityPanel(metrics = metrics) }
                }
                items(alerts.take(2)) { alert ->
                    AlertMessageCard(alert = alert)
                }
                if (studyStreak != null) {
                    item { StudyStreak(days = studyStreak) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamTopAppBar(scrollBehavior: TopAppBarScrollBehavior) {
    Column(modifier = Modifier.background(mainBackground)) {
        TopAppBar(
            title = {
                Text(
                    "StudyPilot",
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
                        "EXAM MODE",
                        fontFamily = Roboto,
                        color = primaryBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = mainBackground,
                scrolledContainerColor = mainBackground
            )
        )
        Divider(color = softDividerLine, thickness = 1.dp)
    }
}

@Composable
private fun ExamStatusCard(details: ExamDetails) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = cardShadowColor,
                ambientColor = cardShadowColor
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Row(
            modifier = Modifier.background(
                Brush.verticalGradient(listOf(cardBackground, sectionBackground))
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        primaryBlue,
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = details.examName,
                    fontFamily = Roboto,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    lineHeight = 21.6.sp
                )
                Text(
                    text = "${details.daysRemaining} Days Left",
                    fontFamily = Roboto,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryBlue,
                    lineHeight = 37.8.sp
                )
                Spacer(Modifier.height(4.dp))
                Row {
                    val (urgencyText, urgencyBgColor, urgencyTextColor) = when (details.urgency) {
                        Urgency.NORMAL -> Triple("On Track", urgencyNormalBg, Color(0xFF0D47A1))
                        Urgency.WARNING -> Triple("Approaching Fast", urgencyWarningBg, Color(0xFFE65100))
                        Urgency.CRITICAL -> Triple("Urgent Action", urgencyCriticalBg, Color(0xFFB71C1C))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(urgencyBgColor)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = urgencyText,
                            color = urgencyTextColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = Roboto
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val (phaseText, phaseBgColor, phaseTextColor) = when (details.phase) {
                        StudyPhase.SyllabusCompletion -> Triple("Syllabus", urgencyNormalBg, Color(0xFF0D47A1))
                        StudyPhase.Revision -> Triple("Revision", urgencyWarningBg, Color(0xFFE65100))
                        StudyPhase.Testing -> Triple("Testing", urgencyCriticalBg, Color(0xFFB71C1C))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(phaseBgColor)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = phaseText,
                            color = phaseTextColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = Roboto
                        )
                    }
                }
            }
            Image(
                painter = painterResource(id = R.drawable.image_7),
                contentDescription = "Exam Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }
    }
}

@Composable
private fun TodayStudyPlanHeader(
    isSwapMode: Boolean,
    onCancelSwap: () -> Unit,
    onEnterSwapMode: () -> Unit,
    onSaveSwap: () -> Unit
) {
    if (isSwapMode) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(primaryBlue)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Reorder Sessions",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = Roboto
                )
                Text(
                    text = "Tap to select, tap another to swap",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    fontFamily = Roboto
                )
            }
            Spacer(Modifier.width(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onCancelSwap,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "Cancel",
                        fontSize = 13.sp,
                        fontFamily = Roboto,
                        fontWeight = FontWeight.Medium
                    )
                }
                Button(
                    onClick = onSaveSwap,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = primaryBlue
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        "Save",
                        fontSize = 13.sp,
                        fontFamily = Roboto,
                        fontWeight = FontWeight.Bold,
                        color = primaryBlue
                    )
                }
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Today's Study Plan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = textPrimary,
                    fontFamily = Roboto,
                    lineHeight = 21.6.sp
                )
                Text(
                    text = "Tap ✏️ on a session to change its subject",
                    fontSize = 12.sp,
                    color = textSecondary,
                    fontFamily = Roboto,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 16.2.sp
                )
            }
            IconButton(onClick = onEnterSwapMode) {
                Icon(
                    Icons.Rounded.SwapHoriz,
                    contentDescription = "Swap Sessions",
                    tint = primaryBlue
                )
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: StudySession,
    isSwapMode: Boolean,
    isSourceNode: Boolean,
    isCurrent: Boolean,
    showEditSubjectButton: Boolean,
    onClick: () -> Unit,
    onEditSubjectClicked: () -> Unit,
    onSkipCatchup: (() -> Unit)? = null
) {
    val isCompleted = session.status == SessionStatus.COMPLETED
    val animatedCardColor by animateColorAsState(
        if (isSourceNode) primaryBlue.copy(alpha = 0.1f) else cardBackground
    )
    val indicatorColor = when {
        isCompleted -> completedGreen
        isCurrent -> currentBlue
        else -> upcomingGrey
    }

    val cardHeight = 74.dp
    val border = if (isSourceNode) BorderStroke(1.dp, primaryBlue) else BorderStroke(1.dp, cardBorder)
    val elevation = if (isSourceNode) 8.dp else 2.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(14.dp),
                spotColor = cardShadowColor,
                ambientColor = cardShadowColor
            )
            .clickable(enabled = !isCompleted && isSwapMode, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = animatedCardColor),
        border = border,
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

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = session.subject,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textPrimary,
                    fontFamily = Roboto,
                    lineHeight = 18.9.sp
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${session.durationMinutes} minutes",
                        fontSize = 12.sp,
                        color = textSecondary,
                        fontFamily = Roboto,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 16.2.sp
                    )
                    if (session.isRedistributed) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFFF3E0))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Catch-up",
                                fontSize = 9.sp,
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = Roboto
                            )
                        }
                    }
                }
            }

            when {
                isCompleted -> Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Completed",
                    tint = completedGreen
                )
                session.isRedistributed && onSkipCatchup != null && !isSwapMode -> {
                    TextButton(
                        onClick = onSkipCatchup,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            "Skip",
                            fontSize = 12.sp,
                            color = Color(0xFFE53935),
                            fontFamily = Roboto,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                showEditSubjectButton -> IconButton(
                    onClick = onEditSubjectClicked,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Change subject",
                        tint = textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StartSessionButton(onClick: () -> Unit, enabled: Boolean = true) {
    Spacer(Modifier.height(4.dp))
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = if (enabled) 8.dp else 0.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = primaryBlue.copy(alpha = 0.4f)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color(0xFFE0E0E0)
        ),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled)
                        Brush.verticalGradient(colors = listOf(Color(0xFF2196F3), primaryBlue))
                    else
                        Brush.verticalGradient(colors = listOf(Color(0xFFBDBDBD), Color(0xFF9E9E9E)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (enabled) "Start Session →" else "All sessions complete ✓",
                color = Color.White,
                fontSize = 15.sp,
                fontFamily = Roboto,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AccountabilityPanel(metrics: AccountabilityMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Row(
            Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetricItem(
                icon = Icons.Outlined.CheckCircle,
                number = metrics.completed.toString(),
                label = "Completed"
            )
            VerticalDivider()
            MetricItem(
                icon = Icons.Outlined.Analytics,
                number = metrics.missed.toString(),
                label = "Missed"
            )
            VerticalDivider()
            MetricItem(
                icon = Icons.Outlined.Info,
                number = metrics.backlog.toString(),
                label = "Backlog"
            )
        }
    }
}

@Composable
private fun MetricItem(icon: ImageVector, number: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = textSecondary, modifier = Modifier.size(24.dp))
        Text(number, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = textPrimary, fontFamily = Roboto)
        Text(label, fontSize = 12.sp, color = textSecondary, fontFamily = Roboto)
    }
}

@Composable
private fun VerticalDivider() {
    Box(modifier = Modifier.width(1.dp).height(50.dp).background(softDividerLine))
}

@Composable
private fun AlertMessageCard(alert: AlertMessage) {
    val (icon, color) = when (alert.type) {
        AlertType.MOTIVATIONAL -> Icons.Outlined.Analytics to primaryBlue
        AlertType.WARNING -> Icons.Outlined.Info to Color(0xFFD32F2F)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(alertBackground)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color)
        Text(
            text = alert.message,
            fontSize = 13.sp,
            color = textPrimary,
            fontFamily = Roboto,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun StudyStreak(days: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.LocalFireDepartment,
            contentDescription = null,
            tint = Color(0xFFFF7043),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Study Streak: $days days",
            color = textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = Roboto
        )
    }
}