package com.example.studypilot.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studypilot.data.SessionRepository
import com.example.studypilot.data.UserPreferencesRepository
import com.example.studypilot.ui.mode.StudyMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

class AnalyticsViewModel(
    private val repository: SessionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalyticsUiState?>(null)
    val uiState: StateFlow<AnalyticsUiState?> = _uiState

    init {
        observeAnalytics()
    }

    private fun observeAnalytics() {
        viewModelScope.launch {
            repository.getSessionsForUser(userId).collectLatest { sessions ->

                val today = LocalDate.now()

                /* ================ DAILY SUMMARY ================ */

                /* ================ DAILY SUMMARY ================ */

                val todaySessions = sessions.filter { session ->
                    session.timestamp.toLocalDate() == today
                }

                val completedToday = todaySessions.count { it.completed }

// Calculate PLANNED sessions (not actual DB rows) using same formula as HomeViewModel
                val userPrefs = userPreferencesRepository.getUserPreferences(userId).firstOrNull()
                val plannedSessionsToday = if (userPrefs != null) {
                    val dailyHours = when (userPrefs.selectedMode) {
                        StudyMode.EXAM -> userPrefs.examPreferences.dailyStudyHours
                        StudyMode.FOCUS -> userPrefs.focusPreferences.dailyStudyHours
                        StudyMode.CASUAL -> userPrefs.casualPreferences.dailyStudyHours
                    }
                    val sessionLengthMinutes = when (userPrefs.selectedMode) {
                        StudyMode.EXAM -> userPrefs.examPreferences.sessionLength.minutes
                        StudyMode.FOCUS -> userPrefs.focusPreferences.sessionLength.minutes
                        StudyMode.CASUAL -> userPrefs.casualPreferences.sessionLength.minutes
                    }

                    val dailyMinutes = dailyHours * 60.0
                    kotlin.math.floor(dailyMinutes / sessionLengthMinutes).toInt().coerceAtLeast(1)
                } else {
                    1 // Fallback if prefs not loaded
                }

                val dailySummary = DailySummaryUi(
                    totalStudySeconds = todaySessions.sumOf { it.elapsedSeconds },
                    sessionCount = plannedSessionsToday,  // Now shows PLANNED count, not DB row count
                    completionRate = if (plannedSessionsToday > 0) {
                        completedToday.toFloat() / plannedSessionsToday
                    } else 0f
                )

                /* ================ WEEKLY STUDY ================ */

                val weeklyStudy = (0..6).map { offset ->
                    val date = today.minusDays(offset.toLong())

                    val daySeconds = sessions
                        .filter { it.timestamp.toLocalDate() == date }
                        .sumOf { it.elapsedSeconds } // Actual time studied that day

                    DayStudyUi(
                        dayLabel = date.dayOfWeek.getDisplayName(
                            TextStyle.SHORT,
                            Locale.getDefault()
                        ),
                        studySeconds = daySeconds,
                        date = date,
                        isFuture = date.isAfter(today)
                    )
                }.reversed() // Monday first, Sunday last

                /* ================ SUBJECT STATS ================ */

                val totalStudyTime = sessions.sumOf { it.elapsedSeconds }

                val subjectStats = sessions
                    .filter { it.subjectName != "General Study" }
                    .groupBy { it.subjectName }
                    .map { (subject, sessionsList) ->
                        val subjectTime = sessionsList.sumOf { it.elapsedSeconds }
                        val completedSessions = sessionsList.count { it.completed }
                        val totalSessions = sessionsList.size

                        SubjectStatUi(
                            subjectName = subject,
                            totalStudySeconds = subjectTime,
                            sessionCount = totalSessions,
                            completedSessions = completedSessions,
                            averageSessionSeconds = if (totalSessions > 0) {
                                subjectTime / totalSessions
                            } else 0,
                            completionRate = if (totalSessions > 0) {
                                completedSessions.toFloat() / totalSessions
                            } else 0f,
                            percentage = if (totalStudyTime > 0) {
                                (subjectTime.toFloat() / totalStudyTime * 100)
                            } else 0f,
                            lastStudied = sessionsList.maxOfOrNull { it.timestamp } ?: 0L
                        )
                    }
                    .sortedByDescending { it.totalStudySeconds } // Most studied first

                /* ================ STUDY STREAK ================ */

                val streak = calculateStreak(sessions, today)

                /* ================ PRODUCTIVITY TRENDS ================ */

                val thisWeekSeconds = sessions
                    .filter { it.timestamp.toLocalDate() >= today.minusDays(6) }
                    .sumOf { it.elapsedSeconds }

                val lastWeekSeconds = sessions
                    .filter {
                        val date = it.timestamp.toLocalDate()
                        date >= today.minusDays(13) && date < today.minusDays(6)
                    }
                    .sumOf { it.elapsedSeconds }

                val weeklyChange = if (lastWeekSeconds > 0) {
                    ((thisWeekSeconds - lastWeekSeconds).toFloat() / lastWeekSeconds * 100)
                } else if (thisWeekSeconds > 0) {
                    100f // 100% increase from 0
                } else {
                    0f
                }

                _uiState.value = AnalyticsUiState(
                    dailySummary = dailySummary,
                    weeklyStudy = weeklyStudy,
                    subjectStats = subjectStats,
                    currentStreak = streak.current,
                    longestStreak = streak.longest,
                    thisWeekSeconds = thisWeekSeconds,
                    lastWeekSeconds = lastWeekSeconds,
                    weeklyChange = weeklyChange
                )
            }
        }
    }

    private fun calculateStreak(sessions: List<com.example.studypilot.data.StudySession>, today: LocalDate): StreakData {
        if (sessions.isEmpty()) return StreakData(0, 0)

        // Get all unique dates with completed sessions
        val studyDates = sessions
            .filter { it.completed }
            .map { it.timestamp.toLocalDate() }
            .distinct()
            .sortedDescending()

        if (studyDates.isEmpty()) return StreakData(0, 0)

        // Calculate current streak
        var currentStreak = 0
        var checkDate = today

        // Check if studied today or yesterday (allow 1-day gap)
        if (studyDates.contains(today)) {
            currentStreak = 1
            checkDate = today.minusDays(1)
        } else if (studyDates.contains(today.minusDays(1))) {
            currentStreak = 1
            checkDate = today.minusDays(2)
        } else {
            // Streak broken
            currentStreak = 0
        }

        // Continue counting backwards
        while (studyDates.contains(checkDate)) {
            currentStreak++
            checkDate = checkDate.minusDays(1)
        }

        // Calculate longest streak ever
        var longestStreak = 0
        var tempStreak = 0
        var previousDate: LocalDate? = null

        studyDates.sortedDescending().forEach { date ->
            if (previousDate == null || previousDate!!.minusDays(1) == date) {
                tempStreak++
                longestStreak = maxOf(longestStreak, tempStreak)
            } else {
                tempStreak = 1
            }
            previousDate = date
        }

        return StreakData(currentStreak, longestStreak)
    }
}

/* ================ EXTENSION ================ */

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

/* ================ DATA CLASSES ================ */

data class StreakData(
    val current: Int,
    val longest: Int
)