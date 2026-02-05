package com.example.studypilot.ui.session

data class SessionUiState(
    val subjectName: String ,
    val modeName: String ,
    val timerText: String ,
    val isRunning: Boolean,
    val remainingTime: Int,
    val canEndSession: Boolean = false,
    val sessionSaved: Boolean = false
)
