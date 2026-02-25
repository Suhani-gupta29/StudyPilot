package com.example.studypilot.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.studypilot.data.UserPreferencesRepository
import com.example.studypilot.ui.auth.AuthState
import com.example.studypilot.ui.auth.AuthViewModel
import com.example.studypilot.ui.mode.CasualSessionLength
import com.example.studypilot.ui.mode.ExamSessionLength
import com.example.studypilot.ui.mode.FocusSessionLength
import com.example.studypilot.ui.mode.StudyMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authViewModel: AuthViewModel
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsState())
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadSettings()
    }

    // Called from the screen to inject the active mode from homeViewModel
    fun setActiveMode(mode: StudyMode) {
        if (_uiState.value.activeMode != mode) {
            _uiState.value = _uiState.value.copy(activeMode = mode)
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val authState = authViewModel.authState.value
                if (authState is AuthState.Authenticated) {
                    val email = authState.email ?: ""
                    val derivedName = email
                        .substringBefore("@")
                        .replace(".", " ")
                        .split(" ")
                        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

                    val prefs = withContext(Dispatchers.IO) {
                        userPreferencesRepository.getUserPreferences(authState.uid).first()
                    }

                    if (prefs != null) {
                        val savedName = if (!prefs.displayName.isNullOrBlank()) prefs.displayName else derivedName
                        _uiState.value = _uiState.value.copy(
                            email = email,
                            displayName = savedName,
                            pendingDisplayName = savedName,
                            examDailyStudyHours = prefs.examPreferences.dailyStudyHours,
                            examSessionLength = prefs.examPreferences.sessionLength,
                            focusDailyStudyHours = prefs.focusPreferences.dailyStudyHours,
                            focusSessionLength = prefs.focusPreferences.sessionLength,
                            casualDailyStudyHours = prefs.casualPreferences.dailyStudyHours,
                            casualSessionLength = prefs.casualPreferences.sessionLength,
                            examDate = prefs.examDate,
                            examName = prefs.examName,
                            isLoading = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            email = email,
                            displayName = derivedName,
                            pendingDisplayName = derivedName,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load settings"
                )
            }
        }
    }

    // ── Exam preferences ──────────────────────────────────────────────────────
    fun onExamDailyHoursChanged(hours: Float) {
        _uiState.value = _uiState.value.copy(examDailyStudyHours = hours)
    }

    fun onExamSessionLengthChanged(length: ExamSessionLength) {
        _uiState.value = _uiState.value.copy(examSessionLength = length)
    }

    // ── Focus preferences ─────────────────────────────────────────────────────
    fun onFocusDailyHoursChanged(hours: Float) {
        _uiState.value = _uiState.value.copy(focusDailyStudyHours = hours)
    }

    fun onFocusSessionLengthChanged(length: FocusSessionLength) {
        _uiState.value = _uiState.value.copy(focusSessionLength = length)
    }

    // ── Casual preferences ────────────────────────────────────────────────────
    fun onCasualDailyHoursChanged(hours: Float) {
        _uiState.value = _uiState.value.copy(casualDailyStudyHours = hours)
    }

    fun onCasualSessionLengthChanged(length: CasualSessionLength) {
        _uiState.value = _uiState.value.copy(casualSessionLength = length)
    }

    // ── Save — only saves the currently active mode's prefs ──────────────────
    fun saveStudyPreferences() {
        viewModelScope.launch {
            val authState = authViewModel.authState.value
            if (authState !is AuthState.Authenticated) return@launch
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                withContext(Dispatchers.IO) {
                    val currentPrefs = userPreferencesRepository
                        .getUserPreferences(authState.uid).first() ?: return@withContext
                    val state = _uiState.value

                    val updatedPrefs = when (state.activeMode) {
                        StudyMode.EXAM -> currentPrefs.copy(
                            examPreferences = currentPrefs.examPreferences.copy(
                                dailyStudyHours = state.examDailyStudyHours,
                                sessionLength = state.examSessionLength
                            ),
                            lastAccessed = System.currentTimeMillis()
                        )
                        StudyMode.FOCUS -> currentPrefs.copy(
                            focusPreferences = currentPrefs.focusPreferences.copy(
                                dailyStudyHours = state.focusDailyStudyHours,
                                sessionLength = state.focusSessionLength
                            ),
                            lastAccessed = System.currentTimeMillis()
                        )
                        StudyMode.CASUAL -> currentPrefs.copy(
                            casualPreferences = currentPrefs.casualPreferences.copy(
                                dailyStudyHours = state.casualDailyStudyHours,
                                sessionLength = state.casualSessionLength
                            ),
                            lastAccessed = System.currentTimeMillis()
                        )
                    }
                    userPreferencesRepository.saveUserPreferences(updatedPrefs)
                }
                _uiState.value = _uiState.value.copy(isSaving = false)
                _effects.send(SettingsEffect.ShowSnackbar("Preferences saved"))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Failed to save preferences"
                )
            }
        }
    }

    // ── Profile name ──────────────────────────────────────────────────────────
    fun openNameEditDialog() {
        _uiState.value = _uiState.value.copy(
            showNameEditDialog = true,
            pendingDisplayName = _uiState.value.displayName,
            nameError = null
        )
    }

    fun cancelNameEdit() {
        _uiState.value = _uiState.value.copy(showNameEditDialog = false, nameError = null)
    }

    fun onPendingNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(
            pendingDisplayName = name,
            nameError = if (name.isBlank()) "Name cannot be empty" else null
        )
    }

    fun saveDisplayName() {
        val name = _uiState.value.pendingDisplayName.trim()
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(nameError = "Name cannot be empty")
            return
        }
        _uiState.value = _uiState.value.copy(
            displayName = name,
            showNameEditDialog = false,
            nameError = null
        )
        viewModelScope.launch {
            try {
                val authState = authViewModel.authState.value
                if (authState is AuthState.Authenticated) {
                    withContext(Dispatchers.IO) {
                        val currentPrefs = userPreferencesRepository
                            .getUserPreferences(authState.uid).first()
                        if (currentPrefs != null) {
                            userPreferencesRepository.saveUserPreferences(
                                currentPrefs.copy(
                                    displayName = name,
                                    lastAccessed = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Failed to persist display name", e)
            }
            _effects.send(SettingsEffect.ShowSnackbar("Name updated"))
        }
    }

    // ── Exam date ─────────────────────────────────────────────────────────────
    fun openExamDateDialog() {
        _uiState.value = _uiState.value.copy(showExamDateDialog = true)
    }

    fun cancelExamDateEdit() {
        _uiState.value = _uiState.value.copy(showExamDateDialog = false)
    }

    fun saveExamDate(newDateMillis: Long) {
        _uiState.value = _uiState.value.copy(
            examDate = newDateMillis,
            showExamDateDialog = false
        )
        viewModelScope.launch {
            try {
                val authState = authViewModel.authState.value
                if (authState is AuthState.Authenticated) {
                    withContext(Dispatchers.IO) {
                        val currentPrefs = userPreferencesRepository
                            .getUserPreferences(authState.uid).first()
                        if (currentPrefs != null) {
                            userPreferencesRepository.saveUserPreferences(
                                currentPrefs.copy(
                                    examDate = newDateMillis,
                                    // Reset any "exam over" dialog flag so the user
                                    // gets re-notified at the correct new date.
                                    examOverDialogShownForExamDate = null,
                                    lastAccessed = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Failed to persist exam date", e)
            }
            _effects.send(SettingsEffect.ShowSnackbar("Exam date updated"))
        }
    }

    // ── Sign out ──────────────────────────────────────────────────────────────
    fun onSignOutClicked() {
        _uiState.value = _uiState.value.copy(showSignOutDialog = true)
    }

    fun cancelSignOut() {
        _uiState.value = _uiState.value.copy(showSignOutDialog = false)
    }

    fun confirmSignOut() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showSignOutDialog = false)
            authViewModel.logout(userPreferencesRepository)
            _effects.send(SettingsEffect.NavigateToLogin)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    // ── Factory ───────────────────────────────────────────────────────────────
    class Factory(
        private val userPreferencesRepository: UserPreferencesRepository,
        private val authViewModel: AuthViewModel
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(userPreferencesRepository, authViewModel) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: $modelClass")
        }
    }
}