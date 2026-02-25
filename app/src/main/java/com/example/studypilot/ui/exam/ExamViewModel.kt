package com.example.studypilot.ui.exam

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.studypilot.data.UserPreferences
import com.example.studypilot.data.UserPreferencesRepository
import com.example.studypilot.ui.auth.AuthViewModel
import com.example.studypilot.ui.auth.AuthState
import com.example.studypilot.ui.mode.CasualModePreferences
import com.example.studypilot.ui.mode.ExamModePreferences
import com.example.studypilot.ui.mode.FocusModePreferences
import com.example.studypilot.ui.mode.StudyMode
import com.example.studypilot.ui.shared.Difficulty
import com.example.studypilot.ui.shared.Priority
import com.example.studypilot.ui.shared.Subject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "ExamViewModel"

data class ExamInputState(
    val examName: String = "",
    val examDate: Long? = null,
    val subjects: List<Subject> = listOf(Subject()),
)

class ExamViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authViewModel: AuthViewModel
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExamInputState())
    val uiState = _uiState.asStateFlow()

    fun onExamNameChange(name: String) {
        _uiState.value = _uiState.value.copy(examName = name)
    }

    fun onExamDateChange(date: Long) {
        Log.d(TAG, "onExamDateChange: $date")
        _uiState.value = _uiState.value.copy(examDate = date)
    }

    fun addSubject() {
        val subjects = _uiState.value.subjects.toMutableList()
        subjects.add(Subject())
        Log.d(TAG, "addSubject: total=${subjects.size}")
        _uiState.value = _uiState.value.copy(subjects = subjects)
    }

    fun removeSubject(index: Int) {
        val subjects = _uiState.value.subjects.toMutableList()
        if (subjects.size > 1) {
            subjects.removeAt(index)
            Log.d(TAG, "removeSubject: removedIndex=$index total=${subjects.size}")
            _uiState.value = _uiState.value.copy(subjects = subjects)
        }
    }

    fun onSubjectNameChange(index: Int, name: String) {
        Log.d(TAG, "onSubjectNameChange: index=$index name=$name")
        val subjects = _uiState.value.subjects.toMutableList()
        subjects[index] = subjects[index].copy(name = name)
        _uiState.value = _uiState.value.copy(subjects = subjects)
    }

    fun onPriorityChange(index: Int, priority: Priority) {
        Log.d(TAG, "onPriorityChange: index=$index priority=$priority")
        val subjects = _uiState.value.subjects.toMutableList()
        subjects[index] = subjects[index].copy(priority = priority)
        _uiState.value = _uiState.value.copy(subjects = subjects)
    }

    fun onDifficultyChange(index: Int, difficulty: Difficulty) {
        Log.d(TAG, "onDifficultyChange: index=$index difficulty=$difficulty")
        val subjects = _uiState.value.subjects.toMutableList()
        subjects[index] = subjects[index].copy(difficulty = difficulty)
        _uiState.value = _uiState.value.copy(subjects = subjects)
    }

    fun saveExamDetails() {
        viewModelScope.launch {
            val authState = authViewModel.authState.first()
            if (authState is AuthState.Authenticated) {

                // Trim subject names and filter blanks
                val trimmed = _uiState.value.subjects.map { it.copy(name = it.name.trim()) }
                val newSubjects = trimmed.filter { it.name.isNotBlank() }

                // Use exactly what the user entered; fall back to placeholder only if truly empty
                val subjectsToSave = newSubjects.ifEmpty {
                    listOf(Subject("General Study", Priority.Low, Difficulty.Medium))
                }

                Log.d(TAG, "Saving exam details: name=${_uiState.value.examName}, date=${_uiState.value.examDate}, subjects=$subjectsToSave")

                val currentPrefs =
                    userPreferencesRepository.getUserPreferences(authState.uid).first()
                        ?: UserPreferences(
                            userId = authState.uid,
                            selectedMode = StudyMode.EXAM,
                            examPreferences = ExamModePreferences(),
                            focusPreferences = FocusModePreferences(),
                            casualPreferences = CasualModePreferences(),
                            lastAccessed = System.currentTimeMillis()
                        )

                // REPLACE exam subjects with exactly what the user entered on this screen.
                // This is a deliberate new-exam setup — the new plan must be built from
                // these subjects only, not from any previously saved list.
                //
                // Clear dailyPlan + dailyPlanDate so HomeViewModel sees no valid cached plan
                // and regenerates from scratch using subjectsToSave.
                //
                // Clear exemptedSessions because they reference sessions from the old plan.
                // Reset examOverDialogShownForExamDate so the dialog triggers correctly
                // at the new exam date if one is set.
                val updatedPrefs = currentPrefs.copy(
                    examName = _uiState.value.examName,
                    examDate = _uiState.value.examDate,
                    planStartDate = System.currentTimeMillis(),
                    examSubjects = subjectsToSave,
                    selectedMode = StudyMode.EXAM,
                    dailyPlan = emptyList(),
                    dailyPlanDate = null,
                    exemptedSessions = emptyMap(),
                    examOverDialogShownForExamDate = null,
                    lastAccessed = System.currentTimeMillis()
                )

                userPreferencesRepository.saveUserPreferences(updatedPrefs)

                val persisted = userPreferencesRepository.getUserPreferences(authState.uid).first()
                Log.d(TAG, "Persisted prefs examSubjects=${persisted?.examSubjects}, dailyPlanDate=${persisted?.dailyPlanDate}")
            }
        }
    }
}

class ExamViewModelFactory(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authViewModel: AuthViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExamViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExamViewModel(userPreferencesRepository, authViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}