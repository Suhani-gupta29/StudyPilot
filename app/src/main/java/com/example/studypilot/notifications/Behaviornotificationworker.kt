package com.example.studypilot.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.studypilot.StudyPilotApplication
import com.example.studypilot.ui.mode.StudyMode
import kotlinx.coroutines.flow.firstOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * BehaviorNotificationWorker
 *
 * Runs once daily (scheduled from NotificationScheduler).
 * Analyses user behaviour from the database and fires the appropriate
 * smart notifications without needing the app to be open.
 *
 * Checks performed (in order):
 *  1. Neglected subject (no session in 3+ days)
 *  2. Subject imbalance today (one subject > 70% of today's time)
 *  3. Daily goal nudge (evening — sessions incomplete)
 *  4. Streak at risk (no session today, streak > 0)
 */
class BehaviorNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "BehaviorNotifWorker"
        const val KEY_USER_ID = "user_id"
        private const val NEGLECT_THRESHOLD_DAYS = 3L
        private const val IMBALANCE_THRESHOLD_PERCENT = 70
    }

    override suspend fun doWork(): Result {
        val userId = inputData.getString(KEY_USER_ID) ?: return Result.failure()
        val app = applicationContext as StudyPilotApplication

        return try {
            val sessions = app.sessionRepository.getSessionsForUser(userId).firstOrNull()
                ?: return Result.success()
            val prefs = app.userPreferencesRepository.getUserPreferences(userId).firstOrNull()
                ?: return Result.success()

            val today = LocalDate.now()

            // ── 1. Neglected subject check ────────────────────────────────────
            val subjectLastStudied = sessions
                .filter { it.completed }
                .groupBy { it.subjectName }
                .mapValues { (_, sessionList) ->
                    sessionList.maxOf { it.timestamp }.toLocalDate()
                }

            // Get active subjects for the current mode
            val activeSubjects = when (prefs.selectedMode) {
                StudyMode.EXAM  -> prefs.examSubjects.map { it.name }
                StudyMode.FOCUS -> prefs.focusSubjects.map { it.name }
                StudyMode.CASUAL -> prefs.casualSubjects.map { it.name }
            }

            activeSubjects.forEach { subjectName ->
                val lastDate = subjectLastStudied[subjectName]
                if (lastDate != null) {
                    val daysSince = ChronoUnit.DAYS.between(lastDate, today)
                    if (daysSince >= NEGLECT_THRESHOLD_DAYS) {
                        Log.d(TAG, "Neglected subject: $subjectName ($daysSince days)")
                        StudyPilotNotificationManager.notifyNeglectedSubject(
                            applicationContext, subjectName, daysSince.toInt()
                        )
                    }
                }
                // If subject has never been studied, and was added more than 3 days ago — also notify
                // (We skip this check to avoid spamming new users on day 1)
            }

            // ── 2. Subject imbalance today ────────────────────────────────────
            val todaySessions = sessions.filter { it.timestamp.toLocalDate() == today }
            val totalTodaySeconds = todaySessions.sumOf { it.elapsedSeconds }

            if (totalTodaySeconds > 0 && todaySessions.map { it.subjectName }.distinct().size >= 2) {
                val bySubject = todaySessions.groupBy { it.subjectName }
                    .mapValues { (_, s) -> s.sumOf { it.elapsedSeconds } }
                val dominantEntry = bySubject.maxByOrNull { it.value }

                if (dominantEntry != null) {
                    val dominantPercent = (dominantEntry.value.toFloat() / totalTodaySeconds * 100).toInt()
                    if (dominantPercent >= IMBALANCE_THRESHOLD_PERCENT) {
                        Log.d(TAG, "Imbalance: ${dominantEntry.key} at $dominantPercent%")
                        StudyPilotNotificationManager.notifySubjectImbalance(
                            applicationContext, dominantEntry.key, dominantPercent
                        )
                    }
                }
            }

            // ── 3. Daily goal nudge ───────────────────────────────────────────
            // Calculate planned sessions same way as HomeViewModel
            val dailyHours = when (prefs.selectedMode) {
                StudyMode.EXAM  -> prefs.examPreferences.dailyStudyHours
                StudyMode.FOCUS -> prefs.focusPreferences.dailyStudyHours
                StudyMode.CASUAL -> prefs.casualPreferences.dailyStudyHours
            }
            val sessionLengthMin = when (prefs.selectedMode) {
                StudyMode.EXAM  -> prefs.examPreferences.sessionLength.minutes
                StudyMode.FOCUS -> prefs.focusPreferences.sessionLength.minutes
                StudyMode.CASUAL -> prefs.casualPreferences.sessionLength.minutes
            }.coerceAtLeast(1)

            val plannedSessions = (dailyHours * 60f / sessionLengthMin).toInt().coerceAtLeast(1)
            val completedToday = todaySessions.count { it.completed }

            if (completedToday < plannedSessions) {
                Log.d(TAG, "Goal nudge: $completedToday/$plannedSessions done")
                StudyPilotNotificationManager.notifyDailyGoalNudge(
                    applicationContext, completedToday, plannedSessions
                )
            }

            // ── 4. Streak at risk ─────────────────────────────────────────────
            val hasStudiedToday = todaySessions.any { it.completed }
            if (!hasStudiedToday) {
                // Calculate current streak
                val studyDates = sessions
                    .filter { it.completed }
                    .map { it.timestamp.toLocalDate() }
                    .distinct()
                    .sortedDescending()

                val streak = calculateStreak(studyDates, today)
                if (streak > 0) {
                    Log.d(TAG, "Streak at risk! Current streak: $streak days")
                    StudyPilotNotificationManager.notifyStreakAtRisk(applicationContext, streak)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Worker failed", e)
            Result.retry()
        }
    }

    private fun calculateStreak(sortedDates: List<LocalDate>, today: LocalDate): Int {
        if (sortedDates.isEmpty()) return 0
        // Streak counts from yesterday backwards (we already know today has no session)
        var streak = 0
        var check = today.minusDays(1)
        for (date in sortedDates) {
            if (date == check) {
                streak++
                check = check.minusDays(1)
            } else if (date.isBefore(check)) {
                break
            }
        }
        return streak
    }

    private fun Long.toLocalDate(): LocalDate =
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
}