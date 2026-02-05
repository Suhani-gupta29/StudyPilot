package com.example.studypilot.ui.analytics

import java.time.LocalDate

data class AnalyticsUiState(
    val dailySummary: DailySummaryUi,
    val weeklyStudy: List<DayStudyUi>,
    val subjectStats: List<SubjectStatUi>,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val thisWeekSeconds: Int = 0,
    val lastWeekSeconds: Int = 0,
    val weeklyChange: Float = 0f
)

data class DailySummaryUi(
    val totalStudySeconds: Int,
    val sessionCount: Int,
    val completionRate: Float // 0.0 to 1.0
)

data class DayStudyUi(
    val dayLabel: String,
    val studySeconds: Int,
    val date: LocalDate,
    val isFuture: Boolean = false
)

data class SubjectStatUi(
    val subjectName: String,
    val totalStudySeconds: Int,
    val sessionCount: Int,
    val completedSessions: Int,
    val averageSessionSeconds: Int,
    val completionRate: Float, // 0.0 to 1.0
    val percentage: Float, // 0.0 to 100.0
    val lastStudied: Long
)