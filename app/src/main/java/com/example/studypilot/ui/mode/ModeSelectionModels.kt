package com.example.studypilot.ui.mode

enum class StudyMode { EXAM, FOCUS, CASUAL }
enum class ExamSessionLength(val minutes: Int) { FORTY_FIVE(45), SIXTY(60), NINETY(90) }
enum class ExamBreakPreference { FIVE_MIN_AFTER_EVERY_SESSION, TEN_MIN_AFTER_TWO_SESSIONS }
enum class ExamNotificationPreference { NORMAL, HIGH }
enum class FocusSessionLength(val minutes: Int) { THIRTY(30), FORTY_FIVE(45), SIXTY(60) }
enum class FocusBreakPreference { FIVE_MIN_AFTER_EACH_SESSION, TEN_MIN_AFTER_EACH_SESSION }
enum class FocusNotificationType { SESSION_REMINDERS, DAILY_SUMMARY }
enum class CasualSessionLength(val minutes: Int) { FIFTEEN(15), THIRTY(30), FORTY_FIVE(45) }

data class ExamModePreferences(
    val dailyStudyHours: Float = 4f,
    val sessionLength: ExamSessionLength = ExamSessionLength.SIXTY,
    val breakPreference: ExamBreakPreference = ExamBreakPreference.FIVE_MIN_AFTER_EVERY_SESSION,
    val notificationPreference: ExamNotificationPreference = ExamNotificationPreference.NORMAL
)

data class FocusModePreferences(
    val dailyStudyHours: Float = 2f,
    val sessionLength: FocusSessionLength = FocusSessionLength.FORTY_FIVE,
    val breakPreference: FocusBreakPreference = FocusBreakPreference.TEN_MIN_AFTER_EACH_SESSION,
    val notificationPreferences: Set<FocusNotificationType> = setOf(FocusNotificationType.SESSION_REMINDERS, FocusNotificationType.DAILY_SUMMARY)
)

data class CasualModePreferences(
    val dailyStudyHours: Float = 1f,
    val sessionLength: CasualSessionLength = CasualSessionLength.THIRTY,
    val notificationsEnabled: Boolean = true
)

data class ModeSelectionState(
    val selectedMode: StudyMode = StudyMode.FOCUS,
    val examPreferences: ExamModePreferences = ExamModePreferences(),
    val focusPreferences: FocusModePreferences = FocusModePreferences(),
    val casualPreferences: CasualModePreferences = CasualModePreferences()
)
