package com.example.studypilot.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.studypilot.data.SessionRepository
import com.example.studypilot.data.UserPreferencesRepository
import com.example.studypilot.ui.mode.StudyMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.floor

class PlannerViewModel(
    private val sessionRepository: SessionRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val userId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    // ── cached preferences (loaded once, stable for the lifetime of this screen) ──
    private var cachedSkipSundaysInFocus: Boolean = false
    private var cachedMode: StudyMode = StudyMode.FOCUS
    private var cachedDailyStudyHours: Float = 0.5f
    private var cachedSessionLengthMinutes: Int = 25
    private var cachedExamDateMs: Long = 0L
    private var cachedPlanStartMs: Long = 0L
    private var cachedExamSubjects: List<String> = emptyList()
    private var cachedFocusSubjects: List<String> = emptyList()
    private var cachedCasualSubjects: List<String> = emptyList()

    // ── live snapshot of every DB row for this user ──
    private var allDbSessions: List<com.example.studypilot.data.StudySession> = emptyList()

    // ── redistribution map built during rebuildCalendar, consumed by rebuildDayDetail ──
    private var cachedRedistributionMap: Map<LocalDate, Int> = emptyMap()

    init {
        viewModelScope.launch {
            // 1. Read prefs once on IO (same pattern as AnalyticsViewModel)
            withContext(Dispatchers.IO) {
                try {
                    val prefs = preferencesRepository.getUserPreferences(userId).first()
                    if (prefs != null) {
                        cachedMode = prefs.selectedMode
                        when (prefs.selectedMode) {
                            StudyMode.EXAM -> {
                                cachedDailyStudyHours   = prefs.examPreferences.dailyStudyHours ?: 0.5f
                                cachedSessionLengthMinutes = prefs.examPreferences.sessionLength.minutes ?: 25
                                cachedExamDateMs        = prefs.examDate ?: 0L
                                cachedPlanStartMs       = prefs.planStartDate ?: 0L
                                cachedExamSubjects      = prefs.examSubjects.map { it.name }
                            }
                            StudyMode.FOCUS -> {
                                cachedDailyStudyHours      = prefs.focusPreferences.dailyStudyHours ?: 0.5f
                                cachedSessionLengthMinutes = prefs.focusPreferences.sessionLength.minutes ?: 25
                                cachedFocusSubjects = prefs.focusSubjects.map { it.name }
                                // TODO: Add skipSundays field to FocusPreferences data class
                                cachedSkipSundaysInFocus = false // prefs.focusPreferences.skipSundays ?: false
                            }
                            StudyMode.CASUAL -> {
                                cachedDailyStudyHours      = prefs.casualPreferences.dailyStudyHours ?: 0.5f
                                cachedSessionLengthMinutes = prefs.casualPreferences.sessionLength.minutes ?: 25
                                cachedCasualSubjects = prefs.casualSubjects.map { it.name }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PlannerViewModel", "prefs load failed", e)
                }
                Unit
            }

            // 2. Live-collect sessions – every new completion triggers a full calendar rebuild
            sessionRepository.getSessionsForUser(userId).collectLatest { sessions ->
                allDbSessions = sessions
                rebuildCalendar()
            }
        }
    }

    // ─── public actions called by the UI ────────────────────────────────────────

    fun selectDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        rebuildDayDetail(date)
    }


    /** Weekly nav (focus / casual default view) */
    fun navigateWeek(forward: Boolean) {
        val current = _uiState.value.selectedDate
        val candidate = if (forward) current.plusWeeks(1) else current.minusWeeks(1)

        val examEnd = _uiState.value.examEndDate
        if (examEnd != null && candidate.isAfter(examEnd)) return

        _uiState.value = _uiState.value.copy(selectedDate = candidate, visibleMonth = candidate)
        rebuildCalendar()
    }

    // ─── core rebuild ───────────────────────────────────────────────────────────

    private fun rebuildCalendar() {
        val today       = LocalDate.now()
        val visibleMonth = _uiState.value.visibleMonth

        val examEndDate: LocalDate? = if (cachedMode == StudyMode.EXAM && cachedExamDateMs > 0L)
            epochMsToLocalDate(cachedExamDateMs) else null
        val planStartDate: LocalDate? = if (cachedMode == StudyMode.EXAM && cachedPlanStartMs > 0L)
            epochMsToLocalDate(cachedPlanStartMs) else null

        val sessionsPerDay = deriveSessions(cachedDailyStudyHours, cachedSessionLengthMinutes)

        // Group every DB row by calendar date
        val sessionsByDate = allDbSessions.groupBy { epochMsToLocalDate(it.timestamp) }

        // ── date range to render ──
        val rangeStart: LocalDate
        val rangeEnd  : LocalDate

        when (cachedMode) {
            StudyMode.EXAM -> {
                rangeStart = planStartDate ?: today
                // Planning ends one day BEFORE the exam date
                rangeEnd = if (examEndDate != null) examEndDate.minusDays(1) else today.plusMonths(1)
            }
            StudyMode.FOCUS, StudyMode.CASUAL -> {
                // Always a Mon–Sun week around the selected date
                rangeStart = _uiState.value.selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                rangeEnd   = _uiState.value.selectedDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            }
        }

        // ── Check if entire week is in future (Focus/Casual only) ──
        if (cachedMode == StudyMode.FOCUS || cachedMode == StudyMode.CASUAL) {
            if (rangeStart.isAfter(today)) {
                _uiState.value = _uiState.value.copy(
                    selectedMode = cachedMode,
                    calendarDates = emptyList(),
                    missedBacklog = 0,
                    weeklyPool = null,
                    examEndDate = null,
                    isLoading = false
                )
                _uiState.value = _uiState.value.copy(selectedDayDetail = null)
                return
            }
        }

        // ── iterate every date in range ──
        val calendarDates   = mutableListOf<CalendarDateInfo>()
        var cumulativeMissed = 0
        var date            = rangeStart

        while (!date.isAfter(rangeEnd)) {
            // Skip Sundays based on mode
            if (shouldSkipSunday(date)) {
                date = date.plusDays(1)
                continue
            }

            val isFuture  = date.isAfter(today)
            val isToday   = date.isEqual(today)
            val isExamDay = examEndDate != null && date.isEqual(examEndDate)

            val dbForDay      = sessionsByDate[date] ?: emptyList()
            val completedCount = dbForDay.count { it.completed }

            val status    : DayStatus
            val phaseLabel: String?

            when (cachedMode) {
                StudyMode.EXAM -> {
                    phaseLabel = computePhaseLabel(date, planStartDate, examEndDate)
                    status = when {
                        isFuture  -> DayStatus.PLANNED
                        else      -> {
                            val s = computeDayStatus(sessionsPerDay, completedCount)
                            if (s == DayStatus.MISSED || s == DayStatus.PARTIAL)
                                cumulativeMissed += (sessionsPerDay - completedCount).coerceAtLeast(0)
                            s
                        }
                    }
                }
                StudyMode.FOCUS -> {
                    phaseLabel = null
                    status = when {
                        isFuture -> DayStatus.PLANNED
                        isToday -> {
                            val s = computeDayStatus(sessionsPerDay, completedCount)
                            if (s == DayStatus.MISSED || s == DayStatus.PARTIAL)
                                cumulativeMissed += (sessionsPerDay - completedCount).coerceAtLeast(0)
                            s
                        }
                        else -> { // past days
                            if (completedCount > 0) {
                                val s = computeDayStatus(sessionsPerDay, completedCount)
                                if (s == DayStatus.MISSED || s == DayStatus.PARTIAL)
                                    cumulativeMissed += (sessionsPerDay - completedCount).coerceAtLeast(0)
                                s
                            } else {
                                DayStatus.EMPTY // past day with no DB data
                            }
                        }
                    }
                }
                StudyMode.CASUAL -> {
                    phaseLabel = null
                    // Casual never shows red
                    status = when {
                        isFuture         -> DayStatus.EMPTY
                        completedCount > 0 -> if (completedCount >= sessionsPerDay) DayStatus.COMPLETED else DayStatus.PARTIAL
                        else             -> DayStatus.EMPTY
                    }
                }
            }

            calendarDates.add(CalendarDateInfo(date, status, isToday, isExamDay, phaseLabel))
            date = date.plusDays(1)
        }

        // ── exam: compute redistribution of missed sessions into future days ──
        cachedRedistributionMap = if (cachedMode == StudyMode.EXAM && cumulativeMissed > 0 && examEndDate != null)
            computeRedistribution(cumulativeMissed, today, examEndDate)
        else emptyMap()

        // ── casual weekly pool ──
        val weeklyPool: WeeklyPoolInfo? = if (cachedMode == StudyMode.CASUAL) {
            val ws = _uiState.value.selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val we = _uiState.value.selectedDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            var done = 0
            var d = ws
            while (!d.isAfter(we)) {
                if (!shouldSkipSunday(d)) {
                    done += (sessionsByDate[d]?.count { it.completed } ?: 0)
                }
                d = d.plusDays(1)
            }
            // Casual mode: 6 days (excluding Sunday)
            val daysInWeek = 6
            WeeklyPoolInfo(ws, we, sessionsPerDay * daysInWeek, done)
        } else null

        _uiState.value = _uiState.value.copy(
            selectedMode  = cachedMode,
            calendarDates = calendarDates,
            missedBacklog = if (cachedMode == StudyMode.CASUAL) 0 else cumulativeMissed,
            weeklyPool    = weeklyPool,
            examEndDate   = examEndDate,
            isLoading     = false
        )

        // keep the detail panel in sync
        rebuildDayDetail(_uiState.value.selectedDate)
    }

    private fun rebuildDayDetail(date: LocalDate) {
        val today   = LocalDate.now()
        val isFuture = date.isAfter(today)
        val isToday  = date.isEqual(today)

        val sessionsPerDay = deriveSessions(cachedDailyStudyHours, cachedSessionLengthMinutes)
        val sessionsByDate = allDbSessions.groupBy { epochMsToLocalDate(it.timestamp) }
        val dbForDay       = sessionsByDate[date] ?: emptyList()

        val extraRedistributed = cachedRedistributionMap[date] ?: 0
        val totalPlanned       = sessionsPerDay + extraRedistributed

        // Greedily match completed DB rows to planned slots by subject name
        val completedNames = dbForDay.filter { it.completed }.map { it.subjectName }.toMutableList()

        // Don't generate planned sessions for past days without DB data
        // Don't generate planned sessions for:
// - Past days without DB data
// - Exam mode: on or after exam date
        val shouldGeneratePlanned = when {
            cachedMode == StudyMode.EXAM && cachedExamDateMs > 0L -> {
                val examDate = epochMsToLocalDate(cachedExamDateMs)
                date.isBefore(examDate) && (!date.isBefore(today) || dbForDay.isNotEmpty())
            }
            else -> !date.isBefore(today) || dbForDay.isNotEmpty()
        }

        val plannedSessions = if (shouldGeneratePlanned) {
            (1..totalPlanned).map { i ->
                val subjectName = when {
                    cachedMode == StudyMode.EXAM && cachedExamSubjects.isNotEmpty() ->
                        cachedExamSubjects[(i - 1) % cachedExamSubjects.size]
                    cachedMode == StudyMode.FOCUS && cachedFocusSubjects.isNotEmpty() ->
                        cachedFocusSubjects[(i - 1) % cachedFocusSubjects.size]
                    cachedMode == StudyMode.CASUAL && cachedCasualSubjects.isNotEmpty() ->
                        cachedCasualSubjects[(i - 1) % cachedCasualSubjects.size]
                    else -> "Session $i"
                }

                val wasCompleted = completedNames.remove(subjectName)  // removes first match only

                PlannedSessionInfo(i, subjectName, cachedSessionLengthMinutes, wasCompleted)
            }
        } else {
            emptyList()
        }

        val completedSessions = dbForDay.map {
            CompletedSessionInfo(it.subjectName, it.elapsedSeconds / 60, it.completed, it.timestamp)
        }.sortedBy { it.timestamp }

        _uiState.value = _uiState.value.copy(
            selectedDayDetail = DayDetail(
                date               = date,
                plannedSessions    = plannedSessions,
                completedSessions  = completedSessions,
                plannedCount       = if (shouldGeneratePlanned) totalPlanned else 0,
                completedCount     = dbForDay.count { it.completed },
                totalStudyMinutes  = dbForDay.sumOf { it.elapsedSeconds } / 60,
                isFutureDate       = isFuture,
                isToday            = isToday,
                redistributedExtra = extraRedistributed
            )
        )
    }

    // ─── pure helpers ────────────────────────────────────────────────────────

    /** Same formula as HomeViewModel.normalizeDailyInputs */
    private fun deriveSessions(dailyHours: Float, sessionMinutes: Int): Int {
        val h = if (dailyHours <= 0f) 0.5f else dailyHours
        val m = if (sessionMinutes < 1) 1 else sessionMinutes
        return floor(h.toDouble() * 60.0 / m.toDouble()).toInt().coerceAtLeast(1)
    }

    private fun shouldSkipSunday(date: LocalDate): Boolean {
        if (date.dayOfWeek != DayOfWeek.SUNDAY) return false
        return when (cachedMode) {
            StudyMode.CASUAL -> true
            StudyMode.FOCUS -> cachedSkipSundaysInFocus
            StudyMode.EXAM -> false
        }
    }

    private fun computeDayStatus(planned: Int, completed: Int): DayStatus = when {
        planned == 0        -> DayStatus.EMPTY
        completed >= planned -> DayStatus.COMPLETED
        completed > 0       -> DayStatus.PARTIAL
        else                -> DayStatus.MISSED
    }

    /** Same 0.65 / 0.90 thresholds as HomeViewModel.calculateStudyPhase */
    private fun computePhaseLabel(date: LocalDate, planStart: LocalDate?, examEnd: LocalDate?): String? {
        if (planStart == null || examEnd == null) return null
        val total   = ChronoUnit.DAYS.between(planStart, examEnd).coerceAtLeast(1)
        val elapsed = ChronoUnit.DAYS.between(planStart, date).coerceAtLeast(0)
        val progress = elapsed.toFloat() / total
        return when {
            progress < 0.65f  -> "Syllabus"
            progress <= 0.90f -> "Revision"
            else              -> "Testing"
        }
    }

    /**
     * Spread `missed` extra sessions evenly across [today+1, examDate).
     * Remainder slots go to the earliest days first.
     */
    private fun computeRedistribution(missed: Int, today: LocalDate, examDate: LocalDate): Map<LocalDate, Int> {
        val days = mutableListOf<LocalDate>()
        var d = today.plusDays(1)
        while (d.isBefore(examDate)) { days.add(d); d = d.plusDays(1) }
        if (days.isEmpty()) return emptyMap()

        val perDay    = missed / days.size
        val remainder = missed % days.size
        return days.mapIndexedNotNull { idx, futureDate ->
            val extra = perDay + if (idx < remainder) 1 else 0
            if (extra > 0) futureDate to extra else null
        }.toMap()
    }

    private fun epochMsToLocalDate(ms: Long): LocalDate =
        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
}

// ─── Factory (same shape as AnalyticsViewModelFactory) ──────────────────────
class PlannerViewModelFactory(
    private val sessionRepository: SessionRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlannerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlannerViewModel(sessionRepository, preferencesRepository, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}