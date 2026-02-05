package com.example.studypilot.ui.shared

import java.util.UUID

// Enums for various models
enum class Priority { High, Medium, Low }
enum class Difficulty { Easy, Medium, Hard }
enum class TaskType { ASSIGNMENT, PRACTICE, PPT, TEST }
enum class Effort { High, Medium, Low }
enum class SessionStatus { COMPLETED, CURRENT, UPCOMING }

// Data class for a subject in Exam Mode
data class Subject(
    val name: String = "",
    val priority: Priority = Priority.Medium,
    val difficulty: Difficulty = Difficulty.Medium
)

// Data class for a subject in Focus Mode
data class FocusSubject(
    val name: String,
    val difficulty: Difficulty,
    val priority: Priority
)

// Data class for a task in Focus Mode
data class FocusTask(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: TaskType,
    val effort: Effort,
    val dueDate: Long?,
    val relatedSubject: String?
)

// Data class for a single study session in the daily plan
data class StudySession(
    val sessionId: String = UUID.randomUUID().toString(),
    val sessionNumber: Int,
    val subject: String,
    val durationMinutes: Int,
    var status: SessionStatus = SessionStatus.UPCOMING
)
