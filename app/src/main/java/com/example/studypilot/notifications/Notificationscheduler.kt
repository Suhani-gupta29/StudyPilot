package com.example.studypilot.notifications

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * NotificationScheduler
 *
 * Call scheduleAll() once after user signs in (from MainActivity or AuthViewModel).
 * Handles:
 *  - Daily behavior analysis worker (8 PM every day)
 *  - Streak-at-risk worker (9 PM every day, separate so it can be re-scheduled independently)
 */
object NotificationScheduler {

    private const val TAG = "NotifScheduler"
    private const val WORK_BEHAVIOR_DAILY = "work_behavior_daily"

    /**
     * Call this right after the user authenticates successfully.
     * Place this in MainActivity where you observe AuthState.Authenticated.
     *
     * Example (in MainActivity.kt inside collectLatest on authState):
     *   if (authState is AuthState.Authenticated) {
     *       NotificationScheduler.scheduleAll(context, authState.uid)
     *   }
     */
    fun scheduleAll(context: Context, userId: String) {
        scheduleDailyBehaviorWorker(context, userId)
        Log.d(TAG, "All notification workers scheduled for userId=$userId")
    }

    /**
     * Cancel all scheduled notification work (e.g., on sign-out).
     * Call from MainActivity where AuthState.Unauthenticated is observed.
     */
    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
        Log.d(TAG, "All notification workers cancelled")
    }

    // ── Daily behavior worker — fires at 8 PM ────────────────────────────────

    private fun scheduleDailyBehaviorWorker(context: Context, userId: String) {
        val delay = 60_000L // 8:00 PM

        val inputData = Data.Builder()
            .putString(BehaviorNotificationWorker.KEY_USER_ID, userId)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val request = PeriodicWorkRequestBuilder<BehaviorNotificationWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(WORK_BEHAVIOR_DAILY)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_BEHAVIOR_DAILY,
            ExistingPeriodicWorkPolicy.UPDATE, // Update if userId changes (re-login)
            request
        )

        Log.d(TAG, "Daily behavior worker scheduled with ${delay / 60_000} min delay")
    }

    // ── Calculate milliseconds until the next occurrence of HH:MM ───────────

    private fun calculateDelayUntil(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // If we're already past today's target time, schedule for tomorrow
        if (now.after(target)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}