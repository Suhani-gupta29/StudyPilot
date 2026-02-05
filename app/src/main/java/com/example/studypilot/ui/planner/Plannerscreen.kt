package com.example.studypilot.ui.planner

import androidx.compose.foundation.background
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


// ─── Colour palette (matches your existing theme intent) ─────────────────────
private val ColorCompleted = Color(0xFF4CAF50)   // green
private val ColorPartial   = Color(0xFFFFC107)   // amber
private val ColorMissed    = Color(0xFFF44336)   // red
private val ColorPlanned  = Color(0xFF9E9E9E)   // grey
private val ColorToday    = Color(0xFF6200EE)   // primary purple
private val ColorExamDay  = Color(0xFFFF5722)   // deep-orange accent
private val ColorPhaseBackground = Color(0x10000000) // very subtle tint

// ─── Entry point ──────────────────────────────────────────────────────────────
@Composable
fun PlannerScreen(
    viewModel: PlannerViewModel,
    onNavigateBack: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {}
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
                activeIndex = 1  // Planner is index 1
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // ... rest of your existing LazyColumn content remains the same

            // 1. Missed-backlog banner  (exam + focus only)
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
                    StudyMode.EXAM    -> ExamPlannerStrip(uiState, viewModel)
                    StudyMode.FOCUS,
                    StudyMode.CASUAL  -> WeekStrip(uiState, viewModel)
                }
            }

            // 4. Day-detail panel
            uiState.selectedDayDetail?.let { detail ->
                item { Spacer(Modifier.height(4.dp)) }
                item { DayDetailPanel(detail, uiState.selectedMode) }
            }
        }
    }
}

// ─── Top bar ──────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlannerTopBar(mode: StudyMode, onBack: () -> Unit) {
    val modeLabel = when (mode) {
        StudyMode.EXAM   -> "Exam Planner"
        StudyMode.FOCUS  -> "Focus Planner"
        StudyMode.CASUAL -> "Casual Planner"
    }
    TopAppBar(
        title = { Text(modeLabel, fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(androidx.compose.material.icons.Icons.Default.ArrowBack, "Back")
            }
        }
    )
}

// ─── Missed-backlog banner ────────────────────────────────────────────────────
@Composable
private fun MissedBacklogBanner(missed: Int) {
    val sessionWord = if (missed == 1) "session" else "sessions"
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(Color(0xFFFFF3E0), RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            androidx.compose.material.icons.Icons.Default.Warning,
            contentDescription = "Warning",
            tint = ColorMissed
        )
        Text(
            "You have $missed missed $sessionWord to catch up",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFE65100),
            fontWeight = FontWeight.Medium
        )
    }
}

// ─── Casual weekly pool progress bar ─────────────────────────────────────────
@Composable
private fun CasualPoolBar(pool: WeeklyPoolInfo) {
    val progress = if (pool.totalPlanned > 0) pool.totalDone.toFloat() / pool.totalPlanned else 0f
    val remaining = (pool.totalPlanned - pool.totalDone).coerceAtLeast(0)

    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("This Week", fontWeight = FontWeight.SemiBold)
                Text("${pool.totalDone} / ${pool.totalPlanned} sessions", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = ColorCompleted,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            if (remaining > 0) {
                Text("$remaining sessions remaining this week", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("All sessions done this week!", style = MaterialTheme.typography.bodySmall, color = ColorCompleted, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// WEEKLY STRIP  (Focus / Casual)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun WeekStrip(uiState: PlannerUiState, vm: PlannerViewModel) {
    val selectedDate = uiState.selectedDate
    val weekStart    = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekEnd      = selectedDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

    val weekLabel = "${formatShortDate(weekStart)} – ${formatShortDate(weekEnd)}"

    // ── week nav header ──
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        IconButton(onClick = { vm.navigateWeek(false) }) {
            Icon(androidx.compose.material.icons.Icons.Default.ChevronLeft, "Previous week")
        }
        Text(weekLabel, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        IconButton(onClick = { vm.navigateWeek(true) }) {
            Icon(androidx.compose.material.icons.Icons.Default.ChevronRight, "Next week")
        }
    }

    Spacer(Modifier.height(8.dp))

    // 7-day strip
    val datesInWeek = uiState.calendarDates.filter {
        !it.date.isBefore(weekStart) && !it.date.isAfter(weekEnd)
    }



    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        // Show empty state if no calendar dates (future week)
        if (datesInWeek.isEmpty() && weekStart.isAfter(LocalDate.now())) {
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Future week - plan will be available when the week starts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            return
        }
        // If calendarDates doesn't cover the full week yet, fill with blanks
        for (dayOffset in 0..6) {
            val d = weekStart.plusDays(dayOffset.toLong())

            // Skip Sunday rendering for Casual mode
            if (uiState.selectedMode == StudyMode.CASUAL && d.dayOfWeek == DayOfWeek.SUNDAY) {
                continue
            }

            val info = datesInWeek.firstOrNull { it.date.isEqual(d) }
            val isSelected = d.isEqual(uiState.selectedDate)

            Box(
                modifier = Modifier.weight(1f)
                    .clickable(onClick = { vm.selectDate(d) }),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // day-of-week label
                    Text(
                        d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    // circle with day number
                    val isToday = d.isEqual(LocalDate.now())
                    val circleColor = when {
                        isSelected -> ColorToday
                        isToday    -> ColorToday.copy(alpha = 0.15f)
                        else       -> Color.Transparent
                    }
                    val textColor = when {
                        isSelected -> Color.White
                        isToday    -> ColorToday
                        else       -> MaterialTheme.colorScheme.onSurface
                    }

                    Box(
                        Modifier.size(36.dp).background(circleColor, CircleShape),
                        Alignment.Center
                    ) {
                        Text(d.dayOfMonth.toString(), fontSize = 14.sp, color = textColor,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center)
                    }

                    // status dot
                    val dotColor = info?.let { dotColorForStatus(it.status) }
                    if (dotColor != null && info!!.status != DayStatus.EMPTY) {
                        Box(Modifier.size(6.dp).background(dotColor, CircleShape))
                    } else {
                        Spacer(Modifier.height(6.dp))   // keep alignment
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

    // Navigation header with date range
    val examStart = uiState.calendarDates.firstOrNull()?.date
    val examEnd = uiState.examEndDate?.minusDays(1) // One day before exam

    if (examStart != null && examEnd != null) {
        val rangeLabel = "${formatShortDate(examStart)} – ${formatShortDate(examEnd)}"

        Text(
            rangeLabel,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))
    }

    // Phase Legend
    val phases = uiState.calendarDates.mapNotNull { it.phaseLabel }.distinct()
    if (phases.isNotEmpty()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            phases.forEach { phase ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        Modifier.size(10.dp)
                            .background(phaseColors[phase] ?: Color.Gray, CircleShape)
                    )
                    Text(
                        phase,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    // Horizontal scrollable strip with phase-segmented days
    // Group dates by phase
    val datesByPhase = uiState.calendarDates.groupBy { it.phaseLabel ?: "Unknown" }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        datesByPhase.forEach { (phase, datesInPhase) ->
            // Phase separator/header
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        phase,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = phaseColors[phase] ?: Color.Gray
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }

            // Days in this phase
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
            .height(72.dp)
            .background(
                if (isSelected) phaseColor.copy(alpha = 0.3f)
                else phaseColor.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Day of week
            Text(
                info.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )

            // Day number with selection indicator
            val textColor = when {
                isSelected -> Color.White
                info.isToday -> ColorToday
                else -> MaterialTheme.colorScheme.onSurface
            }
            val bgColor = if (isSelected) phaseColor else Color.Transparent

            Box(
                Modifier.size(32.dp).background(bgColor, CircleShape),
                Alignment.Center
            ) {
                Text(
                    info.date.dayOfMonth.toString(),
                    fontSize = 14.sp,
                    color = textColor,
                    fontWeight = if (info.isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }

            // Status dot
            if (info.status != DayStatus.EMPTY) {
                Box(Modifier.size(6.dp).background(dotColor, CircleShape))
            } else {
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// DAY DETAIL PANEL  (shown below calendar for the selected date)
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun DayDetailPanel(detail: DayDetail, mode: StudyMode) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // ── header ──
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                val dateText = detail.date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
                val badge = when {
                    detail.isToday      -> " (Today)"
                    detail.isFutureDate -> " (Upcoming)"
                    else                -> ""
                }
                Text("$dateText$badge", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)

                // compact stats chip
                Text("${detail.completedCount}/${detail.plannedCount} done",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (detail.completedCount >= detail.plannedCount && detail.plannedCount > 0)
                        ColorCompleted else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            // total time studied
            if (detail.totalStudyMinutes > 0) {
                Text("Total study time: ${detail.totalStudyMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // exam redistribution notice
            if (mode == StudyMode.EXAM && detail.redistributedExtra > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(Color(0xFFF3E5F5), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(androidx.compose.material.icons.Icons.Default.Info, "Info", tint = Color(0xFF7B1FA2), modifier = Modifier.size(18.dp))
                    Text("+${detail.redistributedExtra} session(s) added to catch up",
                        style = MaterialTheme.typography.bodySmall, color = Color(0xFF4A148C))
                }
            }

            HorizontalDivider()

            // ── planned sessions list ──
            if (detail.plannedSessions.isNotEmpty()) {
                Text("Planned", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                detail.plannedSessions.forEach { session ->
                    PlannedSessionRow(session)
                }
            }


            // ── empty-state for future dates with no sessions ──
            if (detail.plannedCount == 0 && detail.isFutureDate) {
                Text("No sessions planned for this day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PlannedSessionRow(session: PlannedSessionInfo) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // status circle
        val circleColor = if (session.wasCompleted) ColorCompleted else Color(0xFFBDBDBD)
        Box(Modifier.size(20.dp).background(circleColor, CircleShape), Alignment.Center) {
            if (session.wasCompleted) {
                Icon(androidx.compose.material.icons.Icons.Default.Check, "Done", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }

        Column(Modifier.weight(1f)) {
            Text("Session ${session.sessionNumber} – ${session.subjectName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (session.wasCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.onSurface
            )
            Text("${session.durationMinutes} min", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (session.wasCompleted) {
            Text("✓", color = ColorCompleted, fontWeight = FontWeight.Bold)
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