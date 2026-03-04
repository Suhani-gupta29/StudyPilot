package com.example.studypilot.ui.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studypilot.ui.mode.StudyMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import androidx.compose.material.icons.filled.Info
import com.example.studypilot.ui.home.HomeBottomNavigationBar
import com.example.studypilot.ui.theme.Roboto


// ─── Minimalist Color palette ──────────────────────────────────────────────
private val primaryBlue = Color(0xFF1E88E5)
private val textPrimary = Color(0xFF102A43)
private val textSecondary = Color(0xFF627D98)
private val cardBackground = Color(0xFFFFFFFF)
private val mainBackground = Color(0xFFFAFBFC)
private val cardBorder = Color(0xFFE0E7F1)
private val softDividerLine = Color(0xFFE6ECF5)

// Status colors - used sparingly
private val ColorCompleted = Color(0xFF2E7D32)
private val ColorPartial   = Color(0xFFFFA726)
private val ColorMissed    = Color(0xFFF44336)
private val ColorPlanned   = Color(0xFF9E9E9E)
private val ColorToday     = primaryBlue

// ─── Entry point ──────────────────────────────────────────────────────────────
@Composable
fun PlannerScreen(
    viewModel: PlannerViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToSubjects: () -> Unit,
    onDoItToday: ((Int) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            PlannerTopBar(
                mode = uiState.selectedMode,
                onBack = onNavigateBack
            )
        },
        bottomBar = {
            HomeBottomNavigationBar(
                onNavigateToPlanner = {},
                onNavigateToHome = onNavigateToHome,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToAnalytics = onNavigateToAnalytics,
                onNavigateToSubjects = onNavigateToSubjects,
                activeIndex = 1
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE3F2FD),  // gradientTop
                            Color(0xFFFFFFFF)
                        )
                    )
                )
                .padding(padding)
        ) {

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = primaryBlue)
                }
            } else {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // 1. Missed-backlog banner (exam + focus only)
                    if (uiState.selectedMode != StudyMode.CASUAL && uiState.missedBacklog > 0) {
                        item { MissedBacklogBanner(uiState.missedBacklog) }
                    }

                    // 2. Casual weekly-pool bar
                    uiState.weeklyPool?.let { pool ->
                        item { CasualPoolBar(pool) }
                    }

                    // 3. Navigation header + calendar / week strip
                    item {
                        when (uiState.selectedMode) {
                            StudyMode.EXAM -> ExamPlannerStrip(uiState, viewModel)
                            StudyMode.FOCUS,
                            StudyMode.CASUAL -> WeekStrip(uiState, viewModel)
                        }
                    }

                    // 4. Day-detail panel
                    uiState.selectedDayDetail?.let { detail ->
                        item { Spacer(Modifier.height(4.dp)) }
                        item { DayDetailPanel(detail, uiState.selectedMode, viewModel) }
                    }
                }
            }
        }
    }

}

// ─── Top bar ──────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlannerTopBar(mode: StudyMode, onBack: () -> Unit) {
    val modeLabel = when (mode) {
        StudyMode.EXAM   -> "EXAM MODE"
        StudyMode.FOCUS  -> "FOCUS MODE"
        StudyMode.CASUAL -> "CASUAL MODE"
    }

    Column {
        TopAppBar(
            title = {
                Text(
                    text = "StudyPilot",
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
                        .border(
                            width = 1.dp,
                            color = primaryBlue,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = modeLabel.uppercase(),
                        color = primaryBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = Roboto
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )
        Divider(color = softDividerLine)
    }
}

// ─── Missed-backlog banner ────────────────────────────────────────────────────
@Composable
private fun MissedBacklogBanner(missed: Int) {
    val sessionWord = if (missed == 1) "session" else "sessions"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.05f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(ColorMissed.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    androidx.compose.material.icons.Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = ColorMissed,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    "Catch Up Needed",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = textPrimary,
                    fontFamily = Roboto
                )
                Text(
                    "$missed missed $sessionWord to catch up",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSecondary,
                    fontFamily = Roboto
                )
            }
        }
    }
}

// ─── Casual weekly pool progress bar ─────────────────────────────────────────
@Composable
private fun CasualPoolBar(pool: WeeklyPoolInfo) {
    val progress = if (pool.totalPlanned > 0) pool.totalDone.toFloat() / pool.totalPlanned else 0f
    val remaining = (pool.totalPlanned - pool.totalDone).coerceAtLeast(0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.05f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "This week",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = textPrimary,
                    fontFamily = Roboto
                )
                Text(
                    "${pool.totalDone} / ${pool.totalPlanned}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSecondary,
                    fontWeight = FontWeight.Medium,
                    fontFamily = Roboto
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(Color(0xFFF0F0F0), RoundedCornerShape(5.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(primaryBlue, Color(0xFF42A5F5))
                            ),
                            RoundedCornerShape(5.dp)
                        )
                )
            }

            if (remaining > 0) {
                Text(
                    "$remaining sessions remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary,
                    fontFamily = Roboto
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("✓", color = ColorCompleted, fontSize = 14.sp)
                    Text(
                        "All sessions completed!",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorCompleted,
                        fontWeight = FontWeight.Medium,
                        fontFamily = Roboto
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// WEEKLY STRIP (Focus / Casual)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun WeekStrip(uiState: PlannerUiState, vm: PlannerViewModel) {
    val selectedDate = uiState.selectedDate
    val weekStart    = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekEnd      = selectedDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

    val weekLabel = "${formatShortDate(weekStart)} – ${formatShortDate(weekEnd)}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.05f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Week nav header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { vm.navigateWeek(false) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.ChevronLeft,
                        "Previous week",
                        tint = textSecondary
                    )
                }
                Text(
                    weekLabel,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = textPrimary,
                    fontFamily = Roboto
                )
                IconButton(
                    onClick = { vm.navigateWeek(true) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.ChevronRight,
                        "Next week",
                        tint = textSecondary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 7-day strip
            val datesInWeek = uiState.calendarDates.filter {
                !it.date.isBefore(weekStart) && !it.date.isAfter(weekEnd)
            }

            if (datesInWeek.isEmpty() && weekStart.isAfter(LocalDate.now())) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Color(0xFFF8F9FA), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Future week - plan will be available when the week starts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondary,
                        textAlign = TextAlign.Center,
                        fontFamily = Roboto,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                return@Column
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (dayOffset in 0..6) {
                    val d = weekStart.plusDays(dayOffset.toLong())

                    // Skip Sunday for Casual mode
                    if (uiState.selectedMode == StudyMode.CASUAL && d.dayOfWeek == DayOfWeek.SUNDAY) {
                        continue
                    }

                    val info = datesInWeek.firstOrNull { it.date.isEqual(d) }
                    val isSelected = d.isEqual(uiState.selectedDate)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = { vm.selectDate(d) }),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Day-of-week label
                            Text(
                                d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondary,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Normal,
                                fontFamily = Roboto,
                                fontSize = 11.sp
                            )

                            // Circle with day number
                            val isToday = d.isEqual(LocalDate.now())
                            val circleColor = when {
                                isSelected -> primaryBlue
                                isToday    -> Color(0xFFE3F2FD)
                                else       -> Color.Transparent
                            }
                            val textColor = when {
                                isSelected -> Color.White
                                isToday    -> primaryBlue
                                else       -> textPrimary
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(circleColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    d.dayOfMonth.toString(),
                                    fontSize = 14.sp,
                                    color = textColor,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    fontFamily = Roboto
                                )
                            }

                            // Status dot
                            val dotColor = info?.let { dotColorForStatus(it.status) }
                            if (dotColor != null && info!!.status != DayStatus.EMPTY) {
                                Box(
                                    Modifier
                                        .size(6.dp)
                                        .background(dotColor, CircleShape)
                                )
                            } else {
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamPlannerStrip(uiState: PlannerUiState, vm: PlannerViewModel) {
    // Phase colors mapping
    val phaseColors = mapOf(
        "Syllabus" to Color(0xFF42A5F5),
        "Revision" to Color(0xFFFFA726),
        "Testing" to Color(0xFFEF5350)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.05f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Navigation header with date range
            val examStart = uiState.calendarDates.firstOrNull()?.date
            val examEnd = uiState.examEndDate?.minusDays(1)

            if (examStart != null && examEnd != null) {
                val rangeLabel = "${formatShortDate(examStart)} – ${formatShortDate(examEnd)}"

                Text(
                    rangeLabel,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = textPrimary,
                    fontFamily = Roboto
                )

                Spacer(Modifier.height(12.dp))
            }

            // Phase Legend
            val phases = uiState.calendarDates.mapNotNull { it.phaseLabel }.distinct()
            if (phases.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8F9FA), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    phases.forEach { phase ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .background(phaseColors[phase] ?: Color.Gray, CircleShape)
                            )
                            Text(
                                phase,
                                style = MaterialTheme.typography.bodySmall,
                                color = textPrimary,
                                fontWeight = FontWeight.Medium,
                                fontFamily = Roboto,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Horizontal scrollable strip
            val datesByPhase = uiState.calendarDates.groupBy { it.phaseLabel ?: "Unknown" }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                datesByPhase.forEach { (phase, datesInPhase) ->
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                phase,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = phaseColors[phase] ?: Color.Gray,
                                fontFamily = Roboto,
                                fontSize = 11.sp
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                    }

                    items(datesInPhase.size) { index ->
                        val info = datesInPhase[index]
                        val isSelected = info.date.isEqual(uiState.selectedDate)
                        ExamDayCell(info, isSelected, phaseColors[phase] ?: Color.Gray) {
                            vm.selectDate(info.date)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamDayCell(
    info: CalendarDateInfo,
    isSelected: Boolean,
    phaseColor: Color,
    onClick: () -> Unit
) {
    val dotColor = dotColorForStatus(info.status)

    Box(
        modifier = Modifier
            .width(48.dp)
            .height(76.dp)
            .background(
                if (isSelected) phaseColor.copy(alpha = 0.15f)
                else Color(0xFFFAFAFA),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                info.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = textSecondary,
                fontSize = 10.sp,
                fontFamily = Roboto
            )

            val textColor = when {
                isSelected -> phaseColor
                info.isToday -> primaryBlue
                else -> textPrimary
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (isSelected) phaseColor.copy(alpha = 0.15f)
                        else if (info.isToday) Color(0xFFE3F2FD)
                        else Color.Transparent,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    info.date.dayOfMonth.toString(),
                    fontSize = 13.sp,
                    color = textColor,
                    fontWeight = if (info.isToday || isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    fontFamily = Roboto
                )
            }

            if (info.status != DayStatus.EMPTY) {
                Box(
                    Modifier
                        .size(6.dp)
                        .background(dotColor, CircleShape)
                )
            } else {
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// DAY DETAIL PANEL
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun DayDetailPanel(detail: DayDetail, mode: StudyMode, viewModel: PlannerViewModel? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.05f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val dateText = detail.date.format(
                        DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
                    )
                    Text(
                        dateText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = textPrimary,
                        fontFamily = Roboto
                    )
                    val badge = when {
                        detail.isToday      -> "Today"
                        detail.isFutureDate -> "Upcoming"
                        else                -> "Past"
                    }
                    Text(
                        badge,
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                        fontFamily = Roboto
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            if (detail.completedCount >= detail.plannedCount && detail.plannedCount > 0)
                                ColorCompleted.copy(alpha = 0.1f)
                            else Color(0xFFF0F0F0),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "${detail.completedCount}/${detail.plannedCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (detail.completedCount >= detail.plannedCount && detail.plannedCount > 0)
                            ColorCompleted else textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = Roboto
                    )
                }
            }

            // Total time studied
            if (detail.totalStudyMinutes > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8F9FA), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⏱️", fontSize = 16.sp)
                    Text(
                        "Total: ${detail.totalStudyMinutes} min",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textPrimary,
                        fontWeight = FontWeight.Medium,
                        fontFamily = Roboto
                    )
                }
            }

            // Exam redistribution notice
            if (mode == StudyMode.EXAM && detail.redistributedExtra > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3E5F5), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Info,
                        "Info",
                        tint = Color(0xFF7B1FA2),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "+${detail.redistributedExtra} session(s) added",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4A148C),
                        fontFamily = Roboto
                    )
                }
            }

            if (detail.plannedSessions.isNotEmpty() || detail.tasksForDay.isNotEmpty()) {
                HorizontalDivider(color = softDividerLine)
            }

            // Planned sessions list
            if (detail.plannedSessions.isNotEmpty()) {
                Text(
                    "Sessions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    fontFamily = Roboto
                )

                detail.plannedSessions.forEachIndexed { index, session ->
                    PlannedSessionRow(
                        session = session,
                        mode = mode,
                        dayDetail = detail,  // ADD THIS
                        onExempt = if (mode == StudyMode.EXAM && !detail.isFutureDate && viewModel != null) {
                            { sessionIndex -> viewModel.exemptSession(detail.date, sessionIndex) }
                        } else null,
                        onDoItToday = if (mode == StudyMode.FOCUS && !detail.isFutureDate && !detail.isToday && viewModel != null) {  // ADD THIS
                            { sessionIndex -> viewModel.doItToday(sessionIndex, detail.date) }
                        } else null
                    )
                }
            }

            // Tasks due on this day
            if (detail.tasksForDay.isNotEmpty()) {
                if (detail.plannedSessions.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = softDividerLine)
                    Spacer(Modifier.height(4.dp))
                }

                Text(
                    "Tasks",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    fontFamily = Roboto
                )

                detail.tasksForDay.forEach { task ->
                    TaskRow(task)
                }
            }

            // Empty-state for future dates with no sessions
            if (detail.plannedCount == 0 && detail.isFutureDate) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8F9FA), RoundedCornerShape(10.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No sessions planned",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondary,
                        textAlign = TextAlign.Center,
                        fontFamily = Roboto
                    )
                }
            }
        }
    }
}

@Composable
private fun PlannedSessionRow(
    session: PlannedSessionInfo,
    mode: StudyMode = StudyMode.EXAM,
    dayDetail: DayDetail,
    onExempt: ((Int) -> Unit)? = null,
    onDoItToday: ((Int) -> Unit)? = null
) {
    var showExemptDialog by remember { mutableStateOf(false) }
    var showDoItTodayDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (session.wasCompleted) ColorCompleted.copy(alpha = 0.05f)
                else Color(0xFFF8F9FA),
                RoundedCornerShape(10.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    if (session.wasCompleted) ColorCompleted else Color(0xFFE0E0E0),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (session.wasCompleted) {
                Icon(
                    androidx.compose.material.icons.Icons.Default.Check,
                    "Done",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(
                    session.sessionNumber.toString(),
                    color = textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    fontFamily = Roboto
                )
            }
        }

        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    session.subjectName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (session.wasCompleted) textSecondary else textPrimary,
                    fontFamily = Roboto
                )

                // Show badge for redistributed sessions
                if (session.isRedistributed && mode == StudyMode.EXAM) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFF3E0), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Catch-up",
                            fontSize = 10.sp,
                            color = Color(0xFFFF6F00),
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = Roboto
                        )
                    }
                }
            }

            Text(
                "${session.durationMinutes} min",
                style = MaterialTheme.typography.bodySmall,
                color = textSecondary,
                fontFamily = Roboto,
                fontSize = 12.sp
            )
        }

        // Show skip button for redistributed sessions that aren't completed
        if (mode == StudyMode.EXAM && session.isRedistributed && !session.wasCompleted && onExempt != null && !dayDetail.isFutureDate) {
            TextButton(
                onClick = { showExemptDialog = true },
                colors = ButtonDefaults.textButtonColors(contentColor = ColorMissed)
            ) {
                Text(
                    "Skip",
                    fontSize = 12.sp,
                    fontFamily = Roboto
                )
            }
        }

        // Show "Do It Today" button for past incomplete sessions in Focus mode
        if (mode == StudyMode.FOCUS && !dayDetail.isFutureDate && !dayDetail.isToday && !session.wasCompleted && !session.isAddedToday && onDoItToday != null) {
            TextButton(
                onClick = { showDoItTodayDialog = true },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF4CAF50))
            ) {
                Text(
                    "Do It Today",
                    fontSize = 12.sp,
                    fontFamily = Roboto
                )
            }
        }
    }

    // Exemption confirmation dialog
    if (showExemptDialog && onExempt != null) {
        AlertDialog(
            onDismissRequest = { showExemptDialog = false },
            title = {
                Text(
                    "Skip this session?",
                    fontFamily = Roboto,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    "This catch-up session will be removed from your plan for today. You can't undo this action.",
                    fontFamily = Roboto
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onExempt(session.sessionNumber - 1) // Pass 0-based index
                        showExemptDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ColorMissed)
                ) {
                    Text("Skip Session", fontFamily = Roboto)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExemptDialog = false }) {
                    Text("Cancel", fontFamily = Roboto)
                }
            }
        )
    }

    if (showDoItTodayDialog) {
        AlertDialog(
            onDismissRequest = { showDoItTodayDialog = false },
            title = { Text("Add to Today?") },
            text = { Text("Add this session to today's plan?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDoItToday?.invoke(session.sessionNumber - 1)
                        showDoItTodayDialog = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDoItTodayDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TaskRow(task: TaskInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (task.completed) Color(0xFFE8F5E9) else Color(0xFFF3E5F5),
                RoundedCornerShape(10.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    if (task.completed) ColorCompleted else Color(0xFF9C27B0),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (task.completed) {
                Icon(
                    androidx.compose.material.icons.Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text("📝", fontSize = 14.sp)
            }
        }

        Column(Modifier.weight(1f)) {
            Text(
                task.taskName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (task.completed) textSecondary else textPrimary,
                fontFamily = Roboto,
                textDecoration = if (task.completed) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
            )
            task.relatedSubject?.let {
                Text(
                    "$it • ${task.taskType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary,
                    fontFamily = Roboto,
                    fontSize = 12.sp
                )
            } ?: Text(
                task.taskType,
                style = MaterialTheme.typography.bodySmall,
                color = textSecondary,
                fontFamily = Roboto,
                fontSize = 12.sp
            )
        }
    }
}

// ─── Shared utility functions ─────────────────────────────────────────────────

private fun dotColorForStatus(status: DayStatus): Color = when (status) {
    DayStatus.COMPLETED -> ColorCompleted
    DayStatus.PARTIAL   -> ColorPartial
    DayStatus.MISSED    -> ColorMissed
    DayStatus.PLANNED   -> ColorPlanned
    DayStatus.EMPTY     -> Color.Transparent
}

private fun formatShortDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))