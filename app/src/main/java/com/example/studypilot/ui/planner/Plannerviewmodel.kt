package com.example.studypilot.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.studypilot.data.SessionRepository
import com.example.studypilot.data.UserPreferencesRepository
import com.example.studypilot.ui.casual.CasualTask
import com.example.studypilot.ui.mode.StudyMode
import com.example.studypilot.ui.shared.Difficulty
import com.example.studypilot.ui.shared.FocusSubject
import com.example.studypilot.ui.shared.FocusTask
import com.example.studypilot.ui.shared.Priority
import com.example.studypilot.ui.shared.Subject
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


private var cachedExamSubjectsWithData: List<Subject> = emptyList()
private var cachedFocusSubjectsWithData: List<FocusSubject> = emptyList()
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

    private var cachedFocusTasks: List<FocusTask> = emptyList()
    private var cachedCasualTasks: List<CasualTask> = emptyList()

    // ── cached daily plans (saved session order from HomeViewModel) ──
    private var cachedDailyPlan: List<com.example.studypilot.ui.shared.StudySession> = emptyList()
    private var cachedDailyPlanDate: String = ""
    private var cachedFocusDailyPlan: List<com.example.studypilot.ui.shared.StudySession> = emptyList()
    private var cachedFocusPlanDate: String = ""
    private var cachedCasualDailyPlan: List<com.example.studypilot.ui.shared.StudySession> = emptyList()
    private var cachedCasualPlanDate: String = ""

    private var cachedExemptedSessions: Map<String, List<Int>> = emptyMap()

    // ── live snapshot of every DB row for this user ──
    private var allDbSessions: List<com.example.studypilot.data.StudySession> = emptyList()

    private var userStartDate: LocalDate? = null

    // ── redistribution map built during rebuildCalendar, consumed by rebuildDayDetail ──
    private var cachedRedistributionMap: Map<LocalDate, Int> = emptyMap()

    init {
        viewModelScope.launch {
            // Launch sessions collector in parallel
            launch {
                sessionRepository.getSessionsForUser(userId).collectLatest { sessions ->
                    allDbSessions = sessions

                    // Calculate user's start date (earliest session timestamp OR today if no sessions yet)
                    userStartDate = if (sessions.isNotEmpty()) {
                        sessions.minOfOrNull { epochMsToLocalDate(it.timestamp) }
                    } else {
                        LocalDate.now() // User just signed up today
                    }
                    android.util.Log.d("PlannerViewModel", "User start date: $userStartDate")

                    rebuildCalendar()
                }
            }

            // Collect preferences changes continuously
            preferencesRepository.getUserPreferences(userId).collectLatest { prefs ->
                if (prefs == null) return@collectLatest

                android.util.Log.d("PlannerViewModel", "Preferences changed - reloading")

                // Update cached preferences
                cachedMode = prefs.selectedMode
                when (prefs.selectedMode) {
                    StudyMode.EXAM -> {
                        cachedDailyStudyHours   = prefs.examPreferences.dailyStudyHours ?: 0.5f
                        cachedSessionLengthMinutes = prefs.examPreferences.sessionLength.minutes ?: 25
                        cachedExamDateMs        = prefs.examDate ?: 0L
                        cachedPlanStartMs       = prefs.planStartDate ?: 0L
                        cachedExamSubjects      = prefs.examSubjects.map { it.name }
                        cachedExamSubjectsWithData = prefs.examSubjects
                        cachedDailyPlan = prefs.dailyPlan
                        cachedDailyPlanDate = prefs.dailyPlanDate ?: ""
                    }
                    StudyMode.FOCUS -> {
                        cachedDailyStudyHours      = prefs.focusPreferences.dailyStudyHours ?: 0.5f
                        cachedSessionLengthMinutes = prefs.focusPreferences.sessionLength.minutes ?: 25
                        cachedFocusSubjects = prefs.focusSubjects.map { it.name }
                        cachedFocusSubjectsWithData = prefs.focusSubjects
                        cachedFocusTasks = prefs.tasks
                        cachedSkipSundaysInFocus = false
                        cachedFocusDailyPlan = prefs.focusDailyPlan
                        cachedFocusPlanDate = prefs.focusPlanDate ?: ""
                    }
                    StudyMode.CASUAL -> {
                        cachedDailyStudyHours      = prefs.casualPreferences.dailyStudyHours ?: 0.5f
                        cachedSessionLengthMinutes = prefs.casualPreferences.sessionLength.minutes ?: 25
                        cachedCasualSubjects = prefs.casualSubjects.map { it.name }
                        cachedCasualTasks = prefs.casualTasks
                        cachedCasualDailyPlan = prefs.casualDailyPlan
                        cachedCasualPlanDate = prefs.casualPlanDate ?: ""
                    }
                }

                cachedExemptedSessions = prefs.exemptedSessions
                android.util.Log.d("PlannerViewModel", "Reloaded prefs: mode=$cachedMode, dailyPlanDate=$cachedDailyPlanDate, subjects=${cachedExamSubjectsWithData.size}")

                val prefsChanged = when (cachedMode) {
                    StudyMode.EXAM -> cachedDailyPlanDate != (prefs.dailyPlanDate ?: "")
                    StudyMode.FOCUS -> cachedFocusPlanDate != (prefs.focusPlanDate ?: "")
                    StudyMode.CASUAL -> cachedCasualPlanDate != (prefs.casualPlanDate ?: "")
                }

                // Rebuild calendar with new preferences
                rebuildCalendar()
            }
        }
    }

    // ─── public actions called by the UI ────────────────────────────────────────

    fun selectDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        rebuildDayDetail(date)
    }

    fun exemptSession(date: LocalDate, sessionIndex: Int) {
        viewModelScope.launch {
            val dateStr = date.toString()

            android.util.Log.d("PlannerViewModel", "Exempting session $sessionIndex on $dateStr")

            withContext(Dispatchers.IO) {
                try {
                    // Load current preferences
                    val currentPrefs = preferencesRepository.getUserPreferences(userId).first()
                    if (currentPrefs == null) {
                        android.util.Log.e("PlannerViewModel", "Cannot exempt - prefs not loaded")
                        return@withContext
                    }

                    // Update exemptions map
                    val updatedExemptions = currentPrefs.exemptedSessions.toMutableMap()
                    val exemptionsForDate = updatedExemptions[dateStr]?.toMutableList() ?: mutableListOf()
                    if (sessionIndex !in exemptionsForDate) {
                        exemptionsForDate.add(sessionIndex)
                        updatedExemptions[dateStr] = exemptionsForDate.sorted()
                    }

                    // Update cached exemptions
                    cachedExemptedSessions = updatedExemptions

                    // If exempting today's session, update the saved daily plan
                    val today = java.time.LocalDate.now().toString()
                    val updatedPrefs = if (dateStr == today) {
                        val (currentPlan, planDateField) = when (cachedMode) {
                            StudyMode.EXAM -> currentPrefs.dailyPlan to currentPrefs.dailyPlanDate
                            StudyMode.FOCUS -> currentPrefs.focusDailyPlan to currentPrefs.focusPlanDate
                            StudyMode.CASUAL -> currentPrefs.casualDailyPlan to currentPrefs.casualPlanDate
                        }

                        // Only update if there's a saved plan for today
                        if (planDateField == today && currentPlan.isNotEmpty()) {
                            // Remove exempted session and renumber
                            val updatedPlan = currentPlan
                                .filterIndexed { index, _ -> index !in exemptionsForDate }
                                .mapIndexed { newIndex, session ->
                                    session.copy(sessionNumber = newIndex + 1)
                                }

                            android.util.Log.d("PlannerViewModel", "Updated daily plan: ${currentPlan.size} -> ${updatedPlan.size} sessions")

                            when (cachedMode) {
                                StudyMode.EXAM -> currentPrefs.copy(
                                    dailyPlan = updatedPlan,
                                    dailyPlanDate = today,
                                    exemptedSessions = updatedExemptions,

                                    )
                                StudyMode.FOCUS -> currentPrefs.copy(
                                    focusDailyPlan = updatedPlan,
                                    focusPlanDate = today,
                                    exemptedSessions = updatedExemptions,

                                    )
                                StudyMode.CASUAL -> currentPrefs.copy(
                                    casualDailyPlan = updatedPlan,
                                    casualPlanDate = today,
                                    exemptedSessions = updatedExemptions,

                                    )
                            }
                        } else {
                            // Just update exemptions
                            currentPrefs.copy(
                                exemptedSessions = updatedExemptions,

                                )
                        }
                    } else {
                        // Not today - just save exemptions
                        currentPrefs.copy(
                            exemptedSessions = updatedExemptions,

                            )
                    }

                    preferencesRepository.saveUserPreferences(updatedPrefs)
                    android.util.Log.d("PlannerViewModel", "Saved exemption and updated preferences")

                } catch (e: Exception) {
                    android.util.Log.e("PlannerViewModel", "Failed to exempt session", e)
                }
            }

            // Rebuild UI to reflect changes
            rebuildCalendar()
        }
    }

    fun doItToday(sessionIndex: Int, fromDate: LocalDate) {
        if (cachedMode != StudyMode.FOCUS) return

        viewModelScope.launch {
            val todayStr = LocalDate.now().toString()
            val today = LocalDate.now()

            withContext(Dispatchers.IO) {
                try {
                    val currentPrefs = preferencesRepository.getUserPreferences(userId).first()
                    if (currentPrefs == null) return@withContext

                    // Don't allow "Do It Today" on signup day
                    if (userStartDate != null && userStartDate == today) {
                        android.util.Log.d("PlannerViewModel", "Cannot move sessions on signup day")
                        return@withContext
                    }

                    // Get current today's plan
                    val currentPlan = if (currentPrefs.focusPlanDate == todayStr) {
                        currentPrefs.focusDailyPlan.toMutableList()
                    } else {
                        mutableListOf()
                    }

                    // Add ALL of yesterday's missed sessions to today
                    val missedFromYesterday = addYesterdaysMissedSessionsToToday()

                    android.util.Log.d("PlannerViewModel", "Adding ${missedFromYesterday.size} missed sessions from yesterday to today")

                    missedFromYesterday.forEach { missed ->
                        val newSession = com.example.studypilot.ui.shared.StudySession(
                            sessionNumber = currentPlan.size + 1,
                            subject = missed.subjectName,
                            durationMinutes = missed.durationMinutes,
                            status = com.example.studypilot.ui.shared.SessionStatus.UPCOMING
                        )
                        currentPlan.add(newSession)
                    }

                    // Renumber all sessions
                    val renumberedPlan = currentPlan.mapIndexed { index, session ->
                        session.copy(sessionNumber = index + 1)
                    }

                    // Save
                    val updatedPrefs = currentPrefs.copy(
                        focusDailyPlan = renumberedPlan,
                        focusPlanDate = todayStr,
                    )

                    preferencesRepository.saveUserPreferences(updatedPrefs)

                    android.util.Log.d("PlannerViewModel", "Added ${missedFromYesterday.size} sessions to today")

                } catch (e: Exception) {
                    android.util.Log.e("PlannerViewModel", "Failed to add to today", e)
                }
            }

            // Refresh
            rebuildCalendar()
        }
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
                val today = LocalDate.now()
                val signupDate = userStartDate ?: today

                // Calculate the week containing selectedDate
                val weekStart = _uiState.value.selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val weekEnd = _uiState.value.selectedDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

                // Don't show weeks before signup date
                if (weekEnd.isBefore(signupDate)) {
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

                rangeStart = if (weekStart.isBefore(signupDate)) {
                    signupDate // Start from signup date
                } else {
                    weekStart // Normal Monday start
                }

                // Always end on Saturday for Casual mode (Sunday is skipped)
                rangeEnd = if (cachedMode == StudyMode.CASUAL) {
                    weekStart.plusDays(5) // Monday + 5 = Saturday
                } else {
                    weekEnd // Sunday for Focus mode
                }

                android.util.Log.d("PlannerViewModel", "Week range: $rangeStart to $rangeEnd (userStart=$userStartDate, selectedDate=${_uiState.value.selectedDate})")
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

        // ── casual weekly pool ─
        val weeklyPool: WeeklyPoolInfo? = if (cachedMode == StudyMode.CASUAL) {
            // Calculate based on actual calendar dates shown (respects signup date automatically)
            var totalCompleted = 0
            var totalDaysInWeek = 0

            calendarDates.forEach { dateInfo ->
                val completedOnDay = (sessionsByDate[dateInfo.date]?.count { it.completed } ?: 0)
                totalCompleted += completedOnDay.coerceAtMost(sessionsPerDay)
                totalDaysInWeek++
            }

            // Total planned = sessionsPerDay × number of active days shown
            val totalPlanned = sessionsPerDay * totalDaysInWeek
            android.util.Log.d("PlannerViewModel", "Weekly pool: $totalCompleted/$totalPlanned across $totalDaysInWeek days (signup-aware)")

            WeeklyPoolInfo(rangeStart, rangeEnd, totalPlanned, totalCompleted)
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

        // Don't show planned sessions for dates before user signup
        if (userStartDate != null && date.isBefore(userStartDate!!)) {
            _uiState.value = _uiState.value.copy(
                selectedDayDetail = DayDetail(
                    date = date,
                    plannedSessions = emptyList(),
                    completedSessions = emptyList(),
                    plannedCount = 0,
                    completedCount = 0,
                    totalStudyMinutes = 0,
                    isFutureDate = isFuture,
                    isToday = isToday,
                    redistributedExtra = 0,
                    tasksForDay = emptyList()
                )
            )
            return
        }

        val sessionsPerDay = deriveSessions(cachedDailyStudyHours, cachedSessionLengthMinutes)
        val sessionsByDate = allDbSessions.groupBy { epochMsToLocalDate(it.timestamp) }
        val dbForDay       = sessionsByDate[date] ?: emptyList()

        val extraRedistributed = cachedRedistributionMap[date] ?: 0
        val totalPlanned       = sessionsPerDay + extraRedistributed

        val shouldGeneratePlanned = when(cachedMode) {
            StudyMode.EXAM -> {
                if (cachedExamDateMs <= 0L) {
                    false
                } else {
                    val examDate = epochMsToLocalDate(cachedExamDateMs)
                    val planStart =
                        if (cachedPlanStartMs > 0L)
                            epochMsToLocalDate(cachedPlanStartMs)
                        else
                            userStartDate ?: today

                    // Use same logic as Focus mode - respect user start date
                    val effectiveStart = if (userStartDate != null && planStart.isBefore(userStartDate!!)) {
                        userStartDate!!
                    } else {
                        planStart
                    }

                    !date.isBefore(effectiveStart) && date.isBefore(examDate)
                }
            }
            StudyMode.FOCUS -> {
                val weekStart = _uiState.value.selectedDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                val weekEnd = _uiState.value.selectedDate.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY))

                val effectiveStart = if (userStartDate != null && weekStart.isBefore(userStartDate!!)) {
                    userStartDate!!
                } else {
                    weekStart
                }

                !date.isBefore(effectiveStart) && !date.isAfter(weekEnd)
            }
            StudyMode.CASUAL -> {
                val weekStart = _uiState.value.selectedDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                val weekEnd = _uiState.value.selectedDate.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY))

                val effectiveStart = if (userStartDate != null && weekStart.isBefore(userStartDate!!)) {
                    userStartDate!!
                } else {
                    weekStart
                }

                !date.isBefore(effectiveStart) && !date.isAfter(weekEnd)
            }
        }

        val plannedSessions = if (shouldGeneratePlanned) {
            val dateStr = date.toString()
            val savedPlanForToday = when (cachedMode) {
                StudyMode.EXAM -> if (cachedDailyPlanDate == dateStr) {
                    // Validate that all subjects in saved plan exist in current subject list
                    val currentSubjects = cachedExamSubjectsWithData.map { it.name }.toSet()
                    val planSubjects = cachedDailyPlan.map { it.subject }.toSet()
                    val allValid = planSubjects.all { it in currentSubjects }

                    if (!allValid) {
                        android.util.Log.w("PlannerViewModel", "Saved plan has invalid subjects - regenerating. Plan subjects: $planSubjects, Current subjects: $currentSubjects")
                        null
                    } else {
                        cachedDailyPlan
                    }
                } else null
                StudyMode.FOCUS -> if (cachedFocusPlanDate == dateStr) cachedFocusDailyPlan else null
                StudyMode.CASUAL -> if (cachedCasualPlanDate == dateStr) cachedCasualDailyPlan else null
            }

            val basePlannedSessions = if (savedPlanForToday != null && savedPlanForToday.isNotEmpty()) {
                // Use saved plan order (includes swaps!)
                android.util.Log.d("PlannerViewModel", "Using saved plan for $dateStr with ${savedPlanForToday.size} sessions")
                android.util.Log.d("PlannerViewModel", "Saved plan subjects: ${savedPlanForToday.map { it.subject }}")

                val completedNames = dbForDay.filter { it.completed }.map { it.subjectName }.toMutableList()

                savedPlanForToday.mapIndexed { i, session ->
                    val wasCompleted = completedNames.remove(session.subject)
                    android.util.Log.d("PlannerViewModel", "Session $i: subject='${session.subject}'")
                    PlannedSessionInfo(
                        sessionNumber = i + 1,
                        subjectName = session.subject,
                        durationMinutes = cachedSessionLengthMinutes,
                        wasCompleted = wasCompleted,
                        isRedistributed = i >= sessionsPerDay  // Mark sessions beyond base count
                    )
                }
            } else {
                // Generate from scratch using SessionGenerator
                android.util.Log.d("PlannerViewModel", "No saved plan for $dateStr, generating fresh")

                val subjectOrder = when (cachedMode) {
                    StudyMode.EXAM -> {
                        if (cachedExamSubjectsWithData.isEmpty()) {
                            android.util.Log.w("PlannerViewModel", "No exam subjects loaded yet - using empty list")
                            emptyList()
                        } else {
                            // Always use subjects with data, just like HomeViewModel does
                            com.example.studypilot.utils.SessionGenerator.generateExamSessionOrder(
                                cachedExamSubjectsWithData, totalPlanned
                            )
                        }
                    }
                    StudyMode.FOCUS -> {
                        if (cachedFocusSubjectsWithData.isEmpty()) {
                            emptyList()
                        } else {
                            com.example.studypilot.utils.SessionGenerator.generateFocusSessionOrder(
                                cachedFocusSubjectsWithData, totalPlanned
                            )
                        }
                    }
                    StudyMode.CASUAL -> {
                        com.example.studypilot.utils.SessionGenerator.generateCasualSessionOrder(
                            cachedCasualSubjects, totalPlanned
                        )
                    }
                }

                val completedNames = dbForDay.filter { it.completed }.map { it.subjectName }.toMutableList()

                subjectOrder.mapIndexed { i, subjectName ->
                    val wasCompleted = completedNames.remove(subjectName)
                    PlannedSessionInfo(
                        i + 1,
                        subjectName,
                        cachedSessionLengthMinutes,
                        wasCompleted,
                        isRedistributed = i >= sessionsPerDay
                    )
                }
            }

            // Filter out exempted sessions
            val exemptedIndices = cachedExemptedSessions[dateStr] ?: emptyList()
            val filteredSessions = basePlannedSessions
                .filterIndexed { index, _ -> index !in exemptedIndices }
                .mapIndexed { newIndex, session ->
                    session.copy(sessionNumber = newIndex + 1)
                }

            // ADD MISSED SESSIONS FROM PAST (Focus mode only, today only)
            val finalSessions = filteredSessions


            android.util.Log.d("PlannerViewModel", "Day $dateStr: base=${sessionsPerDay}, redistributed=$extraRedistributed, exempted=${exemptedIndices.size}, final=${finalSessions.size}")

            finalSessions
        } else {
            emptyList()
        }

        val completedSessions = dbForDay.map {
            CompletedSessionInfo(it.subjectName, it.elapsedSeconds / 60, it.completed, it.timestamp)
        }.sortedBy { it.timestamp }

        // Filter tasks with due date matching this date
        val tasksForThisDay = mutableListOf<TaskInfo>()
        when (cachedMode) {
            StudyMode.FOCUS -> {
                cachedFocusTasks.forEach { task ->
                    task.dueDate?.let { dueMs ->
                        val taskDate = epochMsToLocalDate(dueMs)
                        if (taskDate.isEqual(date)) {
                            tasksForThisDay.add(
                                TaskInfo(
                                    taskName = task.name,
                                    relatedSubject = task.relatedSubject,
                                    dueDate = dueMs,
                                    taskType = "Focus",
                                    completed = task.completed
                                )
                            )
                        }
                    }
                }
            }
            StudyMode.CASUAL -> {
                cachedCasualTasks.forEach { task ->
                    task.dueDate?.let { dueMs ->
                        val taskDate = epochMsToLocalDate(dueMs)
                        if (taskDate.isEqual(date)) {
                            tasksForThisDay.add(
                                TaskInfo(
                                    taskName = task.name,
                                    relatedSubject = task.relatedSubject,
                                    dueDate = dueMs,
                                    taskType = "Casual",
                                    completed = task.completed
                                )
                            )
                        }
                    }
                }
            }
            else -> { /* Exam mode has no tasks */ }
        }

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
                redistributedExtra = extraRedistributed,
                tasksForDay        = tasksForThisDay
            )
        )

    }

    private fun calculateMissedSessionsForToday(): List<MissedSessionInfo> {
        return emptyList()
    }

    private fun addYesterdaysMissedSessionsToToday(): List<MissedSessionInfo> {
        if (cachedMode != StudyMode.FOCUS) return emptyList()

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        // Don't add if yesterday was signup day or before
        if (userStartDate != null && (yesterday.isBefore(userStartDate) || yesterday == userStartDate)) {
            android.util.Log.d("PlannerViewModel", "Yesterday was signup day or before - no missed sessions")
            return emptyList()
        }

        // Skip if yesterday was Sunday and user skips Sundays
        if (shouldSkipSunday(yesterday)) {
            android.util.Log.d("PlannerViewModel", "Yesterday was Sunday - skipped")
            return emptyList()
        }

        val missedSessions = mutableListOf<MissedSessionInfo>()
        val sessionsByDate = allDbSessions.groupBy { epochMsToLocalDate(it.timestamp) }
        val sessionsPerDay = deriveSessions(cachedDailyStudyHours, cachedSessionLengthMinutes)

        val yesterdaySessions = sessionsByDate[yesterday] ?: emptyList()
        val completedCount = yesterdaySessions.count { it.completed }
        val missedCount = (sessionsPerDay - completedCount).coerceAtLeast(0)

        if (missedCount > 0 && cachedFocusSubjectsWithData.isNotEmpty()) {
            // Figure out which subjects were missed yesterday
            val completedSubjects = yesterdaySessions.filter { it.completed }.map { it.subjectName }
            val availableSubjects = cachedFocusSubjectsWithData.map { it.name }

            // Find subjects that weren't completed enough times
            val missedSubjects = availableSubjects.filter { subject ->
                val expectedCount = (sessionsPerDay / availableSubjects.size) + 1
                val actualCount = completedSubjects.count { it == subject }
                actualCount < expectedCount
            }

            repeat(missedCount) { index ->
                val subjectName = if (missedSubjects.isNotEmpty()) {
                    missedSubjects[index % missedSubjects.size]
                } else {
                    availableSubjects.getOrNull(index % availableSubjects.size) ?: "General Study"
                }

                missedSessions.add(
                    MissedSessionInfo(
                        date = yesterday, // These are from yesterday
                        subjectName = subjectName,
                        durationMinutes = cachedSessionLengthMinutes
                    )
                )
            }
        }

        android.util.Log.d("PlannerViewModel", "Added ${missedSessions.size} missed sessions from yesterday")
        return missedSessions
    }

    data class MissedSessionInfo(
        val date: LocalDate,
        val subjectName: String,
        val durationMinutes: Int
    )

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