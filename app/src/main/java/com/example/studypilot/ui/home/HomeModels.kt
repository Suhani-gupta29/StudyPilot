package com.example.studypilot.ui.home

import com.example.studypilot.ui.casual.CasualTask
import com.example.studypilot.ui.shared.Difficulty
import com.example.studypilot.ui.shared.Priority
import com.example.studypilot.ui.shared.Subject

// --- New Data Models ---
enum class StudyPhase {
    SyllabusCompletion,
    Revision,
    Testing
}

data class ExamDetails(
    val examName: String,
    val examDate: Long,
    val subjects: List<Subject>,
    val daysRemaining: Long,
    val urgency: Urgency,
    val phase: StudyPhase
)

// --- Existing Data Models ---
data class AccountabilityMetrics(
    val completed: Int,
    val missed: Int,
    val backlog: Int
)

data class AlertMessage(
    val message: String,
    val type: AlertType
)

enum class Urgency { NORMAL, WARNING, CRITICAL }
enum class AlertType { MOTIVATIONAL, WARNING }
enum class FocusAlertType { MOTIVATIONAL, AWARENESS }

// --- Casual Mode --- //
data class CasualDetails(val tasks: List<CasualTask>)
data class CasualSession(val sessionNumber: Int, val subjectName: String, val duration: Int, val status: CasualSessionStatus)
enum class CasualSessionStatus { COMPLETED, CURRENT, UPCOMING }

data class CasualMetrics(val completedToday: Int, val pendingToday: Int, val studyStreak: Int)

