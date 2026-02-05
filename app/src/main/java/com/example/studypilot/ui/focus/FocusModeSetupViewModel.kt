package com.example.studypilot.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studypilot.data.UserPreferencesRepository
import com.example.studypilot.ui.auth.AuthViewModel
import com.example.studypilot.ui.auth.AuthState
import com.example.studypilot.ui.shared.Difficulty
import com.example.studypilot.ui.shared.Effort
import com.example.studypilot.ui.shared.FocusSubject
import com.example.studypilot.ui.shared.FocusTask
import com.example.studypilot.ui.shared.Priority
import com.example.studypilot.ui.shared.TaskType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class FocusModeSetupState(
    val subjects: List<FocusSubject> = listOf(FocusSubject("", Difficulty.Medium, Priority.Medium)),
    val tasks: List<FocusTask> = emptyList()
)

class FocusModeSetupViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authViewModel: AuthViewModel
) : ViewModel() {
    private val _uiState = MutableStateFlow(FocusModeSetupState())
    val uiState = _uiState.asStateFlow()

    fun addSubject() {
        val currentSubjects = _uiState.value.subjects.toMutableList()
        currentSubjects.add(FocusSubject("", Difficulty.Medium, Priority.Medium))
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

    fun onSubjectDifficultyChange(index: Int, difficulty: Difficulty) {
        val subjects = _uiState.value.subjects.toMutableList()
        subjects[index] = subjects[index].copy(difficulty = difficulty)
        _uiState.value = _uiState.value.copy(subjects = subjects)
    }

    fun onSubjectPriorityChange(index: Int, priority: Priority) {
        val subjects = _uiState.value.subjects.toMutableList()
        subjects[index] = subjects[index].copy(priority = priority)
        _uiState.value = _uiState.value.copy(subjects = subjects)
    }

    fun addTask() {
        val currentTasks = _uiState.value.tasks.toMutableList()
        currentTasks.add(FocusTask("","", TaskType.ASSIGNMENT, Effort.Medium, null, null))
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

    fun onTaskTypeChange(index: Int, type: TaskType) {
        val tasks = _uiState.value.tasks.toMutableList()
        tasks[index] = tasks[index].copy(type = type)
        _uiState.value = _uiState.value.copy(tasks = tasks)
    }

    fun onTaskEffortChange(index: Int, effort: Effort) {
        val tasks = _uiState.value.tasks.toMutableList()
        tasks[index] = tasks[index].copy(effort = effort)
        _uiState.value = _uiState.value.copy(tasks = tasks)
    }

    fun onTaskDueDateChange(index: Int, dueDate: Long?) {
        val tasks = _uiState.value.tasks.toMutableList()
        tasks[index] = tasks[index].copy(dueDate = dueDate)
        _uiState.value = _uiState.value.copy(tasks = tasks)
    }

    fun onTaskSubjectChange(index: Int, subjectName: String) {
        val tasks = _uiState.value.tasks.toMutableList()
        val newSubject = if (subjectName.isBlank()) null else subjectName
        tasks[index] = tasks[index].copy(relatedSubject = newSubject)
        _uiState.value = _uiState.value.copy(tasks = tasks)
    }

    fun saveFocusSetup() {
        viewModelScope.launch {
            val authState = authViewModel.authState.first()
            if (authState is AuthState.Authenticated) {
                val currentPrefs = userPreferencesRepository.getUserPreferences(authState.uid).first()
                if (currentPrefs != null) {
                    // FILTER OUT EMPTY SUBJECTS - only save subjects with non-blank names
                    val validSubjects = _uiState.value.subjects.filter { it.name.trim().isNotBlank() }

                    val updatedPrefs = currentPrefs.copy(
                        focusSubjects = validSubjects,  // Save only valid subjects
                        tasks = _uiState.value.tasks,
                        lastAccessed = System.currentTimeMillis()
                    )
                    userPreferencesRepository.saveUserPreferences(updatedPrefs)
                }
            }
        }
    }
}