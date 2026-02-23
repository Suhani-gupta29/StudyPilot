package com.example.studypilot.ui.casual

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studypilot.data.UserPreferences
import com.example.studypilot.data.UserPreferencesRepository
import com.example.studypilot.ui.auth.AuthViewModel
import com.example.studypilot.ui.auth.AuthState
import com.example.studypilot.ui.mode.CasualModePreferences
import com.example.studypilot.ui.mode.CasualSessionLength
import com.example.studypilot.ui.mode.ExamModePreferences
import com.example.studypilot.ui.mode.FocusModePreferences
import com.example.studypilot.ui.mode.StudyMode
import com.example.studypilot.ui.shared.Subject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

data class CasualModeSetupState(
    val subjects: List<Subject> = listOf(Subject("")),
    val tasks: List<CasualTask> = emptyList(),
    val dailyStudyHours: Float = 1f,
    val sessionLength: Int = 30
)

class CasualModeSetupViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authViewModel: AuthViewModel
) : ViewModel() {
    private val _uiState = MutableStateFlow(CasualModeSetupState())
    val uiState = _uiState.asStateFlow()

    private var isSaving = false

    init {
        viewModelScope.launch {
            val authState = authViewModel.authState.first()
            if (authState is AuthState.Authenticated) {
                userPreferencesRepository.getUserPreferences(authState.uid).collectLatest { currentPrefs ->
                    if (currentPrefs != null) {
                        Log.d("CasualModeSetupVM", "Loaded prefs: ${currentPrefs.casualPreferences}")
                        _uiState.value = _uiState.value.copy(
                            subjects = if (currentPrefs.casualSubjects.isNotEmpty()) currentPrefs.casualSubjects else listOf(Subject("")),
                            tasks = currentPrefs.casualTasks,
                            dailyStudyHours = currentPrefs.casualPreferences.dailyStudyHours,
                            sessionLength = currentPrefs.casualPreferences.sessionLength.minutes
                        )
                    }
                }
            }
        }
    }




    fun addSubject() {
        val currentSubjects = _uiState.value.subjects.toMutableList()
        currentSubjects.add(Subject(""))
        _uiState.value = _uiState.value.copy(subjects = currentSubjects)
    }

    fun removeSubject(index: Int) {
        val currentSubjects = _uiState.value.subjects.toMutableList()
        if (currentSubjects.size > 1) {
            currentSubjects.removeAt(index)
            _uiState.value = _uiState.value.copy(subjects = currentSubjects)
        }
    }

    fun onSubjectNameChange(index: Int, name: String) {
        val subjects = _uiState.value.subjects.toMutableList()
        subjects[index] = subjects[index].copy(name = name)
        _uiState.value = _uiState.value.copy(subjects = subjects)
    }

    fun addTask() {
        val currentTasks = _uiState.value.tasks.toMutableList()
        currentTasks.add(CasualTask(id = java.util.UUID.randomUUID().toString(),
            name = "",
            relatedSubject = null,
            dueDate = null,
            completed = false))
        _uiState.value = _uiState.value.copy(tasks = currentTasks)
    }

    fun removeTask(index: Int) {
        val currentTasks = _uiState.value.tasks.toMutableList()
        currentTasks.removeAt(index)
        _uiState.value = _uiState.value.copy(tasks = currentTasks)
    }

    fun onTaskNameChange(index: Int, name: String) {
        val tasks = _uiState.value.tasks.toMutableList()
        tasks[index] = tasks[index].copy(name = name)
        _uiState.value = _uiState.value.copy(tasks = tasks)
    }

    fun onTaskDueDateChange(index: Int, dueDate: Long?) {
        val tasks = _uiState.value.tasks.toMutableList()
        tasks[index] = tasks[index].copy(dueDate = dueDate)
        _uiState.value = _uiState.value.copy(tasks = tasks)
    }

    fun onTaskSubjectChange(index: Int, subjectName: String?) {
        val tasks = _uiState.value.tasks.toMutableList()
        tasks[index] = tasks[index].copy(relatedSubject = subjectName)
        _uiState.value = _uiState.value.copy(tasks = tasks)
    }


    fun savePreferences() {
        viewModelScope.launch(Dispatchers.IO) {
            val authState = authViewModel.authState.first()
            if (authState is AuthState.Authenticated) {
                val currentPrefs = userPreferencesRepository.getUserPreferences(authState.uid).first() ?: UserPreferences(
                    userId = authState.uid,
                    selectedMode = StudyMode.CASUAL,
                    examPreferences = ExamModePreferences(),
                    focusPreferences = FocusModePreferences(),
                    casualPreferences = CasualModePreferences(),
                    lastAccessed = System.currentTimeMillis()
                )

                val sessionLengthEnum = CasualSessionLength.values().find { it.minutes == _uiState.value.sessionLength } ?: CasualSessionLength.THIRTY

                // FILTER OUT EMPTY SUBJECTS AND TASKS
                val validSubjects = _uiState.value.subjects.filter { it.name.trim().isNotBlank() }
                val validTasks = _uiState.value.tasks.filter { it.name.trim().isNotBlank() }

                val updatedPrefs = currentPrefs.copy(
                    casualSubjects = validSubjects,  // Save only valid subjects
                    casualTasks = validTasks,        // Save only valid tasks
                    casualPreferences = currentPrefs.casualPreferences.copy(
                        dailyStudyHours = _uiState.value.dailyStudyHours,
                        sessionLength = sessionLengthEnum
                    ),
                    selectedMode = StudyMode.CASUAL,
                    lastAccessed = System.currentTimeMillis()
                )
                Log.d("CasualModeSetupVM", "Saving prefs: ${updatedPrefs.casualPreferences}")
                userPreferencesRepository.saveUserPreferences(updatedPrefs)
            }
        }
    }
}
