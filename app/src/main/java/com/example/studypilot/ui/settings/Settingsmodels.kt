package com.example.studypilot.ui.settings

import com.example.studypilot.ui.mode.ExamSessionLength
import com.example.studypilot.ui.mode.FocusSessionLength
import com.example.studypilot.ui.mode.CasualSessionLength
import com.example.studypilot.ui.mode.StudyMode

data class SettingsState(
    // Profile
    val displayName: String = "",
    val email: String = "",
    val pendingDisplayName: String = "",
    val nameError: String? = null,

    // Active mode — read from homeViewModel, not toggled in settings
    val activeMode: StudyMode = StudyMode.FOCUS,

    // Study Preferences — only the active mode's values are shown/edited
    val examDailyStudyHours: Float = 4f,
    val examSessionLength: ExamSessionLength = ExamSessionLength.SIXTY,

    val focusDailyStudyHours: Float = 2f,
    val focusSessionLength: FocusSessionLength = FocusSessionLength.FORTY_FIVE,

    val casualDailyStudyHours: Float = 1f,
    val casualSessionLength: CasualSessionLength = CasualSessionLength.THIRTY,

    // UI feedback
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,

    // Exam date (only relevant in EXAM mode)
    val examDate: Long? = null,
    val examName: String? = null,

    // Dialogs
    val showSignOutDialog: Boolean = false,
    val showNameEditDialog: Boolean = false,
    val showExamDateDialog: Boolean = false,
)

sealed class SettingsEffect {
    object NavigateToLogin : SettingsEffect()
    data class ShowSnackbar(val message: String) : SettingsEffect()
}