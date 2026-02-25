package com.example.studypilot.notifications

import android.content.Context
import android.util.Log

/**
 * SessionNotificationHelper
 *
 * Thin stateful helper to be held inside your SessionViewModel (or wherever
 * your study timer lives). Tracks elapsed time and fires the right notification
 * at the right moment during an active session.
 *
 * USAGE in your SessionViewModel:
 *
 *   private val notifHelper = SessionNotificationHelper()
 *
 *   // Call every tick (every second) while session is running:
 *   fun onTimerTick(context: Context, elapsedSeconds: Int, breakDurationMin: Int, nextSubject: String) {
 *       notifHelper.onTick(context, elapsedSeconds, breakDurationMin, nextSubject)
 *   }
 *
 *   // Call when the user manually ends or completes a session:
 *   fun onSessionCompleted(context: Context, elapsedSeconds: Int, longestSessionEverSeconds: Int) {
 *       notifHelper.onSessionEnded(context, elapsedSeconds, longestSessionEverSeconds)
 *   }
 *
 *   // Call when break starts (e.g. after session saved):
 *   fun startBreak(context: Context, breakMinutes: Int) {
 *       notifHelper.startBreak(breakMinutes)
 *       // Your existing break timer logic ...
 *   }
 *
 *   // Call every tick while break is running:
 *   fun onBreakTick(context: Context, remainingSeconds: Int, nextSubject: String) {
 *       notifHelper.onBreakTick(context, remainingSeconds, nextSubject)
 *   }
 *
 *   // Reset between sessions:
 *   fun resetNotifState() { notifHelper.reset() }
 */
class SessionNotificationHelper {

    companion object {
        private const val TAG = "SessionNotifHelper"

        // Fire "you've been going too long" notification after this many minutes
        private const val LONG_STUDY_THRESHOLD_MINUTES = 1

        // Only fire the long-study notification once per session
    }

    // ── State ────────────────────────────────────────────────────────────────
    private var longStudyNotifFired = false
    private var breakTotalSeconds   = 0
    private var breakNotifFired     = false

    // ════════════════════════════════════════════════════════════════════════
    //  STUDY PHASE
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Call every second while the study timer is running.
     *
     * @param elapsedSeconds   how many seconds have elapsed in this session
     * @param breakDurationMin planned break duration (from user prefs) — shown in session-done notification
     * @param nextSubject      subject name of the NEXT session in today's plan
     */
    fun onStudyTick(
        context: Context,
        elapsedSeconds: Int,
        breakDurationMin: Int,
        nextSubject: String
    ) {
        val elapsedMinutes = elapsedSeconds / 60

        // Fire "too long without a break" once per session
        if (!longStudyNotifFired && elapsedMinutes >= LONG_STUDY_THRESHOLD_MINUTES) {
            Log.d(TAG, "Firing long-study notification at ${elapsedMinutes}min")
            StudyPilotNotificationManager.notifyLongStudyNoBreak(context, elapsedMinutes)
            longStudyNotifFired = true
        }
    }

    /**
     * Call when the user finishes/saves a session.
     *
     * @param elapsedSeconds           total seconds studied in this session
     * @param longestSessionEverSeconds the previous personal best (from Analytics)
     * @param breakDurationMin         break length to tell the user to take
     */
    fun onSessionEnded(
        context: Context,
        elapsedSeconds: Int,
        longestSessionEverSeconds: Int,
        breakDurationMin: Int
    ) {
        val elapsedMin = elapsedSeconds / 60

        // Personal best?
        if (elapsedSeconds > longestSessionEverSeconds) {
            Log.d(TAG, "Personal best! $elapsedMin minutes")
            StudyPilotNotificationManager.notifyPersonalBest(context, elapsedMin)
        } else {
            // Regular session-complete notification
            StudyPilotNotificationManager.notifySessionComplete(context, breakDurationMin)
        }
    }

    /**
     * Call when all of today's planned sessions are completed.
     * (Check: completedCount == totalPlannedCount in HomeViewModel)
     */
    fun onAllSessionsComplete(context: Context) {
        Log.d(TAG, "All sessions done — firing daily goal met notification")
        StudyPilotNotificationManager.notifyDailyGoalMet(context)
    }

    // ════════════════════════════════════════════════════════════════════════
    //  BREAK PHASE
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Call when a break starts. Sets the total break duration for countdown tracking.
     */
    fun startBreak(breakMinutes: Int) {
        breakTotalSeconds = breakMinutes * 60
        breakNotifFired   = false
        Log.d(TAG, "Break started: ${breakMinutes}min")
    }

    /**
     * Call every second while the break timer is counting down.
     *
     * @param remainingSeconds seconds left in the break
     * @param nextSubject      the upcoming session's subject name
     */
    fun onBreakTick(context: Context, remainingSeconds: Int, nextSubject: String) {
        // Fire notification when break reaches exactly 0 (or goes negative by 1 tick)
        if (!breakNotifFired && remainingSeconds <= 0) {
            Log.d(TAG, "Break over — notifying to start $nextSubject")
            StudyPilotNotificationManager.notifyBreakOver(context, nextSubject)
            breakNotifFired = true
        }
    }

    // ── Reset between sessions ───────────────────────────────────────────────

    fun reset() {
        longStudyNotifFired = false
        breakTotalSeconds   = 0
        breakNotifFired     = false
    }
}