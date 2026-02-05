
package com.example.studypilot.ui.casual

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.studypilot.ui.shared.Difficulty
import com.example.studypilot.ui.shared.Priority

enum class CasualSessionStatus { COMPLETED, CURRENT, UPCOMING }
enum class CasualAlertType { MOTIVATIONAL, AWARENESS }

@Entity(tableName = "casual_details")
data class CasualDetails(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tasks: List<CasualTask>,
)

data class CasualSession(
    val sessionNumber: Int,
    val subjectName: String,
    val duration: Int,
    val status: CasualSessionStatus,
    val subjectInfo: String? = null
)

data class CasualMetrics(
    val completedToday: Int,
    val pendingToday: Int,
    val studyStreak: Int
)

data class CasualAlert(
    val message: String,
    val type: CasualAlertType
)

data class CasualTask(
    val name: String,
    val relatedSubject: String?,
    val dueDate: Long?
)
