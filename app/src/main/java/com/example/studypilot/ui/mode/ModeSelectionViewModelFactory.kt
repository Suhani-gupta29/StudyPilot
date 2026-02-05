package com.example.studypilot.ui.mode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.studypilot.data.UserPreferencesRepository

class ModeSelectionViewModelFactory(
    private val repository: UserPreferencesRepository,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ModeSelectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ModeSelectionViewModel(repository, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
