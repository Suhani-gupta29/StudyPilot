package com.example.studypilot.ui.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.studypilot.data.SessionRepository
import com.example.studypilot.data.UserPreferencesRepository
import com.example.studypilot.ui.auth.AuthViewModel

class SubjectsViewModelFactory(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authViewModel: AuthViewModel,
    private val sessionRepository: SessionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SubjectsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SubjectsViewModel(userPreferencesRepository, authViewModel, sessionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}