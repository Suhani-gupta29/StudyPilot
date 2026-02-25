package com.example.studypilot.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.studypilot.MainActivity
import com.example.studypilot.R

/**
 * StudyPilotNotificationManager
 *
 * Central helper to post all behavior-based notifications in Study Pilot.
 * Call createChannels() once from Application.onCreate().
 */
object StudyPilotNotificationManager {

    // ── Channel IDs ──────────────────────────────────────────────────────────
    const val CHANNEL_TIMER      = "sp_timer"       // Break over / session reminders
    const val CHANNEL_SUBJECT    = "sp_subject"     // Neglected subject warnings
    const val CHANNEL_GOAL       = "sp_goal"        // Daily / streak goal nudges
    const val CHANNEL_MOTIVATION = "sp_motivation"  // Wins, personal bests, encouragement

    // ── Notification IDs (unique per type so they replace themselves) ────────
    private const val NOTIF_BREAK_OVER          = 1001
    private const val NOTIF_SESSION_COMPLETE     = 1002
    private const val NOTIF_LONG_STUDY_NO_BREAK  = 1003
    private const val NOTIF_NEGLECTED_SUBJECT    = 2001
    private const val NOTIF_SUBJECT_IMBALANCE    = 2002
    private const val NOTIF_DAILY_GOAL_NUDGE     = 3001
    private const val NOTIF_STREAK_AT_RISK       = 3002
    private const val NOTIF_GOAL_MET             = 3003
    private const val NOTIF_PERSONAL_BEST        = 4001

    // ── Channel Setup ────────────────────────────────────────────────────────

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        listOf(
            NotificationChannel(CHANNEL_TIMER,      "Timer & Sessions",  NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Break over and session start reminders"
            },
            NotificationChannel(CHANNEL_SUBJECT,    "Subject Insights",  NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Alerts when a subject is being neglected or over-studied"
            },
            NotificationChannel(CHANNEL_GOAL,       "Goals & Streaks",   NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Daily goal nudges and streak at-risk warnings"
            },
            NotificationChannel(CHANNEL_MOTIVATION, "Achievements",      NotificationManager.IMPORTANCE_LOW).apply {
                description = "Personal bests and milestone celebrations"
            }
        ).forEach { nm.createNotificationChannel(it) }
    }

    // ── Internal builder ─────────────────────────────────────────────────────

    private fun buildNotification(
        context: Context,
        channelId: String,
        title: String,
        body: String,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT
    ): NotificationCompat.Builder {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // replace with your study icon
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setContentIntent(pi)
            .setAutoCancel(true)
    }

    private fun post(context: Context, id: Int, builder: NotificationCompat.Builder) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(id, builder.build())
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PUBLIC NOTIFICATION FUNCTIONS  — call these from your ViewModels / Service
    // ════════════════════════════════════════════════════════════════════════

    // ── 1. Break is over — time to start next session ────────────────────────
    /**
     * Call when the break timer finishes in your session/timer screen.
     * Trigger: BreakTimer countdown reaches 0.
     */
    fun notifyBreakOver(context: Context, nextSubject: String) {
        val n = buildNotification(
            context, CHANNEL_TIMER,
            title = "⏰ Break's Over!",
            body  = "Ready to focus on $nextSubject? Let's go! 💪",
            priority = NotificationCompat.PRIORITY_HIGH
        )
        post(context, NOTIF_BREAK_OVER, n)
    }

    // ── 2. Session complete — take a break ───────────────────────────────────
    /**
     * Call right after a study session is saved as completed.
     * Trigger: Inside repository.saveSession() success callback or after
     *          SessionStatus flips to COMPLETED in HomeViewModel.
     */
    fun notifySessionComplete(context: Context, breakMinutes: Int) {
        val n = buildNotification(
            context, CHANNEL_TIMER,
            title = "✅ Session Done!",
            body  = "Solid work! Take a $breakMinutes-minute break — you earned it 🧘"
        )
        post(context, NOTIF_SESSION_COMPLETE, n)
    }

    // ── 3. Been studying too long without a break ────────────────────────────
    /**
     * Call from a periodic check (every 5 min) when user has been in an
     * active session for more than `thresholdMinutes` without completing it.
     * Trigger: SessionScreen ViewModel detects elapsed > threshold.
     */
    fun notifyLongStudyNoBreak(context: Context, elapsedMinutes: Int) {
        val n = buildNotification(
            context, CHANNEL_TIMER,
            title = "😮 Still Going?",
            body  = "You've been studying for $elapsedMinutes minutes straight. A short break will boost retention!"
        )
        post(context, NOTIF_LONG_STUDY_NO_BREAK, n)
    }

    // ── 4. Subject neglected for N days ─────────────────────────────────────
    /**
     * Call from BehaviorNotificationWorker (daily worker) after checking
     * AnalyticsViewModel's subjectStats.lastStudied timestamps.
     * Trigger: lastStudied > 3 days ago for any subject.
     */
    fun notifyNeglectedSubject(context: Context, subjectName: String, daysSince: Int) {
        val n = buildNotification(
            context, CHANNEL_SUBJECT,
            title = "📚 $subjectName Needs Attention",
            body  = "You haven't touched $subjectName in $daysSince days. Don't let it slip behind!"
        )
        post(context, NOTIF_NEGLECTED_SUBJECT + subjectName.hashCode(), n)
    }

    // ── 5. One subject monopolising study time ───────────────────────────────
    /**
     * Call from BehaviorNotificationWorker after analysing today's sessions.
     * Trigger: One subject > 70% of today's total study time AND there are 2+ subjects.
     */
    fun notifySubjectImbalance(context: Context, dominantSubject: String, percent: Int) {
        val n = buildNotification(
            context, CHANNEL_SUBJECT,
            title = "⚖️ Subject Imbalance Detected",
            body  = "$dominantSubject is taking up $percent% of your study time today. Your other subjects need some love too!"
        )
        post(context, NOTIF_SUBJECT_IMBALANCE, n)
    }

    // ── 6. Evening nudge — daily goal not yet met ────────────────────────────
    /**
     * Schedule via WorkManager at 8 PM daily.
     * Trigger: Completed sessions today < planned sessions AND time > 8 PM.
     */
    fun notifyDailyGoalNudge(context: Context, completedSessions: Int, totalSessions: Int) {
        val remaining = totalSessions - completedSessions
        val sessionText = if (remaining == 1) "session" else "sessions"
        val n = buildNotification(
            context, CHANNEL_GOAL,
            title = "🎯 Don't Break the Chain!",
            body  = "You still have $remaining $sessionText left today. Even one more session counts!"
        )
        post(context, NOTIF_DAILY_GOAL_NUDGE, n)
    }

    // ── 7. Streak at risk ────────────────────────────────────────────────────
    /**
     * Schedule via WorkManager at 9 PM daily.
     * Trigger: No completed session today AND user has a streak > 0.
     */
    fun notifyStreakAtRisk(context: Context, streakDays: Int) {
        val n = buildNotification(
            context, CHANNEL_GOAL,
            title = "🔥 Your $streakDays-Day Streak Is at Risk!",
            body  = "Study for just 10 minutes to keep your streak alive. Don't let it end tonight!",
            priority = NotificationCompat.PRIORITY_HIGH
        )
        post(context, NOTIF_STREAK_AT_RISK, n)
    }

    // ── 8. Daily goal achieved ───────────────────────────────────────────────
    /**
     * Call when the last planned session for the day is completed.
     * Trigger: completedSessions == totalSessions in HomeViewModel.
     */
    fun notifyDailyGoalMet(context: Context) {
        val n = buildNotification(
            context, CHANNEL_MOTIVATION,
            title = "🌟 Daily Goal Smashed!",
            body  = "You completed all your sessions for today. Incredible consistency! 🎉"
        )
        post(context, NOTIF_GOAL_MET, n)
    }

    // ── 9. Personal best session ─────────────────────────────────────────────
    /**
     * Call when a session's elapsedSeconds exceeds the user's previous longest.
     * Trigger: After session save, compare with AnalyticsViewModel's max session time.
     */
    fun notifyPersonalBest(context: Context, minutes: Int) {
        val n = buildNotification(
            context, CHANNEL_MOTIVATION,
            title = "🏆 New Personal Best!",
            body  = "You just studied for $minutes minutes — your longest session ever! Keep it up! 🚀"
        )
        post(context, NOTIF_PERSONAL_BEST, n)
    }

    // ── 10. Cancel all notifications (e.g., user signs out) ─────────────────
    fun cancelAll(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancelAll()
    }
}