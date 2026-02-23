package com.example.studypilot.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.studypilot.data.SessionRepository

class SessionViewModelFactory(
    private val subject: String,
    private val mode: String,
    private val initialMinutes: Int,
    private val userId: String,
    private val repository: SessionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SessionViewModel(subject, mode, initialMinutes, userId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}