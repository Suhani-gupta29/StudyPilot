package com.example.studypilot.ui.planner

import com.example.studypilot.ui.mode.StudyMode
import java.time.LocalDate

// ─── Top-level planner UI state ──────────────────────────────────────────────
data class PlannerUiState(
    val selectedMode: StudyMode = StudyMode.FOCUS,
    val selectedDate: LocalDate = LocalDate.now(),
    val visibleMonth: LocalDate = LocalDate.now(),
    val calendarDates: List<CalendarDateInfo> = emptyList(),
    val selectedDayDetail: DayDetail? = null,
    val missedBacklog: Int = 0,
    val weeklyPool: WeeklyPoolInfo? = null,
    val examEndDate: LocalDate? = null,
    val isLoading: Boolean = true
)

// ─── One cell on the calendar ─────────────────────────────────────────────────
data class CalendarDateInfo(
    val date: LocalDate,
    val status: DayStatus,
    val isToday: Boolean,
    val isExamDate: Boolean = false,
    val phaseLabel: String? = null
)

enum class DayStatus {
    COMPLETED,   // green
    PARTIAL,     // yellow
    MISSED,      // red
    PLANNED,     // grey – future, has planned sessions
    EMPTY        // no dot
}

// ─── Bottom sheet content when a date is tapped ──────────────────────────────
data class DayDetail(
    val date: LocalDate,
    val plannedSessions: List<PlannedSessionInfo>,
    val completedSessions: List<CompletedSessionInfo>,
    val plannedCount: Int,
    val completedCount: Int,
    val totalStudyMinutes: Int,
    val isFutureDate: Boolean,
    val isToday: Boolean,
    val redistributedExtra: Int = 0
)

data class PlannedSessionInfo(
    val sessionNumber: Int,
    val subjectName: String,
    val durationMinutes: Int,
    val wasCompleted: Boolean
)

data class CompletedSessionInfo(
    val subjectName: String,
    val elapsedMinutes: Int,
    val wasFullyCompleted: Boolean,
    val timestamp: Long
)

// ─── Casual pool header ───────────────────────────────────────────────────────
data class WeeklyPoolInfo(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val totalPlanned: Int,
    val totalDone: Int
)