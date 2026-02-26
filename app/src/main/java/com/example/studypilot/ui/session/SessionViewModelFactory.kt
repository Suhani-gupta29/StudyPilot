package com.example.studypilot.ui.session

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.studypilot.data.SessionRepository


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