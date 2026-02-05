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
    val casualSwapSourceIndex: Int? = null
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

    private var lastProcessedTime: Long = 0L
    private val DEBOUNCE_DELAY_MS = 500L  // 500ms minimum between processes

    init {
        viewModelScope.launch {
            authViewModel.authState.collectLatest { authState ->
                if (authState is AuthState.Authenticated) {
                    _uiState.value = _uiState.value.copy(userEmail = authState.email)

                    // NEW: Collect completed sessions from database and update UI
                    launch {
                        repository.getSessionsForUser(authState.uid).collectLatest { completedSessions ->
                            updateSessionStatusesFromDatabase(completedSessions)
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
                            if (incomingTs != 0L && incomingTs < lastProcessedPrefsTimestamp) {
                                android.util.Log.d("HomeViewModel", "Skipping stale prefs emission (lastAccessed=$incomingTs) older than processedTs=$lastProcessedPrefsTimestamp")
                                return@collectLatest
                            }

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
                                StudyMode.FOCUS -> processFocusData(userPreferences)
                                StudyMode.CASUAL -> processCasualData(userPreferences)
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

        // OPTIMIZATION: Only update if count changed
        val currentCompletedCount = when (_uiState.value.selectedMode) {
            StudyMode.EXAM -> _uiState.value.studySessions?.count { it.status == SessionStatus.COMPLETED } ?: 0
            StudyMode.FOCUS -> _uiState.value.focusSessions?.count { it.status == SessionStatus.COMPLETED } ?: 0
            StudyMode.CASUAL -> _uiState.value.casualSessions?.count { it.status == CasualSessionStatus.COMPLETED } ?: 0
        }

        if (todayCompletedSessions.size == currentCompletedCount) {
            // No new completions, skip update
            return
        }

        android.util.Log.d("HomeViewModel", "Database sync: ${todayCompletedSessions.size} sessions completed today (was $currentCompletedCount)")

        // Launch coroutine since updateExamSessionStatuses is now suspend
        viewModelScope.launch {
            when (_uiState.value.selectedMode) {
                StudyMode.EXAM -> updateExamSessionStatuses(todayCompletedSessions, today)
                StudyMode.FOCUS -> updateFocusSessionStatuses(todayCompletedSessions, today)
                StudyMode.CASUAL -> updateCasualSessionStatuses(todayCompletedSessions, today)
            }
        }
    }

    private suspend fun updateExamSessionStatuses(todayCompletedSessions: List<com.example.studypilot.data.StudySession>, today: String) {
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
        val streak = calculateExamStreak(todayCompletedSessions)

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

        _uiState.value = _uiState.value.copy(
            studySessions = updatedSessions,
            accountabilityMetrics = metrics,
            studyStreak = streak,
            alerts = alerts
        )
    }

    private fun updateFocusSessionStatuses(todayCompletedSessions: List<com.example.studypilot.data.StudySession>, today: String) {
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

        val currentStreak = _uiState.value.focusMetrics?.focusStreak ?: 0
        val metrics = calculateFocusMetrics(updatedSessions, currentStreak)

        val alerts = generateFocusAlerts(metrics.focusStreak, metrics.pendingToday, updatedSessions.size)

        android.util.Log.d("HomeViewModel", "Updated focus sessions: completed=$completedCount, totalTime=${totalTime}min")

        _uiState.value = _uiState.value.copy(
            focusSessions = updatedSessions,
            focusMetrics = metrics,
            focusAlerts = alerts
        )
    }

    private fun updateCasualSessionStatuses(todayCompletedSessions: List<com.example.studypilot.data.StudySession>, today: String) {
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

        val metrics = CasualMetrics(
            completedToday = completed,
            pendingToday = pending,
            studyStreak = 0 // Implement streak logic if needed
        )

        _uiState.value = _uiState.value.copy(
            casualSessions = updatedSessions,
            casualMetrics = metrics
        )
    }

    private fun calculateExamStreak(todayCompletedSessions: List<com.example.studypilot.data.StudySession>): Int {
        // Simple streak: count consecutive days with at least 1 completed session
        // For now, return 1 if any session completed today, else 0
        // TODO: Implement proper multi-day streak tracking
        return if (todayCompletedSessions.any { it.completed }) 1 else 0
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
        // Create a hash of the key preferences to detect actual changes
        _uiState.value = _uiState.value.copy(selectedMode = StudyMode.EXAM)

        // ADD BACK THIS SECTION - it was missing!
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

        android.util.Log.d(
            "HomeViewModel",
            "Normalized exam inputs: dailyMinutes=${normalized.dailyMinutes}, sessionLength=${normalized.sessionLengthMinutes}, totalSessions=${normalized.totalSessions}"
        )

        // If a saved daily plan exists for today, VALIDATE it before accepting.
        // Accept a saved plan only when:
        //  - every session has a non-blank subject name AND that subject exists in the current sanitized `safeSubjects` list
        //  - the saved plan's session count equals the normalized total sessions for today
        // Otherwise regenerate using the current `safeSubjects` (so user-entered subjects are respected).
        var shouldPersistPlan = false
        val sessions = if (userPreferences.dailyPlanDate == today && userPreferences.dailyPlan.isNotEmpty()) {
            // Trim and normalize saved sessions defensively
            val sanitizedSaved = userPreferences.dailyPlan.map { s ->
                s.copy(subject = s.subject.trim(), durationMinutes = max(1, s.durationMinutes))
            }

            val validSubjectNames = safeSubjects.map { it.name }.toSet()

            // Validate: Saved session subjects must be non-blank and exist in savedSubjects
            val allSubjectsValid = sanitizedSaved.all { it.subject.isNotBlank() && it.subject in validSubjectNames }
            val matchingCounts = sanitizedSaved.size == normalized.totalSessions
            // Also ensure saved session durations match the current normalized session length
            val durationsMatch = sanitizedSaved.all { it.durationMinutes == normalized.sessionLengthMinutes }

            if (allSubjectsValid && matchingCounts && durationsMatch) {
                // saved plan is consistent with current exam subjects, expected count, and session length — accept it
                android.util.Log.d("HomeViewModel", "Accepting saved daily plan durations=${sanitizedSaved.map { it.durationMinutes }}")
                sanitizedSaved
            } else {
                // saved plan is stale/invalid — regenerate from current saved subjects and persist the regenerated plan
                android.util.Log.d("HomeViewModel", "Rejecting saved daily plan: allSubjectsValid=$allSubjectsValid, matchingCounts=$matchingCounts, durationsMatch=$durationsMatch; regenerating plan using examPrefs sessionLength=${normalized.sessionLengthMinutes}")
                shouldPersistPlan = true
                // Run CPU work (allocation & ordering) off the main thread
                withContext(Dispatchers.Default) {
                    generateStudySessions(safeSubjects, normalized.totalSessions, normalized.sessionLengthMinutes)
                }
            }
        } else {
            // No saved plan for today — generate fresh from saved subjects
            shouldPersistPlan = true
            val gen = withContext(Dispatchers.Default) {
                generateStudySessions(safeSubjects, normalized.totalSessions, normalized.sessionLengthMinutes)
            }
            android.util.Log.d("HomeViewModel", "Generated new daily plan durations=${gen.map { it.durationMinutes }} (from examPrefs sessionLength=${normalized.sessionLengthMinutes})")
            gen
        }

        // Compute real metrics from generated sessions (no placeholders)
        val completed = sessions.count { it.status == SessionStatus.COMPLETED }
        val pending = sessions.size - completed
        val metrics = AccountabilityMetrics(completed = completed, missed = pending, backlog = pending)

        val alerts = generateAlerts(userPreferences, examDetails, metrics)

        android.util.Log.d("HomeViewModel", "Final sessions to UI durations=${sessions.map { it.durationMinutes }} subjects=${sessions.map { it.subject }}")
        _uiState.value = _uiState.value.copy(
            examDetails = examDetails,
            studySessions = sessions,
            alerts = alerts,
            accountabilityMetrics = metrics,
            studyStreak = 0 // keep existing behavior for streak if needed
        )

        // Persist regenerated plan when we created a new plan (either because none existed, or saved plan was invalid)
        if (shouldPersistPlan) {
            // Persist regenerated plan on IO dispatcher
            saveDailyPlan(sessions)
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
        sessionLength: Int
    ): List<StudySession> {

        // Ensure sessionLength >= 1 and at least 1 session
        val sessionLengthSafe = max(1, sessionLength)
        val safeTotalSessions = totalSessions.coerceAtLeast(1)

        // Use passed-in subjects as authoritative. Caller (HomeViewModel) ensures fallback when the saved list is empty.
        val safeSubjects = subjects

        // Explicit numeric weight maps (do NOT rely on enum ordinal)
        val difficultyWeights = mapOf(
            Difficulty.Hard to 3,
            Difficulty.Medium to 2,
            Difficulty.Easy to 1
        )
        val priorityWeights = mapOf(
            Priority.High to 4,
            Priority.Medium to 2,
            Priority.Low to 1
        )

        // Compute combined weight = difficultyWeight * priorityWeight
        val subjectWeights = safeSubjects.associateWith { subj ->
            val dW = difficultyWeights[subj.difficulty] ?: 1
            val pW = priorityWeights[subj.priority] ?: 1
            val w = dW * pW
            if (w <= 0) 1 else w
        }

        // Allocate sessions: ensure each subject appears at least once when possible
        val sessionsPerSubject = mutableMapOf<Subject, Int>()

        if (safeTotalSessions >= safeSubjects.size) {
            // give one slot to each subject first
            safeSubjects.forEach { sessionsPerSubject[it] = 1 }
            var remaining = safeTotalSessions - safeSubjects.size

            // Distribute remaining deterministically by weight (highest weight first round-robin)
            val sortedByWeight = safeSubjects.sortedByDescending { subjectWeights[it] ?: 1 }
            var idx = 0
            while (remaining > 0) {
                val subj = sortedByWeight[idx % sortedByWeight.size]
                sessionsPerSubject[subj] = sessionsPerSubject.getOrDefault(subj, 0) + 1
                remaining--
                idx++
            }
        } else {
            // fewer sessions than subjects: pick top-N subjects by weight
            val topSubjects = safeSubjects.sortedByDescending { subjectWeights[it] ?: 1 }.take(safeTotalSessions)
            topSubjects.forEach { sessionsPerSubject[it] = 1 }
        }

        // Build the raw subject list according to allocation
        val rawSubjects = mutableListOf<Subject>()
        // Iterate subjects in descending weight order so higher-weight subjects appear earlier in the final list
        val subjectsSortedForOrder = safeSubjects.sortedByDescending { subjectWeights[it] ?: 1 }
        subjectsSortedForOrder.forEach { subj ->
            val count = sessionsPerSubject.getOrDefault(subj, 0)
            repeat(count) { rawSubjects.add(subj) }
        }

        // Defensive: if allocation failed and there are no saved subjects, fall back to General Study
        if (rawSubjects.isEmpty()) {
            if (safeSubjects.isEmpty()) rawSubjects.add(Subject("General Study", Priority.Low, Difficulty.Medium))
            else rawSubjects.add(safeSubjects.first())
        }

        // Apply consecutive-subject limiting
        val limitedSubjects = limitConsecutiveSubjects(rawSubjects)

        // Ensure we have exactly safeTotalSessions items: if limited reduced or increased, adjust deterministically
        val finalSubjects = mutableListOf<Subject>()
        var pointer = 0
        while (finalSubjects.size < safeTotalSessions) {
            finalSubjects.add(limitedSubjects[pointer % limitedSubjects.size])
            pointer++
        }

        // Map to StudySession ensuring non-empty subject names (respect user-entered names)
        val resulting = finalSubjects.mapIndexed { index, subject ->
            StudySession(
                sessionNumber = index + 1,
                subject = subject.name,
                durationMinutes = sessionLengthSafe,
                status = SessionStatus.UPCOMING
            )
        }

        if (resulting.isEmpty()) {
            return listOf(
                StudySession(
                    sessionNumber = 1,
                    subject = "General Study",
                    durationMinutes = sessionLengthSafe,
                    status = SessionStatus.UPCOMING
                )
            )
        }

        return resulting
    }



    private fun saveDailyPlan(sessions: List<StudySession>) {
        viewModelScope.launch {
            val authState = authViewModel.authState.first()
            if (authState is AuthState.Authenticated) {
                withContext(Dispatchers.IO) {
                    val currentPrefs = userPreferencesRepository.getUserPreferences(authState.uid).first()
                    if (currentPrefs != null) {
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
                        val updatedPrefs = currentPrefs.copy(
                            dailyPlan = sessions,
                            dailyPlanDate = today,
                            lastAccessed = System.currentTimeMillis() // Force timestamp update
                        )
                        userPreferencesRepository.saveUserPreferences(updatedPrefs)

                        android.util.Log.d("HomeViewModel", "Saved dailyPlan with ${sessions.size} sessions, durations=${sessions.map { it.durationMinutes }}")
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
                        // Convert FocusSession to a saveable format
                        val sessionData = sessions.map {
                            StudySession(
                                sessionNumber = it.sessionNumber,
                                subject = it.subjectName,
                                durationMinutes = it.duration,
                                status = it.status
                            )
                        }
                        val updatedPrefs = currentPrefs.copy(
                            focusDailyPlan = sessionData,
                            focusPlanDate = today,
                            lastAccessed = System.currentTimeMillis()
                        )
                        userPreferencesRepository.saveUserPreferences(updatedPrefs)
                        android.util.Log.d("HomeViewModel", "Saved focus dailyPlan")
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
                        // Convert CasualSession to saveable format
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
                        val updatedPrefs = currentPrefs.copy(
                            casualDailyPlan = sessionData,
                            casualPlanDate = today,
                            lastAccessed = System.currentTimeMillis()
                        )
                        userPreferencesRepository.saveUserPreferences(updatedPrefs)
                        android.util.Log.d("HomeViewModel", "Saved casual dailyPlan")
                    }
                }
            }
        }
    }

    // region Focus Mode Logic
    private fun processFocusData(userPreferences: UserPreferences) {
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

        // CRITICAL FIX: Only generate sessions when subjects exist
        val sessions = if (userPreferences.focusSubjects.isNotEmpty()) {
            generateFocusSessions(focusDetails)
        } else {
            // NO SUBJECTS - Clear any saved plan and return empty list (will show empty card in UI)
            viewModelScope.launch {
                val authState = authViewModel.authState.first()
                if (authState is AuthState.Authenticated) {
                    withContext(Dispatchers.IO) {
                        val currentPrefs = userPreferencesRepository.getUserPreferences(authState.uid).first()
                        if (currentPrefs != null && currentPrefs.focusDailyPlan.isNotEmpty()) {
                            // Clear the saved plan
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
            emptyList() // Empty list triggers the EmptySessionsCard in HomeScreenFocus UI
        }

        val metrics = calculateFocusMetrics(sessions, _uiState.value.focusMetrics?.focusStreak ?: 0)
        val alerts = generateFocusAlerts(metrics.focusStreak, metrics.pendingToday, sessions.size)

        _uiState.value = _uiState.value.copy(
            focusDetails = focusDetails,
            focusSessions = sessions,
            focusMetrics = metrics,
            focusAlerts = alerts
        )
    }


    private fun generateFocusSessions(focusDetails: FocusDetails): List<FocusSession> {
        // Use shared normalizer — no independent math here
        val normalized = normalizeDailyInputs(
            rawDailyStudyHours = focusDetails.dailyStudyHours,
            rawSessionLengthMinutes = focusDetails.preferredSessionLength
        )

        val sessionLength = normalized.sessionLengthMinutes
        val totalSessions = normalized.totalSessions

        // CHANGE: Return empty list if no subjects (don't create fallback)
        val safeSubjects = focusDetails.subjects
        if (safeSubjects.isEmpty()) {
            return emptyList()
        }

        // Use explicit weight maps for sorting/priority (do not rely on enum ordinals)
        val difficultyWeights = mapOf(Difficulty.Hard to 3, Difficulty.Medium to 2, Difficulty.Easy to 1)
        val priorityWeights = mapOf(Priority.High to 3, Priority.Medium to 2, Priority.Low to 1)

        val subjectWeights = safeSubjects.associateWith { subj ->
            (difficultyWeights[subj.difficulty] ?: 1) * (priorityWeights[subj.priority] ?: 1)
        }

        // Guarantee each subject appears at least once when possible
        val sessionsPerSubject = mutableMapOf<FocusSubject, Int>()
        safeSubjects.forEach { sessionsPerSubject[it] = 1 }

        var remainingSessions = totalSessions - safeSubjects.size

        if (remainingSessions > 0 && subjectWeights.values.sum() > 0) {
            val sortedByWeight = safeSubjects.sortedByDescending { subjectWeights[it] ?: 1 }
            var idx = 0
            while (remainingSessions > 0) {
                val subject = sortedByWeight[idx % sortedByWeight.size]
                sessionsPerSubject[subject] = sessionsPerSubject.getOrDefault(subject, 0) + 1
                remainingSessions--
                idx++
            }
        } else if (remainingSessions > 0) {
            // fallback even distribution
            val pool = safeSubjects
            var idx = 0
            while (remainingSessions > 0) {
                val subject = pool[idx % pool.size]
                sessionsPerSubject[subject] = sessionsPerSubject.getOrDefault(subject, 0) + 1
                remainingSessions--
                idx++
            }
        }

        val allSessions: MutableList<FocusSubject> = mutableListOf()
        sessionsPerSubject.forEach { (subject, count) -> repeat(count) { allSessions.add(subject) } }

        if (allSessions.isEmpty()) return emptyList() // CHANGE: Return empty instead of fallback

        // Sort subjects by explicit numeric weight (do not rely on enum ordinal)
        allSessions.sortWith(compareByDescending<FocusSubject> { subjectWeights[it] ?: 0 }.thenBy { priorityWeights[it.priority] ?: 0 })

        val balancedSubjects = limitConsecutiveSubjects(allSessions.map { Subject(it.name, it.priority, it.difficulty) })

        val finalSubjects = balancedSubjects.mapNotNull { subj -> safeSubjects.find { it.name == subj.name } }.toMutableList()
        if (finalSubjects.isEmpty()) return emptyList() // CHANGE: Return empty instead of fallback

        // Build final FocusSession list: ensure at least 1 session and exactly totalSessions
        val result = mutableListOf<FocusSession>()
        var pointer = 0
        while (result.size < max(1, totalSessions)) {
            val subj = finalSubjects[pointer % finalSubjects.size]
            result.add(
                FocusSession(
                    sessionNumber = result.size + 1,
                    subjectName = subj.name,
                    duration = sessionLength,
                    cognitiveLoadScore = (difficultyWeights[subj.difficulty] ?: 1) * (priorityWeights[subj.priority] ?: 1),
                    status = if (result.isEmpty()) SessionStatus.CURRENT else SessionStatus.UPCOMING
                )
            )
            pointer++
        }

        return result
    }

    private fun calculateFocusMetrics(sessions: List<FocusSession>, currentStreak: Int): FocusMetrics {
        val completedCount = sessions.count { it.status == SessionStatus.COMPLETED }
        val pendingCount = sessions.size - completedCount

        val seventyPercent = (sessions.size * 0.7).roundToInt()
        val deepSessionCompleted = sessions.any { it.status == SessionStatus.COMPLETED && it.cognitiveLoadScore >= 6 }
        val taskCompleted = true // placeholder for future task-based logic

        // Do NOT reset the streak automatically here. Only increment when success criteria met.
        val newStreak = if (completedCount >= seventyPercent || (deepSessionCompleted && taskCompleted)) currentStreak + 1 else currentStreak

        return FocusMetrics(completedToday = completedCount, pendingToday = pendingCount, focusStreak = newStreak)
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
    private fun processCasualData(userPreferences: UserPreferences) {
        viewModelScope.launch {
            android.util.Log.d("HomeViewModel", "Processing CASUAL mode")
            android.util.Log.d("HomeViewModel", "Casual subjects: ${userPreferences.casualSubjects}")
            _uiState.value = _uiState.value.copy(selectedMode = StudyMode.CASUAL)

            val casualTasks = userPreferences.casualTasks
            val casualDetails = CasualDetails(tasks = casualTasks)

            // ONLY GENERATE SESSIONS IF SUBJECTS EXIST
            val sessions = if (userPreferences.casualSubjects.isNotEmpty()) {
                // Move heavy work to Default dispatcher
                val result = withContext(Dispatchers.Default) {
                    val normalized = normalizeDailyInputs(
                        rawDailyStudyHours = userPreferences.casualPreferences.dailyStudyHours,
                        rawSessionLengthMinutes = userPreferences.casualPreferences.sessionLength.minutes
                    )

                    val sessionLength = normalized.sessionLengthMinutes
                    val numberOfSessions = normalized.totalSessions

                    (0 until numberOfSessions).map { index ->
                        val subject = userPreferences.casualSubjects[index % userPreferences.casualSubjects.size]
                        CasualSession(
                            sessionNumber = index + 1,
                            subjectName = subject.name,
                            duration = sessionLength,
                            status = CasualSessionStatus.UPCOMING
                        )
                    }
                }
                result
            } else {
                // NO SUBJECTS - Clear any saved plan and return empty list
                val authState = authViewModel.authState.first()
                if (authState is AuthState.Authenticated) {
                    withContext(Dispatchers.IO) {
                        val currentPrefs = userPreferencesRepository.getUserPreferences(authState.uid).first()
                        if (currentPrefs != null && currentPrefs.casualDailyPlan.isNotEmpty()) {
                            // Clear the saved plan
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
                emptyList()
            }

            val casualMetrics = CasualMetrics(
                completedToday = 0,
                pendingToday = sessions.size,
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
