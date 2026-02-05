package com.example.studypilot.ui.mode

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studypilot.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ModeSelectionViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModeSelectionState())
    val uiState: StateFlow<ModeSelectionState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.getStudyModePreferences(userId).collect { prefs ->
                if (prefs != null) {
                    _uiState.value = prefs
                }
            }
        }
    }

    fun selectMode(mode: StudyMode) {
        _uiState.update { it.copy(selectedMode = mode) }
    }

    fun updateExamDailyStudyHours(hours: Float) {
        _uiState.update { it.copy(examPreferences = it.examPreferences.copy(dailyStudyHours = hours)) }
    }

    fun updateExamSessionLength(length: ExamSessionLength) {
        _uiState.update { it.copy(examPreferences = it.examPreferences.copy(sessionLength = length)) }
    }

    fun updateExamBreakPreference(preference: ExamBreakPreference) {
        _uiState.update { it.copy(examPreferences = it.examPreferences.copy(breakPreference = preference)) }
    }

    fun updateExamNotificationPreference(preference: ExamNotificationPreference) {
        _uiState.update { it.copy(examPreferences = it.examPreferences.copy(notificationPreference = preference)) }
    }

    fun updateFocusDailyStudyHours(hours: Float) {
        _uiState.update { it.copy(focusPreferences = it.focusPreferences.copy(dailyStudyHours = hours)) }
    }

    fun updateFocusSessionLength(length: FocusSessionLength) {
        _uiState.update { it.copy(focusPreferences = it.focusPreferences.copy(sessionLength = length)) }
    }

    fun updateFocusBreakPreference(preference: FocusBreakPreference) {
        _uiState.update { it.copy(focusPreferences = it.focusPreferences.copy(breakPreference = preference)) }
    }

    fun updateFocusNotificationPreferences(type: FocusNotificationType, isEnabled: Boolean) {
        val currentPrefs = _uiState.value.focusPreferences.notificationPreferences
        val newPrefs = if (isEnabled) currentPrefs + type else currentPrefs - type
        _uiState.update { it.copy(focusPreferences = it.focusPreferences.copy(notificationPreferences = newPrefs)) }
    }

    fun updateCasualDailyStudyHours(hours: Float) {
        _uiState.update { it.copy(casualPreferences = it.casualPreferences.copy(dailyStudyHours = hours)) }
    }

    fun updateCasualSessionLength(length: CasualSessionLength) {
        _uiState.update { it.copy(casualPreferences = it.casualPreferences.copy(sessionLength = length)) }
    }

    fun updateCasualNotificationsEnabled(enabled: Boolean) {
        _uiState.update { it.copy(casualPreferences = it.casualPreferences.copy(notificationsEnabled = enabled)) }
    }

    fun savePreferences() {
        viewModelScope.launch {
            userPreferencesRepository.saveStudyModePreferences(userId, _uiState.value)
            Log.d("ModeSelectionVM", "Saved study mode preferences for user=$userId: ${_uiState.value}")
        }
    }
}