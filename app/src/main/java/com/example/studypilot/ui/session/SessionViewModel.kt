package com.example.studypilot.ui.session

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.studypilot.data.SessionRepository
import com.example.studypilot.data.StudySession
import com.example.studypilot.notifications.BreakEndWorker               // ← ADD
import com.example.studypilot.notifications.SessionNotificationHelper    // ← ADD
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SessionViewModel(
    subject: String,
    mode: String,
    initialMinutes: Int,
    private val userId: String,
    private val repository: SessionRepository,
    // ── ADD these 4 new parameters ───────────────────────────────────────────
    private val context: Context,
    private val breakDurationMinutes: Int = 1,         // from user's break preference
    private val nextSessionSubject: String = subject,  // next session in today's plan
    private val longestSessionEverSeconds: Int = 0     // from analytics, for personal best
    // ─────────────────────────────────────────────────────────────────────────
) : ViewModel() {

    private val totalSeconds = initialMinutes * 60
    private var remainingSeconds = totalSeconds
    private var timerJob: Job? = null
    private var sessionEnded = false

    // ── ADD: notification helper ─────────────────────────────────────────────
    private val notifHelper = SessionNotificationHelper()
    // ─────────────────────────────────────────────────────────────────────────

    val sessionId = "${userId}_${System.currentTimeMillis()}"

    private val _uiState = MutableStateFlow(
        SessionUiState(
            subjectName = subject,
            modeName = mode,
            timerText = formatTime(remainingSeconds),
            isRunning = false,
            remainingTime = initialMinutes,
            canEndSession = true,
            sessionSaved = false,
            sessionId = "${userId}_${System.currentTimeMillis()}"
        )
    )
    val uiState: StateFlow<SessionUiState> = _uiState

    fun toggleRunning() {
        if (_uiState.value.isRunning) pauseTimer() else startTimer()
    }

    private fun startTimer() {
        if (timerJob != null) return

        // ── ADD: cancel any pending break notification when user starts a new session
        BreakEndWorker.cancel(context)
        // ─────────────────────────────────────────────────────────────────────

        _uiState.update { it.copy(isRunning = true) }

        timerJob = viewModelScope.launch {
            while (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--

                val elapsed = totalSeconds - remainingSeconds

                // ── ADD: check every tick for "studied too long without break" ─
                notifHelper.onStudyTick(
                    context          = context,
                    elapsedSeconds   = elapsed,
                    breakDurationMin = breakDurationMinutes,
                    nextSubject      = nextSessionSubject
                )
                // ─────────────────────────────────────────────────────────────

                _uiState.update {
                    it.copy(
                        timerText = formatTime(remainingSeconds),
                        canEndSession = true
                    )
                }
            }
            // Timer hit zero naturally — end the session
            endSession()
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(isRunning = false, canEndSession = true) }
    }

    fun endSession() {
        if (sessionEnded) return
        sessionEnded = true

        android.util.Log.d("SessionViewModel", "endSession() called")

        timerJob?.cancel()
        timerJob = null

        val elapsed = totalSeconds - remainingSeconds
        val completed = true

        _uiState.update { it.copy(isRunning = false) }

        viewModelScope.launch {
            try {
                val session = StudySession(
                    userId = userId,
                    subjectName = _uiState.value.subjectName,
                    modeName = _uiState.value.modeName,
                    totalDurationSeconds = totalSeconds,
                    elapsedSeconds = elapsed,
                    remainingSeconds = remainingSeconds,
                    completed = completed,
                    timestamp = System.currentTimeMillis()
                )

                withContext(Dispatchers.IO) {
                    repository.saveSession(session)
                }

                android.util.Log.d("SessionViewModel", "Session saved: elapsed=${elapsed}s")

                // ── ADD: fire immediate "session done / personal best" notification
                notifHelper.onSessionEnded(
                    context                   = context,
                    elapsedSeconds            = elapsed,
                    longestSessionEverSeconds = longestSessionEverSeconds,
                    breakDurationMin          = breakDurationMinutes
                )

                // ── ADD: schedule break-end notification after N minutes ──────
                // This fires even after the user leaves the app / screen.
                // Uses the break preference the user set in Mode Selection.
                // Example: user set 5-min break → notification fires in 5 mins:
                //   "⏰ Break's Over! Ready to focus on [Math]? Let's go! 💪"
                BreakEndWorker.schedule(
                    context       = context,
                    breakMinutes  = breakDurationMinutes,
                    nextSubject   = nextSessionSubject
                )
                android.util.Log.d(
                    "SessionViewModel",
                    "Break notification scheduled for $breakDurationMinutes min. Next: $nextSessionSubject"
                )
                // ─────────────────────────────────────────────────────────────

                _uiState.update { it.copy(sessionSaved = true) }

            } catch (e: Exception) {
                android.util.Log.e("SessionViewModel", "Failed to save session", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        notifHelper.reset()
        android.util.Log.d("SessionViewModel", "ViewModel cleared")
    }

    private fun formatTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }
}

