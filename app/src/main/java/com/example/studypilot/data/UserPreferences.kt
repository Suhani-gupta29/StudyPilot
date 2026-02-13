package com.example.studypilot.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.studypilot.ui.casual.CasualTask
import com.example.studypilot.ui.home.FocusSession
import com.example.studypilot.ui.mode.CasualModePreferences
import com.example.studypilot.ui.mode.ExamModePreferences
import com.example.studypilot.ui.mode.FocusModePreferences
import com.example.studypilot.ui.mode.StudyMode
import com.example.studypilot.ui.shared.FocusSubject
import com.example.studypilot.ui.shared.FocusTask
import com.example.studypilot.ui.shared.StudySession
import com.example.studypilot.ui.shared.Subject

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey
    val userId: String,
    val selectedMode: StudyMode,
    @Embedded(prefix = "exam_")
    val examPreferences: ExamModePreferences,
    @Embedded(prefix = "focus_")
    val focusPreferences: FocusModePreferences,
    @Embedded(prefix = "casual_")
    val casualPreferences: CasualModePreferences,
    val lastAccessed: Long,
    val lastRoute: String? = null,


    // Restored Fields
    val examName: String? = null,
    val examDate: Long? = null,
    val planStartDate: Long? = null,
    val examSubjects: List<Subject> = emptyList(),
    val casualSubjects: List<Subject> = emptyList(),
    val dailyPlan: List<StudySession> = emptyList(),
    val dailyPlanDate: String? = null,
    val tasks: List<FocusTask> = emptyList(),
    val casualTasks: List<CasualTask> = emptyList(),
    val planType: String? = null,
    val excludeSunday: Boolean? = false,
    val focusSubjects: List<FocusSubject> = emptyList(),

    // Focus Mode Persistence Fields
    val focusDailyPlan: List<StudySession> = emptyList(),
    val focusDailyPlanDate: String? = null,


    val focusPlanDate: String = "",
    val casualDailyPlan: List<StudySession> = emptyList(),
    val casualPlanDate: String = "",
    val exemptedSessions: Map<String, List<Int>> = emptyMap()
)