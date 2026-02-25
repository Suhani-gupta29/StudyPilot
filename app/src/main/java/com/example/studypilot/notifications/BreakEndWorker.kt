package com.example.studypilot.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * BreakEndWorker
 *
 * A ONE-SHOT WorkManager worker that fires after the user's break duration
 * (read from their break preference) has elapsed.
 *
 * Since your app goes straight back to Home after a session ends, there is
 * no break countdown screen. Instead, we schedule this worker from inside
 * SessionViewModel.endSession() so it fires even if the app is closed.
 *
 * Flow:
 *   Session ends → endSession() called
 *       → scheduleBreakEnd(context, breakMinutes, nextSubject) called
 *           → WorkManager waits N minutes in background
 *               → BreakEndWorker fires notification:
 *                   "Break's over! Ready to start [nextSubject]? Let's go! 💪"
 */
class BreakEndWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "BreakEndWorker"

        const val KEY_NEXT_SUBJECT = "next_subject"

        // Unique work name — using this means a new session always replaces
        // the previous pending break notification (no stacking)
        private const val WORK_NAME = "break_end_notification"

        /**
         * Call this from SessionViewModel.endSession() right after saving to DB.
         *
         * @param context         application context
         * @param breakMinutes    the user's break preference in minutes
         * @param nextSubject     name of the next session's subject (shown in notification)
         */
        fun schedule(context: Context, breakMinutes: Int, nextSubject: String) {
            val data = Data.Builder()
                .putString(KEY_NEXT_SUBJECT, nextSubject)
                .build()

            val request = OneTimeWorkRequestBuilder<BreakEndWorker>()
                .setInitialDelay(breakMinutes.toLong(), TimeUnit.MINUTES)
                .setInputData(data)
                .addTag(WORK_NAME)
                .build()

            // REPLACE_EXISTING: if user ends another session during break,
            // the old break timer is cancelled and the new one starts fresh
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                androidx.work.ExistingWorkPolicy.REPLACE,
                request
            )

            Log.d(TAG, "Break end worker scheduled for $breakMinutes minutes. Next: $nextSubject")
        }

        /**
         * Call this if the user manually starts the next session before the
         * break timer fires (so they don't get a redundant notification).
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Break end worker cancelled (user started next session early)")
        }
    }

    override suspend fun doWork(): Result {
        val nextSubject = inputData.getString(KEY_NEXT_SUBJECT) ?: "your next session"

        Log.d(TAG, "Break over — firing notification for: $nextSubject")

        StudyPilotNotificationManager.notifyBreakOver(applicationContext, nextSubject)

        return Result.success()
    }
}