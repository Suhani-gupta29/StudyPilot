package com.example.studypilot.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studypilot.data.SessionRepository
import com.example.studypilot.data.StudySession
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
    private val repository: SessionRepository
) : ViewModel() {

    private val totalSeconds = initialMinutes * 60
    private var remainingSeconds = totalSeconds

    private var timerJob: Job? = null

    // ── FIX: guard so endSession() can never execute twice
    //    (auto-end when timer hits 0 and user tapping End at the same tick)
    private var sessionEnded = false

    private val _uiState = MutableStateFlow(
        SessionUiState(
            subjectName = subject,
            modeName = mode,
            timerText = formatTime(remainingSeconds),
            isRunning = false,
            remainingTime = initialMinutes,
            canEndSession = true,  // ← CHANGED: Always allow ending for demo
            sessionSaved = false   // ← flips true after DB write succeeds
        )
    )
    val uiState: StateFlow<SessionUiState> = _uiState

    fun toggleRunning() {
        if (_uiState.value.isRunning) pauseTimer() else startTimer()
    }

    private fun startTimer() {
        if (timerJob != null) return

        _uiState.update { it.copy(isRunning = true) }

        timerJob = viewModelScope.launch {
            while (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--

                _uiState.update {
                    it.copy(
                        timerText = formatTime(remainingSeconds),
                        canEndSession = true  // ← Always true for demo
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
        _uiState.update {
            it.copy(
                isRunning = false,
                canEndSession = true  // ← Always true for demo
            )
        }
    }

    fun endSession() {
        // ── FIX: guard — if already ended, do nothing
        if (sessionEnded) return
        sessionEnded = true

        android.util.Log.d("SessionViewModel", "endSession() called - sessionEnded flag set")

        timerJob?.cancel()
        timerJob = null

        val elapsed = totalSeconds - remainingSeconds

        // ── FIX: completed = true whenever endSession() runs.
        //    The user either pressed End consciously, or the timer hit zero.
        //    Both mean "this session is done".
        //    completed=false should only exist for crash-abandoned rows
        //    (where endSession never ran at all).
        val completed = true

        _uiState.update { it.copy(isRunning = false) }

        viewModelScope.launch {
            try {
                android.util.Log.d("SessionViewModel", "Creating session object - mode: ${_uiState.value.modeName}")

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

                android.util.Log.d("SessionViewModel", "Saving session to DB...")

                withContext(Dispatchers.IO) {
                    repository.saveSession(session)
                }

                android.util.Log.d("SessionViewModel", "Session saved successfully: completed=$completed, elapsed=${elapsed}s, remaining=${remainingSeconds}s, mode=${_uiState.value.modeName}")

                // ── Signal the composable that DB write is done.
                //    Composable watches this flag to auto-navigate back to home.
                _uiState.update { it.copy(sessionSaved = true) }

                android.util.Log.d("SessionViewModel", "sessionSaved flag set to true - should trigger navigation")

            } catch (e: Exception) {
                android.util.Log.e("SessionViewModel", "Failed to save session", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        android.util.Log.d("SessionViewModel", "ViewModel cleared, timer cancelled")
    }

    private fun formatTime(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }
}