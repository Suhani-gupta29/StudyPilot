package com.example.studypilot.ui.session

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.studypilot.data.SessionRepository


// ══════════════════════════════════════════════════════════════════════════════
//  UPDATED FACTORY
//
//  In your SessionScreen composable (inside NavHost in MainActivity), find
//  where you create the SessionViewModel and update it to pass the new params.
//
//  You need to pass:
//    - breakDurationMinutes  → read from userPrefs.examPreferences.breakPreference.minutes
//                              (or focusPreferences / casualPreferences depending on mode)
//    - nextSessionSubject    → the subject name of the NEXT UPCOMING session
//                              from homeViewModel.uiState.value.studySessions
//                              (or focusSessions / casualSessions)
//    - longestSessionEverSeconds → from analyticsViewModel or pass 0 for now
//
//  Example in MainActivity NavHost "session/{subject}/{mode}/{minutes}":
//
//    val uiState by homeViewModel.uiState.collectAsState()
//    val nextSubject = remember(uiState) {
//        // Find the first UPCOMING session after the current one
//        (uiState.studySessions ?: uiState.focusSessions?.map { ... } ?: emptyList())
//            .firstOrNull { it.status == SessionStatus.UPCOMING }
//            ?.subject ?: subject  // fall back to current subject
//    }
//    val breakMinutes = remember(uiState) {
//        when (uiState.selectedMode) {
//            StudyMode.EXAM   -> uiState.examDetails?.let {
//                                    userPreferencesRepository // you need to read from prefs here
//                                } ?: 5
//            StudyMode.FOCUS  -> 5   // replace with actual pref
//            StudyMode.CASUAL -> 5
//            else -> 5
//        }
//    }
// ══════════════════════════════════════════════════════════════════════════════

class SessionViewModelFactory(
    private val subject: String,
    private val mode: String,
    private val initialMinutes: Int,
    private val userId: String,
    private val repository: SessionRepository,
    private val context: Context,
    private val breakDurationMinutes: Int = 5,
    private val nextSessionSubject: String = subject,
    private val longestSessionEverSeconds: Int = 0
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SessionViewModel(
                subject                   = subject,
                mode                      = mode,
                initialMinutes            = initialMinutes,
                userId                    = userId,
                repository                = repository,
                context                   = context,
                breakDurationMinutes      = breakDurationMinutes,
                nextSessionSubject        = nextSessionSubject,
                longestSessionEverSeconds = longestSessionEverSeconds
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}