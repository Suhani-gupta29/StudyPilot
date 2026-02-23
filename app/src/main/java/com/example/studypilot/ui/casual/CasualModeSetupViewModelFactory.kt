package com.example.studypilot.ui.casual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.studypilot.data.UserPreferencesRepository
import com.example.studypilot.ui.auth.AuthViewModel

class CasualModeSetupViewModelFactory(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authViewModel: AuthViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CasualModeSetupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CasualModeSetupViewModel(userPreferencesRepository, authViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
