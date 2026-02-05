package com.example.studypilot.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.studypilot.data.SessionRepository
import com.example.studypilot.data.UserPreferencesRepository

class AnalyticsViewModelFactory(
    private val sessionRepository: SessionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userId: String  // ADD THIS PARAMETER
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AnalyticsViewModel(sessionRepository, userPreferencesRepository, userId) as T  // PASS userId HERE
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}