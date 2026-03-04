package com.example.studypilot.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.studypilot.data.SessionRepository
import com.example.studypilot.data.UserPreferences
import com.example.studypilot.data.UserPreferencesRepository
import com.example.studypilot.ui.auth.AuthViewModel
import com.example.studypilot.ui.auth.AuthState
import com.example.studypilot.ui.casual.CasualAlert
import com.example.studypilot.ui.casual.CasualDetails
import com.example.studypilot.ui.casual.CasualMetrics
import com.example.studypilot.ui.casual.CasualSession
import com.example.studypilot.ui.casual.CasualSessionStatus
import com.example.studypilot.ui.mode.StudyMode
import com.example.studypilot.ui.shared.Difficulty
import com.example.studypilot.ui.shared.FocusSubject
import com.example.studypilot.ui.shared.FocusTask
import com.example.studypilot.ui.shared.Priority
import com.example.studypilot.ui.shared.SessionStatus
import com.example.studypilot.ui.shared.StudySession
import com.example.studypilot.ui.shared.Subject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import com.example.studypilot.ui.casual.CasualTask
import com.example.studypilot.ui.casual.CasualAlertType
import com.example.studypilot.ui.mode.ExamModePreferences
import java.util.Objects
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// Main Home State
data class HomeState(
    val selectedMode: StudyMode = StudyMode.FOCUS,
    val userEmail: String? = null,
    // Exam Mode State
    val examDetails: ExamDetails? = null,
    val studySessions: List<StudySession>? = null,
    val originalStudySessions: List<StudySession>? = null,
    val accountabilityMetrics: AccountabilityMetrics? = null,
    val alerts: List<AlertMessage> = emptyList(),
    val studyStreak: Int? = null,
    val isSwapMode: Boolean = false,
    val swapSourceIndex: Int? = null,
    // Focus Mode State
    val focusDetails: FocusDetails? = null,
    val focusSessions: List<FocusSession>? = null,
    val originalFocusSessions: List<FocusSession>? = null,
    val focusMetrics: FocusMetrics? = null,
    val focusAlerts: List<FocusAlert> = emptyList(),
    val isFocusSwapMode: Boolean = false,
    val focusSwapSourceIndex: Int? = null,
    // Casual Mode State
    val casualDetails: CasualDetails? = null,
    val casualSessions: List<CasualSession>? = null,
    val originalCasualSessions: List<CasualSession>? = null,
    val casualMetrics: CasualMetrics? = null,
    val casualAlerts: List<CasualAlert> = emptyList(),
    val isCasualSwapMode: Boolean = false,
    val casualSwapSourceIndex: Int? = null,
    val isSubjectChangeMode: Boolean = false,
    val subjectChangeSessionIndex: Int? = null,
    // Weekly priority dialog
    val showWeeklyPriorityDialog: Boolean = false,
    val weeklyPriorityDialogMode: StudyMode? = null,
    val showExamOverDialog: Boolean = false,
    val examOverSubjectsSelected: List<String> = emptyList(),
    // True when exam date is today (0 days) or has passed — prompts user to set a new exam date
    val showNewExamDialog: Boolean = false
)

class HomeViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authViewModel: AuthViewModel,
    private val repository: SessionRepository

) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeState())
    val uiState = _uiState.asStateFlow()

    // Track last processed repository timestamp to avoid processing stale DAO emissions
    private var lastProcessedPrefsTimestamp: Long = 0L
    // Remember last session length used to generate the UI plan to detect preference changes
    private var lastUsedSessionLength: Int? = null
    // Track last database update to prevent processExamData from overwriting fresh DB updates
    private var lastDatabaseUpdateTime: Long = 0L

    private var lastSwapSaveTime: Long = 0L

    // 🚨 SIMPLE FIX: Force refresh from database
    fun refreshFromDatabase() {
        android.util.Log.d("HomeViewModel", "🔄 FORCE REFRESH called")
        viewModelScope.launch {
            val authState = authViewModel.authState.value
            if (authState is AuthState.Authenticated) {
                try {
                    // Get fresh data from database
                    val sessions = withContext(Dispatchers.IO) {
                        repository.getTodaySessionsForUser(authState.uid).first()
                    }
                    android.util.Log.d("HomeViewModel", "🔄 Force refresh got ${sessions.size} sessions from DB")
                    updateSessionStatusesFromDatabase(sessions)
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "Force refresh failed", e)
                }
            }
        }
    }


    // CRITICAL: Track if we're currently saving to prevent feedback loops


    init {
        android.util.Log.d("HomeViewModel", "🏗️ HomeViewModel CREATED - hashCode=${this.hashCode()}")
        viewModelScope.launch {
            authViewModel.authState.collectLatest { authState ->
                if (authState is AuthState.Authenticated) {
                    _uiState.value = _uiState.value.copy(userEmail = authState.email)

                    // Collect today's sessions from database and update UI
                    launch {
                        android.util.Log.d("HomeViewModel", "👂 Sessions Flow collector STARTED for user: ${authState.uid}")
                        repository.getTodaySessionsForUser(authState.uid)
                            .collectLatest { todaySessions ->
                                android.util.Log.d("HomeViewModel", "📊 Sessions Flow EMITTED: ${todaySessions.size} sessions")
                                updateSessionStatusesFromDatabase(todaySessions)
                            }
                    }

                    userPreferencesRepository.getUserPreferences(authState.uid)
                        .collectLatest { userPreferences: UserPreferences? ->
                            if (userPreferences == null) {
                                clearData()
                                return@collectLatest
                            }


                            // Debug: log incoming prefs from repository to detect overwrites/races
                            try {
                                val incomingSessionLen = userPreferences.examPreferences.sessionLength.minutes
                                val planDurations = userPreferences.dailyPlan.map { it.durationMinutes }
                                android.util.Log.d("HomeViewModel", "Collector received prefs: examSessionLength=$incomingSessionLen, dailyPlanDate=${userPreferences.dailyPlanDate}, dailyPlanDurations=$planDurations, lastAccessed=${userPreferences.lastAccessed}")
                            } catch (t: Throwable) {
                                android.util.Log.d("HomeViewModel", "Collector received prefs (partial): $userPreferences", t)
                            }

                            val incomingTs = userPreferences.lastAccessed
                            android.util.Log.d("HomeViewModel", "Processing prefs with lastAccessed=$incomingTs")

                            val uiCachedSubjects = _uiState.value.examDetails?.subjects
                            val looksLikeTransientEmptySubjects = userPreferences.examSubjects.isEmpty() && userPreferences.lastAccessed == 0L

                            if (looksLikeTransientEmptySubjects) {
                                if (!uiCachedSubjects.isNullOrEmpty()) {
                                    android.util.Log.d("HomeViewModel", "Detected transient empty examSubjects emission - reusing cached UI subjects to avoid flash/default override")
                                    processExamData(userPreferences, authState.uid, uiCachedSubjects)
                                    return@collectLatest
                                } else if (lastProcessedPrefsTimestamp == 0L) {
                                    android.util.Log.d("HomeViewModel", "Ignoring initial transient empty examSubjects emission until committed prefs arrive")
                                    return@collectLatest
                                }
                            }

                            when (userPreferences.selectedMode) {
                                StudyMode.EXAM -> processExamData(userPreferences, authState.uid)
                                StudyMode.FOCUS -> processFocusData(userPreferences, authState.uid)
                                StudyMode.CASUAL -> processCasualData(userPreferences, authState.uid)
                            }

                            val afterTs = userPreferences.lastAccessed
                            if (afterTs > lastProcessedPrefsTimestamp) lastProcessedPrefsTimestamp = afterTs
                        }
                } else {
                    clearData()
                }
            }
        }
    }

    private fun clearData() {
        _uiState.value = HomeState()
    }

    private fun updateSessionStatusesFromDatabase(completedSessions: List<com.example.studypilot.data.StudySession>) {


        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)

        // Filter sessions completed today
        val todayCompletedSessions = completedSessions.filter { session ->
            val sessionDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(session.timestamp)
            sessionDate == today && session.completed
        }

        android.util.Log.d("HomeViewModel", "🔄 Database sync: Updating UI with ${todayCompletedSessions.size} completed sessions from database")

        // Launch coroutine since updateExamSessionStatuses is now suspend
        viewModelScope.launch {
            when (_uiState.value.selectedMode) {
                StudyMode.EXAM -> updateExamSessionStatuses(todayCompletedSessions, completedSessions, today)
                StudyMode.FOCUS -> updateFocusSessionStatuses(todayCompletedSessions, today)
                StudyMode.CASUAL -> updateCasualSessionStatuses(todayCompletedSessions, today)
            }
        }
    }

    private suspend fun updateExamSessionStatuses(todayCompletedSessions: List<com.example.studypilot.data.StudySession>, allCompletedSessions: List<com.example.studypilot.data.StudySession>, today: String) {
        val sessions = _uiState.value.studySessions ?: return

        // Create a map of subject -> completion count
        val completionMap = todayCompletedSessions
            .groupBy { it.subjectName }
            .mapValues { it.value.size }

        android.util.Log.d("HomeViewModel", "Exam completion map: $completionMap")

        // Track how many times each subject has been marked as completed
        val subjectCompletionTracker = mutableMapOf<String, Int>()

        var foundCurrent = false
        val updatedSessions = sessions.map { session ->
            val subjectName = session.subject
            val timesCompleted = completionMap.getOrDefault(subjectName, 0)
            val currentCount = subjectCompletionTracker.getOrDefault(subjectName, 0)

            when {
                // Mark as completed if this occurrence of the subject has been completed
                currentCount < timesCompleted -> {
                    subjectCompletionTracker[subjectName] = currentCount + 1
                    session.copy(status = SessionStatus.COMPLETED)
                }
                // First non-completed session is CURRENT
                !foundCurrent -> {
                    foundCurrent = true
                    session.copy(status = SessionStatus.CURRENT)
                }
                // Rest are UPCOMING
                else -> session.copy(status = SessionStatus.UPCOMING)
            }
        }

        // Calculate metrics
        val completed = updatedSessions.count { it.status == SessionStatus.COMPLETED }
        val pending = updatedSessions.size - completed

        // Calculate streak
        val streak = calculateExamStreak(allCompletedSessions)

        val metrics = AccountabilityMetrics(
            completed = completed,
            missed = 0, // Don't count as missed until end of day
            backlog = pending
        )

        // Generate updated alerts
        val examDetails = _uiState.value.examDetails
        val alerts = if (examDetails != null) {
            // Fetch actual preferences to get planStartDate
            val authState = authViewModel.authState.value
            val actualPrefs = if (authState is AuthState.Authenticated) {
                try {
                    withContext(Dispatchers.IO) {
                        userPreferencesRepository.getUserPreferences(authState.uid).first()
                    }
                } catch (e: Exception) {
                    null
                }
            } else null

            if (actualPrefs != null) {
                generateAlerts(actualPrefs, examDetails, metrics)
            } else {
                emptyList()
            }
        } else emptyList()

        android.util.Log.d("HomeViewModel", "Updated exam sessions: completed=$completed, pending=$pending, streak=$streak")

        // Track that we just updated from database
        val statusesChanged = updatedSessions.map { it.status } != sessions.map { it.status }
        if (statusesChanged) {
            // Track that we just updated from database
            lastDatabaseUpdateTime = System.currentTimeMillis()
            android.util.Log.d("HomeViewModel", "Database update timestamp set - session statuses changed")
        }

        _uiState.value = _uiState.value.copy(
            studySessions = updatedSessions,
            accountabilityMetrics = metrics,
            studyStreak = streak,
            alerts = alerts
        )
    }

    private suspend fun updateFocusSessionStatuses(todayCompletedSessions: List<com.example.studypilot.data.StudySession>, today: String) {
        val sessions = _uiState.value.focusSessions ?: return

        val completionMap = todayCompletedSessions
            .groupBy { it.subjectName }
            .mapValues { it.value.size }

        val subjectCompletionTracker = mutableMapOf<String, Int>()

        var foundCurrent = false
        val updatedSessions = sessions.map { session ->
            val subjectName = session.subjectName
            val timesCompleted = completionMap.getOrDefault(subjectName, 0)
            val currentCount = subjectCompletionTracker.getOrDefault(subjectName, 0)

            when {
                currentCount < timesCompleted -> {
                    subjectCompletionTracker[subjectName] = currentCount + 1
                    session.copy(status = SessionStatus.COMPLETED)
                }
                !foundCurrent -> {
                    foundCurrent = true
                    session.copy(status = SessionStatus.CURRENT)
                }
                else -> session.copy(status = SessionStatus.UPCOMING)
            }
        }

        val completedCount = updatedSessions.count { it.status == SessionStatus.COMPLETED }
        val totalTime = todayCompletedSessions.sumOf { it.elapsedSeconds / 60 }

        val metrics = calculateFocusMetrics(updatedSessions)

        val alerts = generateFocusAlerts(metrics.focusStreak, metrics.pendingToday, updatedSessions.size)

        android.util.Log.d("HomeViewModel", "Updated focus sessions: completed=$completedCount, totalTime=${totalTime}min")

        // Track that we just updated from database
        lastDatabaseUpdateTime = System.currentTimeMillis()

        _uiState.value = _uiState.value.copy(
            focusSessions = updatedSessions,
            focusMetrics = metrics,
            focusAlerts = alerts
        )
    }

    private suspend fun updateCasualSessionStatuses(todayCompletedSessions: List<com.example.studypilot.data.StudySession>, today: String) {
        val sessions = _uiState.value.casualSessions ?: return

        val completionMap = todayCompletedSessions
            .groupBy { it.subjectName }
            .mapValues { it.value.size }

        val subjectCompletionTracker = mutableMapOf<String, Int>()

        var foundCurrent = false
        val updatedSessions = sessions.map { session ->
            val subjectName = session.subjectName
            val timesCompleted = completionMap.getOrDefault(subjectName, 0)
            val currentCount = subjectCompletionTracker.getOrDefault(subjectName, 0)

            when {
                currentCount < timesCompleted -> {
                    subjectCompletionTracker[subjectName] = currentCount + 1
                    session.copy(status = CasualSessionStatus.COMPLETED)
                }
                !foundCurrent -> {
                    foundCurrent = true
                    session.copy(status = CasualSessionStatus.CURRENT)
                }
                else -> session.copy(status = CasualSessionStatus.UPCOMING)
            }
        }

        val completed = updatedSessions.count { it.status == CasualSessionStatus.COMPLETED }
        val pending = updatedSessions.size - completed

        val authState = authViewModel.authState.value
        val allSessions = if (authState is AuthState.Authenticated) {
            withContext(Dispatchers.IO) {
                repository.getSessionsForUser(authState.uid).first()
            }
        } else emptyList()

        val streak = calculateExamStreak(allSessions)

        val metrics = CasualMetrics(
            completedToday = completed,
            pendingToday = pending,
            studyStreak = streak
        )

        // Track that we just updated from database
        lastDatabaseUpdateTime = System.currentTimeMillis()

        _uiState.value = _uiState.value.copy(
            casualSessions = updatedSessions,
            casualMetrics = metrics
        )
    }

    private fun calculateExamStreak(allSessions: List<com.example.studypilot.data.StudySession>): Int {
        if (allSessions.isEmpty()) return 0

        val today = java.time.LocalDate.now()

        val studyDates = allSessions
            .filter { it.completed }
            .map {
                java.time.Instant.ofEpochMilli(it.timestamp)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
            }
            .distinct()
            .sortedDescending()

        if (studyDates.isEmpty()) return 0

        var currentStreak = 0
        var checkDate = today

        if (studyDates.contains(today)) {
            currentStreak = 1
            checkDate = today.minusDays(1)
        } else if (studyDates.contains(today.minusDays(1))) {
            currentStreak = 1
            checkDate = today.minusDays(2)
        } else {
            return 0
        }

        while (studyDates.contains(checkDate)) {
            currentStreak++
            checkDate = checkDate.minusDays(1)
        }

        return currentStreak
    }


    private suspend fun calculateRedistributedExtrasForToday(
        userId: String,
        examDateMs: Long,
        planStartMs: Long,
        sessionsPerDay: Int,
        exemptedSessions: Map<String, List<Int>> = emptyMap()
    ): Int {
        if (examDateMs <= 0L) return 0

        val today = LocalDate.now()
        val examDate = Instant.ofEpochMilli(examDateMs).atZone(ZoneId.systemDefault()).toLocalDate()

        // Don't redistribute on or after exam date
        if (!today.isBefore(examDate)) return 0

        // Get all DB sessions to calculate cumulative missed
        val allDbSessions = withContext(Dispatchers.IO) {
            repository.getSessionsForUser(userId).first()
        }

        val sessionsByDate = allDbSessions.groupBy { session ->
            Instant.ofEpochMilli(session.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }

        // Calculate cumulative missed from past days
        var cumulativeMissed = 0
        val planStart = if (planStartMs > 0L) {
            Instant.ofEpochMilli(planStartMs).atZone(ZoneId.systemDefault()).toLocalDate()
        } else {
            today.minusMonths(1) // Fallback
        }

        var checkDate = planStart
        while (checkDate.isBefore(today)) {
            val dbForDay = sessionsByDate[checkDate] ?: emptyList()
            val completedCount = dbForDay.count { it.completed }
            // Sessions explicitly skipped by the user on that day should not count as missed
            val dateKey = checkDate.toString()
            val skippedCount = exemptedSessions[dateKey]?.size ?: 0
            val effectiveRequired = (sessionsPerDay - skippedCount).coerceAtLeast(0)

            if (completedCount < effectiveRequired) {
                cumulativeMissed += (effectiveRequired - completedCount)
            }

            checkDate = checkDate.plusDays(1)
        }

        if (cumulativeMissed <= 0) return 0

        // Calculate days remaining between today+1 and examDate
        val daysRemaining = mutableListOf<LocalDate>()
        var d = today.plusDays(1)
        while (d.isBefore(examDate)) {
            daysRemaining.add(d)
            d = d.plusDays(1)
        }

        if (daysRemaining.isEmpty()) {
            // No future days - all catch-up must happen today
            return cumulativeMissed
        }

        // Redistribute evenly (same algorithm as PlannerViewModel)
        val perDay = cumulativeMissed / (daysRemaining.size + 1) // +1 to include today
        val remainder = cumulativeMissed % (daysRemaining.size + 1)

        // Today gets base distribution + 1 if it's in the remainder
        val todayExtra = perDay + if (0 < remainder) 1 else 0

        android.util.Log.d("HomeViewModel", "Redistribution: missed=$cumulativeMissed, daysRemaining=${daysRemaining.size}, todayExtra=$todayExtra")

        return todayExtra
    }



    private val MAX_CONSECUTIVE = 2

    private fun limitConsecutiveSubjects(subjects: List<Subject>): List<Subject> {

        if (subjects.size <= MAX_CONSECUTIVE) return subjects

        val result = mutableListOf<Subject>()
        var consecutiveCount = 0
        var last: Subject? = null

        for (subject in subjects) {
            if (subject == last) {
                consecutiveCount++
                if (consecutiveCount >= MAX_CONSECUTIVE) {
                    val alternative = subjects.firstOrNull { it != subject }
                    if (alternative != null) {
                        result.add(alternative)
                        last = alternative
                        consecutiveCount = 1
                        continue
                    }
                }
            } else {
                consecutiveCount = 1
            }

            result.add(subject)
            last = subject
        }

        return result
    }

    // Shared normalizer for daily time/session inputs (single source of truth)
    private data class NormalizedInput(
        val dailyStudyHours: Double,    // preserved as fractional hours in double for calculations
        val sessionLengthMinutes: Int,  // >= 1
        val dailyMinutes: Double,       // dailyStudyHours * 60
        val totalSessions: Int          // >= 1, derived from dailyMinutes / sessionLength
    )

    private fun normalizeDailyInputs(rawDailyStudyHours: Float?, rawSessionLengthMinutes: Int?): NormalizedInput {
        // Raw inputs come from UI / preferences and must be used exactly as entered
        // Apply safe fallbacks here only in HomeViewModel (single place)

        val safeDailyHoursFloat = when {
            rawDailyStudyHours == null -> 0.5f
            rawDailyStudyHours <= 0f -> 0.5f
            else -> rawDailyStudyHours
        }

        val sessionLen = (rawSessionLengthMinutes ?: 1).coerceAtLeast(1)

        val dailyMinutesDouble = safeDailyHoursFloat.toDouble() * 60.0

        // Derive session count strictly from dailyMinutes ÷ sessionLen, floor, min 1
        val derivedSessions = floor(dailyMinutesDouble / sessionLen.toDouble()).toInt().coerceAtLeast(1)

        return NormalizedInput(
            dailyStudyHours = safeDailyHoursFloat.toDouble(),
            sessionLengthMinutes = sessionLen,
            dailyMinutes = dailyMinutesDouble,
            totalSessions = derivedSessions
        )
    }


    // region Exam Mode Logic
    private var lastProcessedPrefsHash: Int = 0

    private suspend fun processExamData(userPreferences: UserPreferences, userId: String, cachedSubjects: List<Subject>? = null) {
        // CRITICAL: If database just updated sessions (within last 2 seconds), skip this
        // to prevent overwriting fresh completion status

        val currentSubjects = _uiState.value.examDetails?.subjects
        val incomingSubjects = cachedSubjects ?: userPreferences.examSubjects
        val subjectsChanged = currentSubjects != incomingSubjects

        val timeSinceLastDbUpdate = System.currentTimeMillis() - lastDatabaseUpdateTime
        val timeSinceSwapSave = System.currentTimeMillis() - lastSwapSaveTime
        if (timeSinceLastDbUpdate < 2000 && _uiState.value.studySessions != null && !subjectsChanged) {
            android.util.Log.d("HomeViewModel", "Skipping processExamData - database updated ${timeSinceLastDbUpdate}ms ago and subjects unchanged")
            return
        }

        if (subjectsChanged) {
            android.util.Log.d("HomeViewModel", "Processing exam data - subjects changed from $currentSubjects to $incomingSubjects")
        }

        // Create a hash of the key preferences to detect actual changes
        _uiState.value = _uiState.value.copy(selectedMode = StudyMode.EXAM)

        val rawExamMillis = userPreferences.examDate ?: 0L
        // Defensive normalization: some callers may have saved seconds instead of milliseconds. If the value looks like seconds (< 1e12), convert to ms.
        val examMillis = when {
            rawExamMillis == 0L -> 0L
            rawExamMillis < 1_000_000_000_000L -> {
                val converted = rawExamMillis * 1000L
                android.util.Log.d("HomeViewModel", "Normalized examDate: detected seconds value $rawExamMillis converting to ms=$converted")
                converted
            }
            else -> rawExamMillis
        }

        val prefsHash = Objects.hash(
            userPreferences.examSubjects,
            userPreferences.examDate,
            userPreferences.examPreferences.dailyStudyHours,
            userPreferences.examPreferences.sessionLength
        )

        // Skip if preferences haven't actually changed
        if (prefsHash == lastProcessedPrefsHash && _uiState.value.examDetails != null) {
            android.util.Log.d("HomeViewModel", "Skipping processExamData - no changes detected")
            return
        }

        lastProcessedPrefsHash = prefsHash



        // Compute days remaining using start-of-day boundaries to avoid timezone/time-of-day off-by-one issues.
        val daysRemaining = if (examMillis <= 0L) {
            0L
        } else {
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val examStart = Calendar.getInstance().apply {
                timeInMillis = examMillis
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            TimeUnit.MILLISECONDS.toDays(examStart - todayStart).coerceAtLeast(0L)
        }
        val urgency = when {
            daysRemaining <= 7 -> Urgency.CRITICAL
            daysRemaining <= 15 -> Urgency.WARNING
            else -> Urgency.NORMAL
        }

        val phase = calculateStudyPhase(userPreferences.planStartDate, examMillis)

        // Load saved exam subjects directly. We sanitize here defensively (trim, remove blanks, dedupe) because
        // prefs can sometimes contain stale or duplicated entries from older saves.
        val rawSavedSubjects = cachedSubjects ?: userPreferences.examSubjects

        // Trim names, remove blanks, and deduplicate by case-insensitive name while preserving order
        val sanitizedSubjects = rawSavedSubjects
            .map { it.copy(name = it.name.trim()) }
            .filter { it.name.isNotBlank() }
            .fold(mutableListOf<Subject>()) { acc, subj ->
                val exists = acc.any { it.name.equals(subj.name, ignoreCase = true) }
                if (!exists) acc.add(subj)
                acc
            }

        if (sanitizedSubjects.size != rawSavedSubjects.size || rawSavedSubjects.any { it.name.trim() != it.name }) {
            android.util.Log.d("HomeViewModel", "Sanitized examSubjects: from=$rawSavedSubjects to=$sanitizedSubjects")
        }

        val safeSubjects = if (sanitizedSubjects.isEmpty()) listOf(Subject("General Study", Priority.Low, Difficulty.Medium)) else sanitizedSubjects

        // Debug log loaded subjects to help trace issues where subjects disappear
        android.util.Log.d("HomeViewModel", "Loaded examSubjects from prefs (sanitized): $safeSubjects")

        // Prefer explicit mode-selection prefs saved by the user (mode selection screen).
        // Use IO dispatcher for repository reads to avoid blocking the main thread.
        val savedModePrefs = withContext(Dispatchers.IO) {
            userPreferencesRepository.getStudyModePreferences(userId).firstOrNull()
        }
        val defaultExamPrefs = ExamModePreferences()
        val examPrefs = when {
            savedModePrefs?.examPreferences != null && savedModePrefs.examPreferences != defaultExamPrefs -> savedModePrefs.examPreferences
            userPreferences.examPreferences != defaultExamPrefs -> userPreferences.examPreferences
            else -> savedModePrefs?.examPreferences ?: userPreferences.examPreferences
        }

        android.util.Log.d(
            "HomeViewModel",
            "Exam prefs loaded: dailyStudyHours=${examPrefs.dailyStudyHours}, sessionLengthMinutes=${examPrefs.sessionLength.minutes}, break=${examPrefs.breakPreference}, notification=${examPrefs.notificationPreference}"
        )

        // Build ExamDetails (used for alerts and UI). Ensure it's available before session generation.
        val examDetails = ExamDetails(
            examName = userPreferences.examName ?: "No Exam Set",
            examDate = examMillis,
            subjects = safeSubjects,
            daysRemaining = daysRemaining,
            urgency = urgency,
            phase = phase
        )

        // Today's date string for comparing saved daily plan
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)

        val normalized = normalizeDailyInputs(
            rawDailyStudyHours = examPrefs.dailyStudyHours,
            rawSessionLengthMinutes = examPrefs.sessionLength.minutes
        )

        val redistributedExtras = withContext(Dispatchers.IO) {
            calculateRedistributedExtrasForToday(
                userId = userId,
                examDateMs = examMillis,
                planStartMs = userPreferences.planStartDate ?: 0L,
                sessionsPerDay = normalized.totalSessions,
                exemptedSessions = userPreferences.exemptedSessions
            )
        }

        val totalSessionsWithCatchup = normalized.totalSessions + redistributedExtras

        android.util.Log.d(
            "HomeViewModel",
            "Normalized exam inputs: dailyMinutes=${normalized.dailyMinutes}, sessionLength=${normalized.sessionLengthMinutes}, totalSessions=${normalized.totalSessions}"
        )

        // CRITICAL FIX: Always load from database first to get completion status
        val dbSessionsToday = withContext(Dispatchers.IO) {
            repository.getTodaySessionsForUser(userId).firstOrNull()
                ?.filter { it.modeName == "EXAM" } ?: emptyList()
        }

        // Build completion map from database
        val completionMap = dbSessionsToday
            .groupBy { it.subjectName }
            .mapValues { it.value.size }

        var shouldPersistPlan = false
        val sessions = if (userPreferences.dailyPlanDate == today && userPreferences.dailyPlan.isNotEmpty()) {
            // Trim and normalize saved sessions defensively
            val sanitizedSaved = userPreferences.dailyPlan.map { s ->
                s.copy(subject = s.subject.trim(), durationMinutes = max(1, s.durationMinutes))
            }

            val validSubjectNames = safeSubjects.map { it.name }.toSet()

            // Validate: Saved session subjects must be non-blank and exist in savedSubjects
            val allSubjectsValid = sanitizedSaved.all { it.subject.isNotBlank() && it.subject in validSubjectNames }
            // Accept the full original plan OR a plan already shrunk by old skip code (size minus exemptions).
            val exemptedTodayCount = userPreferences.exemptedSessions[today]?.size ?: 0
            val matchingCounts = sanitizedSaved.size == totalSessionsWithCatchup ||
                    sanitizedSaved.size == (totalSessionsWithCatchup - exemptedTodayCount).coerceAtLeast(0)

// CRITICAL: Reject plans with "General Study" when we have real subjects
            val hasGeneralStudy = sanitizedSaved.any { it.subject.trim().equals("General Study", ignoreCase = true) }
            val hasRealSubjects = safeSubjects.any { !it.name.trim().equals("General Study", ignoreCase = true) }
            val isPlanStale = hasGeneralStudy && hasRealSubjects

            if (allSubjectsValid && matchingCounts && !isPlanStale) {
                // CRITICAL FIX: Apply database completion status to saved plan
                val subjectCompletionTracker = mutableMapOf<String, Int>()
                var foundCurrent = false

                val updatedSessions = sanitizedSaved.map { s ->
                    val subjectName = s.subject
                    val timesCompleted = completionMap.getOrDefault(subjectName, 0)
                    val currentCount = subjectCompletionTracker.getOrDefault(subjectName, 0)

                    val status = when {
                        currentCount < timesCompleted -> {
                            subjectCompletionTracker[subjectName] = currentCount + 1
                            SessionStatus.COMPLETED
                        }
                        !foundCurrent -> {
                            foundCurrent = true
                            SessionStatus.CURRENT
                        }
                        else -> SessionStatus.UPCOMING
                    }

                    s.copy(
                        durationMinutes = normalized.sessionLengthMinutes,
                        status = status
                    )
                }

                // Only persist if durations changed (not if just status synced from DB)
                val needsDurationUpdate = sanitizedSaved.any { it.durationMinutes != normalized.sessionLengthMinutes }
                if (needsDurationUpdate) {
                    shouldPersistPlan = true
                    android.util.Log.d("HomeViewModel", "Updating saved plan with new session length: ${normalized.sessionLengthMinutes}")
                } else {
                    android.util.Log.d("HomeViewModel", "Accepting saved daily plan with DB sync - NO PERSIST")
                }

                updatedSessions
            } else {
                // saved plan is stale/invalid — regenerate from current saved subjects and persist the regenerated plan
                android.util.Log.d("HomeViewModel", "Rejecting saved daily plan: allSubjectsValid=$allSubjectsValid, matchingCounts=$matchingCounts; regenerating plan")
                shouldPersistPlan = true
                withContext(Dispatchers.Default) {
                    generateStudySessions(safeSubjects, totalSessionsWithCatchup, normalized.sessionLengthMinutes, normalized.totalSessions, completionMap)
                }
            }
        } else {
            // No saved plan for today — generate fresh from saved subjects
            shouldPersistPlan = true
            val gen = withContext(Dispatchers.Default) {
                generateStudySessions(safeSubjects, totalSessionsWithCatchup, normalized.sessionLengthMinutes, baseSessions = normalized.totalSessions, completionMap = completionMap)
            }
            android.util.Log.d("HomeViewModel", "Generated new daily plan durations=${gen.map { it.durationMinutes }}")
            gen
        }


        android.util.Log.d("HomeViewModel", "Final sessions to UI durations=${sessions.map { it.durationMinutes }} subjects=${sessions.map { it.subject }}")
        // Apply exempted sessions for today so skipped sessions don't reappear on Home Screen
        val exemptedToday = userPreferences.exemptedSessions[today] ?: emptyList()
        val finalExamSessions = if (exemptedToday.isNotEmpty()) {
            sessions
                .filterIndexed { index, _ -> index !in exemptedToday }
                .mapIndexed { i, s -> s.copy(sessionNumber = i + 1) }
        } else {
            sessions
        }

        val finalCompleted = finalExamSessions.count { it.status == SessionStatus.COMPLETED }
        val finalPending = finalExamSessions.size - finalCompleted
        val finalMetrics = AccountabilityMetrics(completed = finalCompleted, missed = finalPending, backlog = finalPending)
        val finalAlerts = generateAlerts(userPreferences, examDetails, finalMetrics)

        _uiState.value = _uiState.value.copy(
            examDetails = examDetails,
            studySessions = finalExamSessions,
            alerts = finalAlerts,
            accountabilityMetrics = finalMetrics,
            studyStreak = 0
        )

        checkAndShowExamOverDialog(userPreferences, examDetails)

        // CRITICAL: Only persist when we created a new plan or updated session length
        if (shouldPersistPlan) {
            saveDailyPlan(sessions)
            android.util.Log.d("HomeViewModel", "Persisted exam plan to preferences only")
        }

        // Record the session length used so future emissions with stale durations are rejected
        lastUsedSessionLength = normalized.sessionLengthMinutes
    }

    private fun calculateStudyPhase(startDate: Long?, examDate: Long?): StudyPhase {
        if (startDate == null || examDate == null) return StudyPhase.SyllabusCompletion

        val totalDays = TimeUnit.MILLISECONDS.toDays(examDate - startDate).coerceAtLeast(1)
        val elapsedDays = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - startDate).coerceAtLeast(0)

        val progress = if (totalDays > 0) elapsedDays.toFloat() / totalDays else 1f

        return when {
            progress < 0.65f -> StudyPhase.SyllabusCompletion
            progress <= 0.90f -> StudyPhase.Revision
            else -> StudyPhase.Testing
        }
    }

    // Add generateAlerts to produce exam-related alerts (missed sessions, phase warnings)
    private fun generateAlerts(
        userPreferences: UserPreferences,
        examDetails: ExamDetails,
        metrics: AccountabilityMetrics
    ): List<AlertMessage> {
        val alerts = mutableListOf<AlertMessage>()

        if (metrics.missed > 0) {
            val sessionText = if (metrics.missed == 1) "session" else "sessions"
            alerts.add(AlertMessage("You are ${metrics.missed} $sessionText behind schedule", AlertType.WARNING))
        }

        val startDate = userPreferences.planStartDate
        val examDate = examDetails.examDate
        // examDate is non-null (Long), but may be 0 when not set — treat 0 as missing
        if (startDate != null && examDate > 0L) {
            val totalDays = TimeUnit.MILLISECONDS.toDays(examDate - startDate).coerceAtLeast(1)
            val elapsedDays = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - startDate).coerceAtLeast(0)

            val revisionStartDay = (totalDays * 0.65).toLong()
            val testingStartDay = (totalDays * 0.90).toLong()

            val daysToRevision = revisionStartDay - elapsedDays
            val daysToTesting = testingStartDay - elapsedDays

            if (daysToRevision in 1..3 && examDetails.phase == StudyPhase.SyllabusCompletion) {
                val dayText = if (daysToRevision == 1L) "day" else "days"
                alerts.add(AlertMessage("Revision phase starts in $daysToRevision $dayText", AlertType.MOTIVATIONAL))
            } else if (daysToTesting in 1..3 && examDetails.phase == StudyPhase.Revision) {
                val dayText = if (daysToTesting == 1L) "day" else "days"
                alerts.add(AlertMessage("Testing phase starts in $daysToTesting $dayText", AlertType.MOTIVATIONAL))
            }
        }

        return alerts
    }

    private fun generateStudySessions(
        subjects: List<Subject>,
        totalSessions: Int,
        sessionLength: Int,
        baseSessions: Int = totalSessions,
        completionMap: Map<String, Int> = emptyMap()
    ): List<StudySession> {
        // Use shared generator
        val subjectOrder = com.example.studypilot.utils.SessionGenerator.generateExamSessionOrder(
            subjects, totalSessions
        )

        // Track completions to assign correct status
        val subjectCompletionTracker = mutableMapOf<String, Int>()
        var foundCurrent = false

        return subjectOrder.mapIndexed { index, subjectName ->
            val timesCompleted = completionMap.getOrDefault(subjectName, 0)
            val currentCount = subjectCompletionTracker.getOrDefault(subjectName, 0)

            val status = when {
                currentCount < timesCompleted -> {
                    subjectCompletionTracker[subjectName] = currentCount + 1
                    SessionStatus.COMPLETED
                }
                !foundCurrent -> {
                    foundCurrent = true
                    SessionStatus.CURRENT
                }
                else -> SessionStatus.UPCOMING
            }

            StudySession(
                sessionNumber = index + 1,
                subject = subjectName,
                durationMinutes = sessionLength,
                status = status,
                isRedistributed = index >= baseSessions  // Mark catch-up sessions
            )
        }
    }



    private fun saveDailyPlan(sessions: List<StudySession>) {
        viewModelScope.launch {
            val authState = authViewModel.authState.first()
            if (authState is AuthState.Authenticated) {
                withContext(Dispatchers.IO) {
                    val currentPrefs = userPreferencesRepository.getUserPreferences(authState.uid).first()
                    if (currentPrefs != null) {
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)

                        // Check if session ORDER changed (for swaps)
                        val sessionOrderChanged = currentPrefs.dailyPlan.map { it.subject } != sessions.map { it.subject }

                        // CRITICAL FIX: Save if date changed, size changed, OR order changed (swaps)
                        if (currentPrefs.dailyPlanDate != today ||
                            currentPrefs.dailyPlan.size != sessions.size ||
                            sessionOrderChanged) {
                            val updatedPrefs = currentPrefs.copy(
                                dailyPlan = sessions,
                                dailyPlanDate = today
                            )
                            userPreferencesRepository.saveUserPreferences(updatedPrefs)
                            android.util.Log.d("HomeViewModel", "Saved dailyPlan with ${sessions.size} sessions (orderChanged=$sessionOrderChanged)")
                        } else {
                            android.util.Log.d("HomeViewModel", "Skipped saving exam plan - no changes detected")
                        }
                    }
                }
            }
        }
    }
    // endregion


    private fun saveFocusDailyPlan(sessions: List<FocusSession>) {
        viewModelScope.launch {
            val authState = authViewModel.authState.first()
            if (authState is AuthState.Authenticated) {
                withContext(Dispatchers.IO) {
                    val currentPrefs = userPreferencesRepository.getUserPreferences(authState.uid).first()
                    if (currentPrefs != null) {
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)

                        val sessionData = sessions.map {
                            StudySession(
                                sessionNumber = it.sessionNumber,
                                subject = it.subjectName,
                                durationMinutes = it.duration,
                                status = it.status
                            )
                        }

                        // Check if session ORDER changed (for swaps)
                        val sessionOrderChanged = currentPrefs.focusDailyPlan.map { it.subject } != sessionData.map { it.subject }

                        // CRITICAL FIX: Save if date changed, size changed, OR order changed (swaps)
                        if (currentPrefs.focusPlanDate != today ||
                            currentPrefs.focusDailyPlan.size != sessions.size ||
                            sessionOrderChanged) {
                            val updatedPrefs = currentPrefs.copy(
                                focusDailyPlan = sessionData,
                                focusPlanDate = today
                            )
                            userPreferencesRepository.saveUserPreferences(updatedPrefs)
                            android.util.Log.d("HomeViewModel", "Saved focus dailyPlan (orderChanged=$sessionOrderChanged)")
                        } else {
                            android.util.Log.d("HomeViewModel", "Skipped saving focus plan - no changes detected")
                        }
                    }
                }
            }
        }
    }

    private fun saveCasualDailyPlan(sessions: List<CasualSession>) {
        viewModelScope.launch {
            val authState = authViewModel.authState.first()
            if (authState is AuthState.Authenticated) {
                withContext(Dispatchers.IO) {
                    val currentPrefs = userPreferencesRepository.getUserPreferences(authState.uid).first()
                    if (currentPrefs != null) {
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)

                        val sessionData = sessions.map { session ->
                            StudySession(
                                sessionNumber = session.sessionNumber,
                                subject = session.subjectName,
                                durationMinutes = session.duration,
                                status = when(session.status) {
                                    CasualSessionStatus.COMPLETED -> SessionStatus.COMPLETED
                                    CasualSessionStatus.CURRENT -> SessionStatus.CURRENT
                                    CasualSessionStatus.UPCOMING -> SessionStatus.UPCOMING
                                }
                            )
                        }

                        // Check if session ORDER changed (for swaps)
                        val sessionOrderChanged = currentPrefs.casualDailyPlan.map { it.subject } != sessionData.map { it.subject }

                        // CRITICAL FIX: Save if date changed, size changed, OR order changed (swaps)
                        if (currentPrefs.casualPlanDate != today ||
                            currentPrefs.casualDailyPlan.size != sessions.size ||
                            sessionOrderChanged) {
                            val updatedPrefs = currentPrefs.copy(
                                casualDailyPlan = sessionData,
                                casualPlanDate = today
                            )
                            userPreferencesRepository.saveUserPreferences(updatedPrefs)
                            android.util.Log.d("HomeViewModel", "Saved casual dailyPlan (orderChanged=$sessionOrderChanged)")
                        } else {
                            android.util.Log.d("HomeViewModel", "Skipped saving casual plan - no changes detected")
                        }
                    }
                }
            }
        }
    }

    // region Focus Mode Logic
    private suspend fun processFocusData(userPreferences: UserPreferences, userId: String) {
        // CRITICAL: If database just updated sessions (within last 2 seconds), skip this
        // to prevent overwriting fresh completion status
        val timeSinceLastDbUpdate = System.currentTimeMillis() - lastDatabaseUpdateTime
        val timeSinceSwapSave = System.currentTimeMillis() - lastSwapSaveTime
        val currentSubjects = _uiState.value.focusDetails?.subjects
        val incomingSubjects = userPreferences.focusSubjects
        val subjectsChanged = currentSubjects != incomingSubjects

        if (timeSinceLastDbUpdate < 2000 && _uiState.value.focusSessions != null && !subjectsChanged) {
            android.util.Log.d("HomeViewModel", "Skipping processFocusData - database updated ${timeSinceLastDbUpdate}ms ago and subjects unchanged")
            return
        }

        if (subjectsChanged) {
            android.util.Log.d("HomeViewModel", "Processing focus data - subjects changed")
        }

        android.util.Log.d("HomeViewModel", "Processing FOCUS mode with prefs=${userPreferences.focusPreferences}")
        _uiState.value = _uiState.value.copy(selectedMode = StudyMode.FOCUS)

        val focusDetails = FocusDetails(
            subjects = userPreferences.focusSubjects,
            tasks = userPreferences.tasks,
            planType = userPreferences.planType,
            excludeSunday = userPreferences.excludeSunday,
            dailyStudyHours = userPreferences.focusPreferences.dailyStudyHours,
            preferredSessionLength = max(1, userPreferences.focusPreferences.sessionLength.minutes),
            breakPreference = userPreferences.focusPreferences.breakPreference,
            notificationsEnabled = userPreferences.focusPreferences.notificationPreferences.isNotEmpty()
        )

        // FIRST: Check database for today's focus sessions
        val dbSessionsToday = withContext(Dispatchers.IO) {
            repository.getTodaySessionsForUser(userId).firstOrNull()?.filter { it.modeName == "FOCUS" } ?: emptyList()
        }

        // Build completion map from database
        val completionMap = dbSessionsToday
            .groupBy { it.subjectName }
            .mapValues { it.value.size }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        val normalized = normalizeDailyInputs(
            rawDailyStudyHours = focusDetails.dailyStudyHours,
            rawSessionLengthMinutes = focusDetails.preferredSessionLength
        )

        var shouldPersistPlan = false
        val sessions = if (userPreferences.focusSubjects.isNotEmpty()) {
            // Check for saved plan first
            if (userPreferences.focusPlanDate == today && userPreferences.focusDailyPlan.isNotEmpty()) {
                val sanitizedSaved = userPreferences.focusDailyPlan.map { s ->
                    s.copy(subject = s.subject.trim(), durationMinutes = max(1, s.durationMinutes))
                }

                val validSubjectNames = focusDetails.subjects.map { it.name }.toSet()
                val allSubjectsValid = sanitizedSaved.all { it.subject.isNotBlank() && it.subject in validSubjectNames }
                // Accept plans >= normalized.totalSessions — "Do It Today" legitimately
                // grows today's plan beyond the base session count.
                val matchingCounts = sanitizedSaved.size >= normalized.totalSessions

                if (allSubjectsValid && matchingCounts) {
                    // CRITICAL FIX: Apply database completion status to saved plan
                    val subjectCompletionTracker = mutableMapOf<String, Int>()
                    var foundCurrent = false

                    val updatedSessions = sanitizedSaved.mapIndexed { index, s ->
                        val subject = focusDetails.subjects.find { it.name == s.subject }
                        val difficultyWeights = mapOf(Difficulty.Hard to 3, Difficulty.Medium to 2, Difficulty.Easy to 1)
                        val priorityWeights = mapOf(Priority.High to 3, Priority.Medium to 2, Priority.Low to 1)
                        val cognitiveScore = (difficultyWeights[subject?.difficulty] ?: 1) *
                                (priorityWeights[subject?.priority] ?: 1)

                        // Determine status based on DB completions
                        val subjectName = s.subject
                        val timesCompleted = completionMap.getOrDefault(subjectName, 0)
                        val currentCount = subjectCompletionTracker.getOrDefault(subjectName, 0)

                        val status = when {
                            currentCount < timesCompleted -> {
                                subjectCompletionTracker[subjectName] = currentCount + 1
                                SessionStatus.COMPLETED
                            }
                            !foundCurrent -> {
                                foundCurrent = true
                                SessionStatus.CURRENT
                            }
                            else -> SessionStatus.UPCOMING
                        }

                        FocusSession(
                            sessionNumber = index + 1,
                            subjectName = s.subject,
                            duration = normalized.sessionLengthMinutes,
                            cognitiveLoadScore = cognitiveScore,
                            status = status
                        )
                    }

                    // CRITICAL: Only persist if session length actually changed
                    if (sanitizedSaved.any { it.durationMinutes != normalized.sessionLengthMinutes }) {
                        shouldPersistPlan = true
                        android.util.Log.d("HomeViewModel", "Updating focus plan with new session length")
                    } else {
                        // Don't persist - just use the loaded plan with updated statuses
                        android.util.Log.d("HomeViewModel", "Accepting saved focus plan with DB sync - NO PERSIST")
                    }

                    updatedSessions
                } else {
                    // Regenerate
                    shouldPersistPlan = true
                    android.util.Log.d("HomeViewModel", "Regenerating focus plan")
                    generateFocusSessions(focusDetails, completionMap, weeklyPriorities = getWeeklyPrioritiesForMode(StudyMode.FOCUS, userPreferences))
                }
            } else {
                // No saved plan — generate new
                shouldPersistPlan = true
                generateFocusSessions(focusDetails, completionMap, weeklyPriorities = getWeeklyPrioritiesForMode(StudyMode.FOCUS, userPreferences))
            }
        } else {
            // No subjects - clear plan only if there's actually a plan to clear
            if (userPreferences.focusDailyPlan.isNotEmpty()) {
                viewModelScope.launch {
                    val authState = authViewModel.authState.first()
                    if (authState is AuthState.Authenticated) {
                        withContext(Dispatchers.IO) {
                            val currentPrefs = userPreferencesRepository.getUserPreferences(authState.uid).first()
                            if (currentPrefs != null && currentPrefs.focusDailyPlan.isNotEmpty()) {
                                val updatedPrefs = currentPrefs.copy(
                                    focusDailyPlan = emptyList(),
                                    focusPlanDate = "",
                                    lastAccessed = System.currentTimeMillis()
                                )
                                userPreferencesRepository.saveUserPreferences(updatedPrefs)
                                android.util.Log.d("HomeViewModel", "Cleared stale focus daily plan")
                            }
                        }
                    }
                }
            }
            emptyList()
        }

        // CRITICAL: Only persist to DB if we're actually creating/updating a plan
        if (shouldPersistPlan && sessions.isNotEmpty()) {
            saveFocusDailyPlan(sessions)
            android.util.Log.d("HomeViewModel", "Persisted focus plan to preferences only")
        }

        val metrics = calculateFocusMetrics(sessions)
        val alerts = generateFocusAlerts(metrics.focusStreak, metrics.pendingToday, sessions.size)

        _uiState.value = _uiState.value.copy(
            focusDetails = focusDetails,
            focusSessions = sessions,
            focusMetrics = metrics,
            focusAlerts = alerts
        )

        checkAndShowWeeklyPriorityDialog(StudyMode.FOCUS, userPreferences)
    }

    private fun generateFocusSessions(focusDetails: FocusDetails, completionMap: Map<String, Int> = emptyMap(),  weeklyPriorities: List<String> = emptyList()): List<FocusSession> {
        val normalized = normalizeDailyInputs(
            rawDailyStudyHours = focusDetails.dailyStudyHours,
            rawSessionLengthMinutes = focusDetails.preferredSessionLength
        )

        val sessionLength = normalized.sessionLengthMinutes
        val totalSessions = normalized.totalSessions

        if (focusDetails.subjects.isEmpty()) return emptyList()

        val orderedSubjects = if (weeklyPriorities.isNotEmpty()) {
            val prioritized = weeklyPriorities.mapNotNull { name ->
                focusDetails.subjects.find { it.name == name }
            }
            val rest = focusDetails.subjects.filter { it.name !in weeklyPriorities }
            prioritized + rest
        } else {
            focusDetails.subjects
        }

        // Use shared generator
        val subjectOrder = com.example.studypilot.utils.SessionGenerator.generateFocusSessionOrder(
            orderedSubjects, totalSessions
        )

        // Track completions to assign correct status
        val subjectCompletionTracker = mutableMapOf<String, Int>()
        var foundCurrent = false

        return subjectOrder.mapIndexed { index, subjectName ->
            val subject = focusDetails.subjects.find { it.name == subjectName }
            val difficultyWeights = mapOf(Difficulty.Hard to 3, Difficulty.Medium to 2, Difficulty.Easy to 1)
            val priorityWeights = mapOf(Priority.High to 3, Priority.Medium to 2, Priority.Low to 1)
            val cognitiveScore = (difficultyWeights[subject?.difficulty] ?: 1) *
                    (priorityWeights[subject?.priority] ?: 1)

            val timesCompleted = completionMap.getOrDefault(subjectName, 0)
            val currentCount = subjectCompletionTracker.getOrDefault(subjectName, 0)

            val status = when {
                currentCount < timesCompleted -> {
                    subjectCompletionTracker[subjectName] = currentCount + 1
                    SessionStatus.COMPLETED
                }
                !foundCurrent -> {
                    foundCurrent = true
                    SessionStatus.CURRENT
                }
                else -> SessionStatus.UPCOMING
            }

            FocusSession(
                sessionNumber = index + 1,
                subjectName = subjectName,
                duration = sessionLength,
                cognitiveLoadScore = cognitiveScore,
                status = status
            )
        }
    }

    private suspend fun calculateFocusMetrics(sessions: List<FocusSession>): FocusMetrics {
        val completedCount = sessions.count { it.status == SessionStatus.COMPLETED }
        val pendingCount = sessions.size - completedCount

        // Get ALL sessions from DB to calculate proper streak
        val authState = authViewModel.authState.value
        val allSessions = if (authState is AuthState.Authenticated) {
            withContext(Dispatchers.IO) {
                repository.getSessionsForUser(authState.uid).first()
            }
        } else emptyList()

        val streak = calculateExamStreak(allSessions) // Reuse the same logic

        return FocusMetrics(completedToday = completedCount, pendingToday = pendingCount, focusStreak = streak)
    }

    private fun generateFocusAlerts(streak: Int, pendingCount: Int, totalSessions: Int): List<FocusAlert> {
        val alerts = mutableListOf<FocusAlert>()
        if (streak > 2) {
            alerts.add(FocusAlert("Great start — momentum matters today", FocusAlertType.MOTIVATIONAL))
        }
        if (pendingCount > 0 && streak == 0) {
            alerts.add(FocusAlert("Try completing one short session to keep streak alive", FocusAlertType.AWARENESS))
        }
        if (totalSessions <= 2 && totalSessions > 0) { // Light day
            alerts.add(FocusAlert("Light day detected — perfect for revision", FocusAlertType.AWARENESS))
        }
        return alerts
    }

    // Update swap validation to NOT fail closed when data is missing
    private fun isFocusSwapValid(sessions: List<FocusSession>): Boolean {
        val focusDetails = _uiState.value.focusDetails
        if (focusDetails == null) return true // fail open — allow swap when validation data missing

        val subjectMap = focusDetails.subjects.associateBy { it.name }

        var consecutiveHardCount = 0
        for (session in sessions) {
            if (subjectMap[session.subjectName]?.difficulty == Difficulty.Hard) {
                consecutiveHardCount++
                if (consecutiveHardCount > 2) return false
            } else consecutiveHardCount = 0
        }

        if (sessions.isNotEmpty()) {
            if (subjectMap[sessions.first().subjectName]?.difficulty == Difficulty.Hard) return false
            if (subjectMap[sessions.last().subjectName]?.difficulty == Difficulty.Hard) return false
        }

        return true
    }
    // endregion

    // region Casual Mode Logic
    private fun processCasualData(userPreferences: UserPreferences, userId: String) {
        viewModelScope.launch {
            // CRITICAL: If database just updated sessions (within last 2 seconds), skip this
            // to prevent overwriting fresh completion status
            val timeSinceLastDbUpdate = System.currentTimeMillis() - lastDatabaseUpdateTime
            val currentSubjects = _uiState.value.casualDetails?.tasks?.map { it.name }
            val incomingSubjects = userPreferences.casualSubjects.map { it.name }
            val subjectsChanged = currentSubjects != incomingSubjects

            if (timeSinceLastDbUpdate < 2000 && _uiState.value.casualSessions != null && !subjectsChanged) {
                android.util.Log.d("HomeViewModel", "Skipping processCasualData - database updated ${timeSinceLastDbUpdate}ms ago and subjects unchanged")
                return@launch
            }

            if (subjectsChanged) {
                android.util.Log.d("HomeViewModel", "Processing casual data - subjects changed")
            }

            android.util.Log.d("HomeViewModel", "Processing CASUAL mode")
            android.util.Log.d("HomeViewModel", "Casual subjects: ${userPreferences.casualSubjects}")
            _uiState.value = _uiState.value.copy(selectedMode = StudyMode.CASUAL)

            val casualTasks = userPreferences.casualTasks
            val casualDetails = CasualDetails(tasks = casualTasks)

            // Check database first
            val dbSessionsToday = withContext(Dispatchers.IO) {
                repository.getTodaySessionsForUser(userId).firstOrNull()?.filter { it.modeName == "CASUAL" } ?: emptyList()
            }

            // Build completion map from database
            val completionMap = dbSessionsToday
                .groupBy { it.subjectName }
                .mapValues { it.value.size }

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
            val normalized = normalizeDailyInputs(
                rawDailyStudyHours = userPreferences.casualPreferences.dailyStudyHours,
                rawSessionLengthMinutes = userPreferences.casualPreferences.sessionLength.minutes
            )

            var shouldPersistPlan = false
            val sessions = if (userPreferences.casualSubjects.isNotEmpty()) {
                // Check for saved plan first
                if (userPreferences.casualPlanDate == today && userPreferences.casualDailyPlan.isNotEmpty()) {
                    val sanitizedSaved = userPreferences.casualDailyPlan.map { s ->
                        s.copy(subject = s.subject.trim(), durationMinutes = max(1, s.durationMinutes))
                    }

                    val validSubjectNames = userPreferences.casualSubjects.map { it.name }.toSet()
                    val allSubjectsValid = sanitizedSaved.all { it.subject.isNotBlank() && it.subject in validSubjectNames }
                    val matchingCounts = sanitizedSaved.size == normalized.totalSessions

                    if (allSubjectsValid && matchingCounts) {
                        // CRITICAL FIX: Apply database completion status to saved plan
                        val subjectCompletionTracker = mutableMapOf<String, Int>()
                        var foundCurrent = false

                        val updatedSessions = sanitizedSaved.mapIndexed { index, s ->
                            // Determine status based on DB completions
                            val subjectName = s.subject
                            val timesCompleted = completionMap.getOrDefault(subjectName, 0)
                            val currentCount = subjectCompletionTracker.getOrDefault(subjectName, 0)

                            val status = when {
                                currentCount < timesCompleted -> {
                                    subjectCompletionTracker[subjectName] = currentCount + 1
                                    CasualSessionStatus.COMPLETED
                                }
                                !foundCurrent -> {
                                    foundCurrent = true
                                    CasualSessionStatus.CURRENT
                                }
                                else -> CasualSessionStatus.UPCOMING
                            }

                            CasualSession(
                                sessionNumber = index + 1,
                                subjectName = s.subject,
                                duration = normalized.sessionLengthMinutes,
                                status = status
                            )
                        }

                        // CRITICAL: Only persist if session length actually changed
                        if (sanitizedSaved.any { it.durationMinutes != normalized.sessionLengthMinutes }) {
                            shouldPersistPlan = true
                            android.util.Log.d("HomeViewModel", "Updating casual plan with new session length")
                        } else {
                            // Don't persist - just use the loaded plan with updated statuses
                            android.util.Log.d("HomeViewModel", "Accepting saved casual plan with DB sync - NO PERSIST")
                        }

                        updatedSessions
                    } else {
                        // Regenerate
                        shouldPersistPlan = true
                        android.util.Log.d("HomeViewModel", "Regenerating casual plan")
                        withContext(Dispatchers.Default) {
                            generateCasualSessions(userPreferences.casualSubjects, normalized.totalSessions, normalized.sessionLengthMinutes, completionMap, weeklyPriorities = getWeeklyPrioritiesForMode(StudyMode.CASUAL, userPreferences))
                        }
                    }
                } else {
                    // No saved plan — generate new
                    shouldPersistPlan = true
                    withContext(Dispatchers.Default) {
                        generateCasualSessions(userPreferences.casualSubjects, normalized.totalSessions, normalized.sessionLengthMinutes, completionMap, weeklyPriorities = getWeeklyPrioritiesForMode(StudyMode.CASUAL, userPreferences))
                    }
                }
            } else {
                // No subjects - clear plan only if there's actually a plan to clear
                if (userPreferences.casualDailyPlan.isNotEmpty()) {
                    val authState = authViewModel.authState.first()
                    if (authState is AuthState.Authenticated) {
                        withContext(Dispatchers.IO) {
                            val currentPrefs = userPreferencesRepository.getUserPreferences(authState.uid).first()
                            if (currentPrefs != null && currentPrefs.casualDailyPlan.isNotEmpty()) {
                                val updatedPrefs = currentPrefs.copy(
                                    casualDailyPlan = emptyList(),
                                    casualPlanDate = "",
                                    lastAccessed = System.currentTimeMillis()
                                )
                                userPreferencesRepository.saveUserPreferences(updatedPrefs)
                                android.util.Log.d("HomeViewModel", "Cleared stale casual daily plan")
                            }
                        }
                    }
                }
                emptyList()
            }

            // CRITICAL: Only persist to DB if we're actually creating/updating a plan
            if (shouldPersistPlan && sessions.isNotEmpty()) {
                saveCasualDailyPlan(sessions)
                android.util.Log.d("HomeViewModel", "Persisted casual plan to preferences only")
            }

            val casualMetrics = CasualMetrics(
                completedToday = sessions.count { it.status == CasualSessionStatus.COMPLETED },
                pendingToday = sessions.size - sessions.count { it.status == CasualSessionStatus.COMPLETED },
                studyStreak = 0
            )
            val casualAlerts = generateCasualAlerts(casualTasks)

            // Update UI on main thread
            _uiState.value = _uiState.value.copy(
                casualDetails = casualDetails,
                casualSessions = sessions,
                casualMetrics = casualMetrics,
                casualAlerts = casualAlerts
            )

            checkAndShowWeeklyPriorityDialog(StudyMode.CASUAL, userPreferences)
        }
    }

    private fun generateCasualSessions(
        subjects: List<Subject>,
        totalSessions: Int,
        sessionLength: Int,
        completionMap: Map<String, Int> = emptyMap(),
        weeklyPriorities: List<String> = emptyList()
    ): List<CasualSession> {
        if (subjects.isEmpty()) return emptyList()

        val orderedSubjects = if (weeklyPriorities.isNotEmpty()) {
            val prioritized = weeklyPriorities.mapNotNull { name -> subjects.find { it.name == name } }
            val rest = subjects.filter { it.name !in weeklyPriorities }
            prioritized + rest
        } else {
            subjects
        }

        val subjectCompletionTracker = mutableMapOf<String, Int>()
        var foundCurrent = false

        return (0 until totalSessions).map { index ->
            val subject = orderedSubjects[index % orderedSubjects.size]
            val subjectName = subject.name

            val timesCompleted = completionMap.getOrDefault(subjectName, 0)
            val currentCount = subjectCompletionTracker.getOrDefault(subjectName, 0)

            val status = when {
                currentCount < timesCompleted -> {
                    subjectCompletionTracker[subjectName] = currentCount + 1
                    CasualSessionStatus.COMPLETED
                }
                !foundCurrent -> {
                    foundCurrent = true
                    CasualSessionStatus.CURRENT
                }
                else -> CasualSessionStatus.UPCOMING
            }

            CasualSession(
                sessionNumber = index + 1,
                subjectName = subjectName,
                duration = sessionLength,
                status = status
            )
        }
    }

    // Minimal casual alerts generator to avoid unresolved references and provide safe defaults
    private fun generateCasualAlerts(tasks: List<CasualTask>): List<CasualAlert> {
        val alerts = mutableListOf<CasualAlert>()
        if (tasks.isEmpty()) {
            alerts.add(CasualAlert("No tasks added — consider adding one to make casual sessions productive", CasualAlertType.AWARENESS))
        } else {
            val count = tasks.size
            alerts.add(CasualAlert("You have $count casual task(s)", CasualAlertType.AWARENESS))
        }
        return alerts
    }
    // endregion

    // region Swap Logic (Shared)
    fun enterSwapMode() {
        when (_uiState.value.selectedMode) {
            StudyMode.EXAM -> {
                _uiState.value = _uiState.value.copy(
                    isSwapMode = true,
                    originalStudySessions = _uiState.value.studySessions
                )
            }
            StudyMode.FOCUS -> {
                _uiState.value = _uiState.value.copy(
                    isFocusSwapMode = true,
                    originalFocusSessions = _uiState.value.focusSessions
                )
            }
            StudyMode.CASUAL -> {
                _uiState.value = _uiState.value.copy(
                    isCasualSwapMode = true,
                    originalCasualSessions = _uiState.value.casualSessions
                )
            }
        }
    }

    fun cancelSwap() {
        when (_uiState.value.selectedMode) {
            StudyMode.EXAM -> {
                _uiState.value = _uiState.value.copy(
                    isSwapMode = false,
                    swapSourceIndex = null,
                    studySessions = _uiState.value.originalStudySessions, // Revert to original order
                    originalStudySessions = null
                )
            }
            StudyMode.FOCUS -> {
                _uiState.value = _uiState.value.copy(
                    isFocusSwapMode = false,
                    focusSwapSourceIndex = null,
                    focusSessions = _uiState.value.originalFocusSessions,
                    originalFocusSessions = null
                )
            }
            StudyMode.CASUAL -> {
                _uiState.value = _uiState.value.copy(
                    isCasualSwapMode = false,
                    casualSwapSourceIndex = null,
                    casualSessions = _uiState.value.originalCasualSessions,
                    originalCasualSessions = null
                )
            }
        }
    }

    fun toggleTaskCompletion(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            val authState = authViewModel.authState.first()
            if (authState is AuthState.Authenticated) {
                val modeName = when (_uiState.value.selectedMode) {
                    StudyMode.FOCUS -> "FOCUS"
                    StudyMode.CASUAL -> "CASUAL"
                    else -> return@launch
                }

                withContext(Dispatchers.IO) {
                    userPreferencesRepository.updateTaskCompletion(
                        userId = authState.uid,
                        taskId = taskId,
                        isCompleted = isCompleted,
                        modeName = modeName
                    )
                }

                android.util.Log.d("HomeViewModel", "Task $taskId marked as ${if (isCompleted) "completed" else "incomplete"}")
            }
        }
    }

    fun saveSwap() {
        viewModelScope.launch {
            when (_uiState.value.selectedMode) {
                StudyMode.EXAM -> {
                    val renumberedSessions = _uiState.value.studySessions?.mapIndexed { index, session ->
                        session.copy(sessionNumber = index + 1)
                    }
                    _uiState.value = _uiState.value.copy(
                        studySessions = renumberedSessions,
                        isSwapMode = false,
                        swapSourceIndex = null,
                        originalStudySessions = null
                    )
                    renumberedSessions?.let {
                        lastSwapSaveTime = System.currentTimeMillis()
                        lastDatabaseUpdateTime = 0L
                        saveDailyPlan(it)
                        android.util.Log.d("HomeViewModel", "Swap saved and persisted for EXAM mode")
                    }
                }
                StudyMode.FOCUS -> {
                    val renumberedSessions = _uiState.value.focusSessions?.mapIndexed { index, session ->
                        session.copy(sessionNumber = index + 1)
                    }
                    _uiState.value = _uiState.value.copy(
                        focusSessions = renumberedSessions,
                        isFocusSwapMode = false,
                        focusSwapSourceIndex = null,
                        originalFocusSessions = null
                    )
                    renumberedSessions?.let {
                        lastSwapSaveTime = System.currentTimeMillis()
                        lastDatabaseUpdateTime = 0L
                        saveFocusDailyPlan(it)
                        android.util.Log.d("HomeViewModel", "Swap saved and persisted for FOCUS mode")
                    }
                }
                StudyMode.CASUAL -> {
                    val renumberedSessions = _uiState.value.casualSessions?.mapIndexed { index, session ->
                        session.copy(sessionNumber = index + 1)
                    }
                    _uiState.value = _uiState.value.copy(
                        casualSessions = renumberedSessions,
                        isCasualSwapMode = false,
                        casualSwapSourceIndex = null,
                        originalCasualSessions = null
                    )
                    renumberedSessions?.let {
                        lastSwapSaveTime = System.currentTimeMillis()
                        lastDatabaseUpdateTime = 0L
                        saveCasualDailyPlan(it)
                        android.util.Log.d("HomeViewModel", "Swap saved and persisted for CASUAL mode")
                    }
                }
            }
        }
    }

    fun handleSessionClickInSwapMode(index: Int) {
        when (_uiState.value.selectedMode) {
            StudyMode.EXAM -> handleExamSessionSwap(index)
            StudyMode.FOCUS -> handleFocusSessionSwap(index)
            StudyMode.CASUAL -> handleCasualSessionSwap(index)
        }
    }

    private fun handleExamSessionSwap(index: Int) {
        val sourceIndex = _uiState.value.swapSourceIndex

        if (sourceIndex == null) {
            _uiState.value = _uiState.value.copy(swapSourceIndex = index)
        } else {
            if (sourceIndex == index) {
                _uiState.value = _uiState.value.copy(swapSourceIndex = null)
                return
            }

            val sessions = _uiState.value.studySessions ?: return
            val subjectMap = _uiState.value.examDetails?.subjects?.associateBy { it.name } ?: emptyMap()
            val proposedList = sessions.toMutableList()
            Collections.swap(proposedList, sourceIndex, index)

            var consecutiveHardCount = 0
            var isSwapValid = true
            for (session in proposedList) {
                if (subjectMap[session.subject]?.difficulty == Difficulty.Hard) {
                    consecutiveHardCount++
                    if (consecutiveHardCount > 2) {
                        isSwapValid = false
                        break
                    }
                } else {
                    consecutiveHardCount = 0
                }
            }

            if (isSwapValid) {
                _uiState.value = _uiState.value.copy(studySessions = proposedList)
            }

            _uiState.value = _uiState.value.copy(swapSourceIndex = null)
        }
    }

    private fun handleFocusSessionSwap(index: Int) {
        val sourceIndex = _uiState.value.focusSwapSourceIndex

        if (sourceIndex == null) {
            _uiState.value = _uiState.value.copy(focusSwapSourceIndex = index)
        } else {
            if (sourceIndex == index) {
                _uiState.value = _uiState.value.copy(focusSwapSourceIndex = null)
                return
            }

            val sessions = _uiState.value.focusSessions?.toMutableList() ?: return
            Collections.swap(sessions, sourceIndex, index)

            if (isFocusSwapValid(sessions)) {
                _uiState.value = _uiState.value.copy(focusSessions = sessions)
            }

            _uiState.value = _uiState.value.copy(focusSwapSourceIndex = null)
        }
    }

    private fun handleCasualSessionSwap(index: Int) {
        val sourceIndex = _uiState.value.casualSwapSourceIndex

        if (sourceIndex == null) {
            _uiState.value = _uiState.value.copy(casualSwapSourceIndex = index)
        } else {
            if (sourceIndex == index) {
                _uiState.value = _uiState.value.copy(casualSwapSourceIndex = null)
                return
            }

            val sessions = _uiState.value.casualSessions?.toMutableList() ?: return
            Collections.swap(sessions, sourceIndex, index)
            _uiState.value = _uiState.value.copy(casualSessions = sessions, casualSwapSourceIndex = null)
        }
    }


    fun skipCatchupSession(sessionIndex: Int) {
        // Only valid in EXAM mode
        if (_uiState.value.selectedMode != StudyMode.EXAM) return

        viewModelScope.launch {
            val authState = authViewModel.authState.first()
            if (authState !is AuthState.Authenticated) return@launch

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)

            // ── Update UI immediately from current displayed list ─────────────────
            val currentSessions = _uiState.value.studySessions?.toMutableList() ?: return@launch
            val updatedSessions = currentSessions
                .filterIndexed { index, _ -> index != sessionIndex }
                .mapIndexed { i, s -> s.copy(sessionNumber = i + 1) }
            _uiState.value = _uiState.value.copy(studySessions = updatedSessions)

            // ── Persist: translate UI index → original dailyPlan index ───────────
            // dailyPlan is ALWAYS kept full (never shrunk). exemptedSessions is the
            // single source of truth for what is hidden. Because the UI list is already
            // filtered by previous exemptions, sessionIndex is a position in that shorter
            // list. We walk the full dailyPlan, skipping already-exempted slots, to find
            // which original index the user just tapped.
            withContext(Dispatchers.IO) {
                val currentPrefs = userPreferencesRepository.getUserPreferences(authState.uid).first() ?: return@withContext
                val currentExemptions = currentPrefs.exemptedSessions.toMutableMap()
                val existingExemptions = (currentExemptions[today] ?: emptyList()).toMutableList()

                val originalPlan = currentPrefs.dailyPlan
                var originalIndex = 0
                var uiCount = 0
                // Walk until we've counted past sessionIndex non-exempted slots
                while (originalIndex < originalPlan.size) {
                    if (originalIndex !in existingExemptions) {
                        if (uiCount == sessionIndex) break   // found the matching original slot
                        uiCount++
                    }
                    originalIndex++
                }

                if (originalIndex >= originalPlan.size) {
                    android.util.Log.e("HomeViewModel", "skipCatchupSession: UI index $sessionIndex could not be mapped (originalPlan.size=${originalPlan.size}, existingExemptions=$existingExemptions)")
                    return@withContext
                }

                android.util.Log.d("HomeViewModel", "skipCatchupSession: UI index $sessionIndex → original index $originalIndex")

                // Record the exemption
                if (originalIndex !in existingExemptions) {
                    existingExemptions.add(originalIndex)
                    currentExemptions[today] = existingExemptions.sorted()
                }

                // IMPORTANT: save dailyPlan unchanged (full original). Only exemptedSessions grows.
                // This keeps matchingCounts valid on every reload so the plan is never regenerated.
                val updatedPrefs = currentPrefs.copy(
                    exemptedSessions = currentExemptions,
                    // dailyPlan intentionally NOT modified — stays full so index walk works next time
                    lastAccessed = System.currentTimeMillis()
                )
                userPreferencesRepository.saveUserPreferences(updatedPrefs)
                android.util.Log.d("HomeViewModel", "Skipped catchup: originalIndex=$originalIndex exemptions=$existingExemptions dailyPlan stays at ${originalPlan.size} sessions")
            }
        }
    }

    private fun getCurrentWeekStartDate(): String {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
    }

    // ── Helper: extract priorities for the current week from prefs ──────────
    private fun getWeeklyPrioritiesForMode(mode: StudyMode, prefs: UserPreferences): List<String> {
        val weekKey = getCurrentWeekStartDate()
        return when (mode) {
            StudyMode.FOCUS -> prefs.focusWeeklySubjectPriorities[weekKey] ?: emptyList()
            StudyMode.CASUAL -> prefs.casualWeeklySubjectPriorities[weekKey] ?: emptyList()
            else -> emptyList()
        }
    }


    private fun checkAndShowWeeklyPriorityDialog(mode: StudyMode, prefs: UserPreferences) {
        val weekKey = getCurrentWeekStartDate()
        val hasSetThisWeek = when (mode) {
            StudyMode.FOCUS -> prefs.focusWeeklySubjectPriorities.containsKey(weekKey)
            StudyMode.CASUAL -> prefs.casualWeeklySubjectPriorities.containsKey(weekKey)
            else -> true
        }
        if (!hasSetThisWeek) {
            _uiState.value = _uiState.value.copy(
                showWeeklyPriorityDialog = true,
                weeklyPriorityDialogMode = mode
            )
        }
    }

    fun saveWeeklyPriorities(orderedSubjectNames: List<String>) {
        viewModelScope.launch {
            val authState = authViewModel.authState.first()
            if (authState is AuthState.Authenticated) {
                val weekKey = getCurrentWeekStartDate()
                withContext(Dispatchers.IO) {
                    val currentPrefs = userPreferencesRepository.getUserPreferences(authState.uid).first() ?: return@withContext
                    val updatedPrefs = when (_uiState.value.weeklyPriorityDialogMode) {
                        StudyMode.FOCUS -> {
                            val updated = currentPrefs.focusWeeklySubjectPriorities.toMutableMap()
                            updated[weekKey] = orderedSubjectNames
                            currentPrefs.copy(
                                focusWeeklySubjectPriorities = updated,
                                lastAccessed = System.currentTimeMillis()
                            )
                        }
                        StudyMode.CASUAL -> {
                            val updated = currentPrefs.casualWeeklySubjectPriorities.toMutableMap()
                            updated[weekKey] = orderedSubjectNames
                            currentPrefs.copy(
                                casualWeeklySubjectPriorities = updated,
                                lastAccessed = System.currentTimeMillis()
                            )
                        }
                        else -> return@withContext
                    }
                    userPreferencesRepository.saveUserPreferences(updatedPrefs)
                }
                _uiState.value = _uiState.value.copy(
                    showWeeklyPriorityDialog = false,
                    weeklyPriorityDialogMode = null
                )
                // Force plan to regenerate with new priorities
                refreshFromDatabase()
                android.util.Log.d("HomeViewModel", "Weekly priorities saved: $orderedSubjectNames")
            }
        }
    }

    fun dismissWeeklyPriorityDialog() {
        viewModelScope.launch {
            val authState = authViewModel.authState.first()
            if (authState is AuthState.Authenticated) {
                val weekKey = getCurrentWeekStartDate()
                withContext(Dispatchers.IO) {
                    val currentPrefs = userPreferencesRepository.getUserPreferences(authState.uid).first() ?: return@withContext
                    // Store empty list so we don't ask again this week
                    val updatedPrefs = when (_uiState.value.weeklyPriorityDialogMode) {
                        StudyMode.FOCUS -> {
                            val updated = currentPrefs.focusWeeklySubjectPriorities.toMutableMap()
                            updated[weekKey] = emptyList()
                            currentPrefs.copy(focusWeeklySubjectPriorities = updated)
                        }
                        StudyMode.CASUAL -> {
                            val updated = currentPrefs.casualWeeklySubjectPriorities.toMutableMap()
                            updated[weekKey] = emptyList()
                            currentPrefs.copy(casualWeeklySubjectPriorities = updated)
                        }
                        else -> return@withContext
                    }
                    userPreferencesRepository.saveUserPreferences(updatedPrefs)
                }
            }
            _uiState.value = _uiState.value.copy(
                showWeeklyPriorityDialog = false,
                weeklyPriorityDialogMode = null
            )
        }
    }

    // ── Enter subject-change mode for a specific session ─────────────────────
    fun enterSubjectChangeMode(sessionIndex: Int) {
        _uiState.value = _uiState.value.copy(
            isSubjectChangeMode = true,
            subjectChangeSessionIndex = sessionIndex
        )
    }

    // ── Cancel subject change (user dismissed sheet without choosing) ─────────
    fun cancelSubjectChange() {
        _uiState.value = _uiState.value.copy(
            isSubjectChangeMode = false,
            subjectChangeSessionIndex = null
        )
    }

    fun changeSessionSubject(sessionIndex: Int, newSubjectName: String) {
        viewModelScope.launch {
            when (_uiState.value.selectedMode) {
                StudyMode.FOCUS -> {
                    val sessions = _uiState.value.focusSessions?.toMutableList() ?: return@launch
                    val oldSession = sessions[sessionIndex]
                    val subject = _uiState.value.focusDetails?.subjects?.find { it.name == newSubjectName }
                    val difficultyWeights = mapOf(
                        com.example.studypilot.ui.shared.Difficulty.Hard to 3,
                        com.example.studypilot.ui.shared.Difficulty.Medium to 2,
                        com.example.studypilot.ui.shared.Difficulty.Easy to 1
                    )
                    val priorityWeights = mapOf(
                        com.example.studypilot.ui.shared.Priority.High to 3,
                        com.example.studypilot.ui.shared.Priority.Medium to 2,
                        com.example.studypilot.ui.shared.Priority.Low to 1
                    )
                    val cognitiveScore = (difficultyWeights[subject?.difficulty] ?: 1) *
                            (priorityWeights[subject?.priority] ?: 1)
                    sessions[sessionIndex] = oldSession.copy(
                        subjectName = newSubjectName,
                        cognitiveLoadScore = cognitiveScore
                    )
                    _uiState.value = _uiState.value.copy(
                        focusSessions = sessions,
                        isSubjectChangeMode = false,
                        subjectChangeSessionIndex = null
                    )
                    saveFocusDailyPlan(sessions)
                    android.util.Log.d("HomeViewModel", "Focus session $sessionIndex subject changed to $newSubjectName")
                }
                StudyMode.CASUAL -> {
                    val sessions = _uiState.value.casualSessions?.toMutableList() ?: return@launch
                    sessions[sessionIndex] = sessions[sessionIndex].copy(subjectName = newSubjectName)
                    _uiState.value = _uiState.value.copy(
                        casualSessions = sessions,
                        isSubjectChangeMode = false,
                        subjectChangeSessionIndex = null
                    )
                    saveCasualDailyPlan(sessions)
                    android.util.Log.d("HomeViewModel", "Casual session $sessionIndex subject changed to $newSubjectName")
                }
                StudyMode.EXAM -> {
                    val sessions = _uiState.value.studySessions?.toMutableList() ?: return@launch
                    sessions[sessionIndex] = sessions[sessionIndex].copy(
                        subject = newSubjectName
                    )
                    _uiState.value = _uiState.value.copy(
                        studySessions = sessions,
                        isSubjectChangeMode = false,
                        subjectChangeSessionIndex = null
                    )
                    saveDailyPlan(sessions)
                    android.util.Log.d("HomeViewModel", "Exam session $sessionIndex subject changed to $newSubjectName")
                }
                else -> {
                    _uiState.value = _uiState.value.copy(
                        isSubjectChangeMode = false,
                        subjectChangeSessionIndex = null
                    )
                }
            }
        }
    }

    private fun checkAndShowExamOverDialog(prefs: UserPreferences, examDetails: ExamDetails) {
        val examDateMs = examDetails.examDate
        if (examDateMs <= 0L) return

        val examDate = java.time.Instant.ofEpochMilli(examDateMs)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()

        val today = java.time.LocalDate.now()

        // Trigger when exam date is TODAY (0 days remaining) OR has already passed
        if (today.isBefore(examDate)) return

        val examDateKey = examDate.toString() // "yyyy-MM-dd"

        // Don't show again if already handled for this exam date
        if (prefs.examOverDialogShownForExamDate == examDateKey) return

        // Show the "set new exam" dialog on the home screen
        _uiState.value = _uiState.value.copy(showNewExamDialog = true)
    }

    // ── Called when user confirms setting a new exam; preserves all subject data ──
    fun onNewExamConfirmed() {
        viewModelScope.launch {
            val authState = authViewModel.authState.first()
            if (authState is AuthState.Authenticated) {
                withContext(Dispatchers.IO) {
                    val currentPrefs = userPreferencesRepository.getUserPreferences(authState.uid).first()
                        ?: return@withContext
                    val examDateMs = currentPrefs.examDate ?: return@withContext
                    val examDateKey = java.time.Instant.ofEpochMilli(examDateMs)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                        .toString()
                    // Mark as handled; clear ONLY the date and today's plan — keep all subjects intact
                    val updatedPrefs = currentPrefs.copy(
                        examOverDialogShownForExamDate = examDateKey,
                        examDate = null,                  // clear old date so user sets a new one
                        dailyPlan = emptyList(),          // today's plan will regenerate
                        dailyPlanDate = null,
                        exemptedSessions = emptyMap(),    // reset exemptions for fresh start
                        lastAccessed = System.currentTimeMillis()
                    )
                    userPreferencesRepository.saveUserPreferences(updatedPrefs)
                }
            }
            _uiState.value = _uiState.value.copy(showNewExamDialog = false)
        }
    }

    // ── Called when user dismisses without setting new exam (stays on home screen) ──
    fun onNewExamDismissed() {
        viewModelScope.launch {
            val authState = authViewModel.authState.first()
            if (authState is AuthState.Authenticated) {
                withContext(Dispatchers.IO) {
                    val currentPrefs = userPreferencesRepository.getUserPreferences(authState.uid).first()
                        ?: return@withContext
                    val examDateMs = currentPrefs.examDate ?: return@withContext
                    val examDateKey = java.time.Instant.ofEpochMilli(examDateMs)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                        .toString()
                    // Mark as shown so it doesn't reappear, but don't clear any data
                    val updatedPrefs = currentPrefs.copy(
                        examOverDialogShownForExamDate = examDateKey,
                        lastAccessed = System.currentTimeMillis()
                    )
                    userPreferencesRepository.saveUserPreferences(updatedPrefs)
                }
            }
            _uiState.value = _uiState.value.copy(showNewExamDialog = false)
        }
    }

    // ── Save the user's post-exam subject selection ──────────────────────────
    fun saveExamSubjectReset(selectedSubjectNames: List<String>) {
        viewModelScope.launch {
            val authState = authViewModel.authState.first()
            if (authState is AuthState.Authenticated) {
                withContext(Dispatchers.IO) {
                    val currentPrefs = userPreferencesRepository.getUserPreferences(authState.uid).first()
                        ?: return@withContext

                    // Get the exam date key
                    val examDateMs = currentPrefs.examDate ?: return@withContext
                    val examDateKey = java.time.Instant.ofEpochMilli(examDateMs)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                        .toString()

                    // Filter examSubjects to only keep the selected ones, preserving Subject objects
                    val filteredSubjects = currentPrefs.examSubjects.filter {
                        it.name in selectedSubjectNames
                    }

                    val updatedPrefs = currentPrefs.copy(
                        examSubjects = filteredSubjects,
                        examPostSubjects = selectedSubjectNames,
                        examOverDialogShownForExamDate = examDateKey,
                        // Clear today's plan so it regenerates with the new subject list
                        dailyPlan = emptyList(),
                        dailyPlanDate = null,
                        lastAccessed = System.currentTimeMillis()
                    )
                    userPreferencesRepository.saveUserPreferences(updatedPrefs)
                }
                _uiState.value = _uiState.value.copy(showExamOverDialog = false)
                // Regenerate today's plan with updated subjects
                refreshFromDatabase()
                android.util.Log.d("HomeViewModel", "Exam subject reset saved: $selectedSubjectNames")
            }
        }
    }

    // ── Dismiss exam-over dialog (keep all subjects) ─────────────────────────
    fun dismissExamOverDialog() {
        viewModelScope.launch {
            val authState = authViewModel.authState.first()
            if (authState is AuthState.Authenticated) {
                withContext(Dispatchers.IO) {
                    val currentPrefs = userPreferencesRepository.getUserPreferences(authState.uid).first()
                        ?: return@withContext

                    val examDateMs = currentPrefs.examDate ?: return@withContext
                    val examDateKey = java.time.Instant.ofEpochMilli(examDateMs)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                        .toString()

                    // Mark as shown so we don't show again for this exam date
                    val updatedPrefs = currentPrefs.copy(
                        examOverDialogShownForExamDate = examDateKey,
                        lastAccessed = System.currentTimeMillis()
                    )
                    userPreferencesRepository.saveUserPreferences(updatedPrefs)
                }
            }
            _uiState.value = _uiState.value.copy(showExamOverDialog = false)
        }
    }
}


data class FocusDetails(
    val subjects: List<FocusSubject>,
    val tasks: List<FocusTask>,
    val planType: String?,
    val excludeSunday: Boolean?,
    val dailyStudyHours: Float,
    val preferredSessionLength: Int,
    val breakPreference: com.example.studypilot.ui.mode.FocusBreakPreference,
    val notificationsEnabled: Boolean
)

data class FocusSession(
    val sessionNumber: Int,
    val subjectName: String,
    val duration: Int,
    val cognitiveLoadScore: Int,
    val status: SessionStatus
)

data class FocusMetrics(
    val completedToday: Int,
    val pendingToday: Int,
    val focusStreak: Int
)

data class FocusAlert(
    val message: String,
    val type: FocusAlertType
)