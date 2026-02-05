package com.example.studypilot.ui.focus

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.studypilot.ui.shared.Difficulty
import com.example.studypilot.ui.shared.Priority

// region Enums
enum class BreakType { Short, Long, Pomodoro }
enum class FocusSessionStatus { COMPLETED, CURRENT, UPCOMING }
enum class FocusAlertType { MOTIVATIONAL, AWARENESS }
enum class PlanType { Weekly, BiWeekly }
enum class TaskType { Report, Presentation, Assignment, Exam }
enum class Effort { Low, Medium, High }

// endregion

// region Data Models
data class FocusSubject(
    val name: String,
    val difficulty: Difficulty,
    val priority: Priority
)

data class FocusTask(
    val name: String,
    val type: TaskType,
    val effort: Effort,
    val dueDate: Long? = null,
    val relatedSubject: String? = null
)

@Entity(tableName = "focus_details")
data class FocusDetails(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjects: List<FocusSubject>,
    val tasks: List<FocusTask>,
    val planType: PlanType,
    val excludeSunday: Boolean,
    val dailyStudyHours: Int,
    val preferredSessionLength: Int,
    val breakPreference: BreakType,
    val notificationsEnabled: Boolean
)

data class FocusSession(
    val sessionNumber: Int,
    val subjectName: String,
    val duration: Int,
    val cognitiveLoadScore: Int,
    val status: FocusSessionStatus
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

