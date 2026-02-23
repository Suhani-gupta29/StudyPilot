package com.example.studypilot.ui.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studypilot.data.UserPreferencesRepository
import com.example.studypilot.ui.auth.AuthViewModel
import com.example.studypilot.ui.auth.AuthState
import com.example.studypilot.ui.mode.StudyMode
import com.example.studypilot.ui.shared.Difficulty
import com.example.studypilot.ui.shared.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.studypilot.ui.shared.Subject
import com.example.studypilot.ui.shared.FocusSubject
import com.example.studypilot.ui.shared.FocusTask
import com.example.studypilot.ui.casual.CasualTask
import com.example.studypilot.ui.shared.TaskType
import com.example.studypilot.ui.shared.Effort
import com.example.studypilot.data.SessionRepository

data class SubjectsUiState(
    val examSubjects: List<SubjectWithStats> = emptyList(),
    val focusSubjects: List<SubjectWithStats> = emptyList(),
    val casualSubjects: List<SubjectWithStats> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: SubjectFilter = SubjectFilter.ALL,
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val showAddTaskDialog: Boolean = false,
    val addDialogError: String? = null,
    val targetAddMode: StudyMode? = null
)

data class SubjectWithStats(
    val name: String,
    val priority: Priority,
    val difficulty: Difficulty,
    val mode: StudyMode,
    val notesCount: Int = 0,
    val lastNoteDate: Long? = null,
    val isWeak: Boolean = false
)

enum class SubjectFilter {
    ALL,
    HIGH_PRIORITY,
    MEDIUM_PRIORITY,
    LOW_PRIORITY,
    HARD,
    MEDIUM,
    EASY,
    WEAK
}

class SubjectsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authViewModel: AuthViewModel,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubjectsUiState())
    val uiState: StateFlow<SubjectsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authViewModel.authState.collectLatest { authState ->
                if (authState is AuthState.Authenticated) {
                    loadSubjects(authState.uid)
                } else {
                    _uiState.value = SubjectsUiState(isLoading = false)
                }
            }
        }
    }

    private suspend fun loadSubjects(userId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)

        withContext(Dispatchers.IO) {
            try {
                val prefs = userPreferencesRepository.getUserPreferences(userId).first()

                if (prefs != null) {
                    // Get all completed sessions for the user
                    val allSessions = sessionRepository.getSessionsForUser(userId).first()

                    // Calculate weak subjects based on completion rate
                    val subjectStats = allSessions
                        .filter { it.completed }
                        .groupBy { it.subjectName }
                        .mapValues { (_, sessions) ->
                            // Calculate completion rate (completed sessions / total sessions for this subject)
                            val totalForSubject = allSessions.count { it.subjectName == sessions.first().subjectName }
                            val completedForSubject = sessions.size
                            val completionRate = if (totalForSubject > 0) completedForSubject.toFloat() / totalForSubject else 0f
                            completionRate
                        }

                    // Mark as weak if completion rate < 40% and has at least 3 sessions
                    val weakThreshold = 0.4f
                    val minSessionsForAnalysis = 3

                    val examSubjectsWithStats = prefs.examSubjects.map { subject ->
                        val totalSessions = allSessions.count { it.subjectName == subject.name && it.modeName == "EXAM" }
                        val completionRate = subjectStats[subject.name] ?: 0f
                        val isWeak = totalSessions >= minSessionsForAnalysis && completionRate < weakThreshold

                        SubjectWithStats(
                            name = subject.name,
                            priority = subject.priority,
                            difficulty = subject.difficulty,
                            mode = StudyMode.EXAM,
                            notesCount = 0,
                            lastNoteDate = null,
                            isWeak = isWeak
                        )
                    }

                    val focusSubjectsWithStats = prefs.focusSubjects.map { subject ->
                        val totalSessions = allSessions.count { it.subjectName == subject.name && it.modeName == "FOCUS" }
                        val completionRate = subjectStats[subject.name] ?: 0f
                        val isWeak = totalSessions >= minSessionsForAnalysis && completionRate < weakThreshold

                        SubjectWithStats(
                            name = subject.name,
                            priority = subject.priority,
                            difficulty = subject.difficulty,
                            mode = StudyMode.FOCUS,
                            notesCount = 0,
                            lastNoteDate = null,
                            isWeak = isWeak
                        )
                    }

                    val casualSubjectsWithStats = prefs.casualSubjects.map { subject ->
                        val totalSessions = allSessions.count { it.subjectName == subject.name && it.modeName == "CASUAL" }
                        val completionRate = subjectStats[subject.name] ?: 0f
                        val isWeak = totalSessions >= minSessionsForAnalysis && completionRate < weakThreshold

                        SubjectWithStats(
                            name = subject.name,
                            priority = subject.priority,
                            difficulty = subject.difficulty,
                            mode = StudyMode.CASUAL,
                            notesCount = 0,
                            lastNoteDate = null,
                            isWeak = isWeak
                        )
                    }

                    _uiState.value = _uiState.value.copy(
                        examSubjects = examSubjectsWithStats,
                        focusSubjects = focusSubjectsWithStats,
                        casualSubjects = casualSubjectsWithStats,
                        isLoading = false
                    )

                    android.util.Log.d("SubjectsViewModel", "Loaded subjects: exam=${examSubjectsWithStats.size}, focus=${focusSubjectsWithStats.size}, casual=${casualSubjectsWithStats.size}")
                }
                else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                android.util.Log.e("SubjectsViewModel", "Failed to load subjects", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun updateFilter(filter: SubjectFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun openAddDialog(mode: StudyMode) {
        _uiState.value = _uiState.value.copy(showAddDialog = true, targetAddMode = mode)
    }

    fun closeAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false, addDialogError = null, targetAddMode = null)
    }

    fun openAddTaskDialog() {
        _uiState.value = _uiState.value.copy(showAddTaskDialog = true)
    }

    fun closeAddTaskDialog() {
        _uiState.value = _uiState.value.copy(showAddTaskDialog = false)
    }

    fun getFilteredSubjects(subjects: List<SubjectWithStats>): List<SubjectWithStats> {
        val query = _uiState.value.searchQuery.lowercase().trim()
        val filter = _uiState.value.selectedFilter

        return subjects
            .filter { subject ->
                if (query.isNotEmpty()) {
                    subject.name.lowercase().contains(query)
                } else {
                    true
                }
            }
            .filter { subject ->
                when (filter) {
                    SubjectFilter.ALL -> true
                    SubjectFilter.HIGH_PRIORITY -> subject.priority == Priority.High
                    SubjectFilter.MEDIUM_PRIORITY -> subject.priority == Priority.Medium
                    SubjectFilter.LOW_PRIORITY -> subject.priority == Priority.Low
                    SubjectFilter.HARD -> subject.difficulty == Difficulty.Hard
                    SubjectFilter.MEDIUM -> subject.difficulty == Difficulty.Medium
                    SubjectFilter.EASY -> subject.difficulty == Difficulty.Easy
                    SubjectFilter.WEAK -> subject.isWeak
                }
            }
    }

    fun deleteSubject(subjectName: String, mode: StudyMode) {
        viewModelScope.launch {
            val authState = authViewModel.authState.first()
            if (authState is AuthState.Authenticated) {
                withContext(Dispatchers.IO) {
                    try {
                        val prefs = userPreferencesRepository.getUserPreferences(authState.uid).first()
                        if (prefs != null) {
                            val updatedPrefs = when (mode) {
                                StudyMode.EXAM -> {
                                    val updatedSubjects = prefs.examSubjects.filter { it.name != subjectName }
                                    prefs.copy(examSubjects = updatedSubjects, dailyPlan = emptyList(), dailyPlanDate = "")
                                }
                                StudyMode.FOCUS -> {
                                    val updatedSubjects = prefs.focusSubjects.filter { it.name != subjectName }
                                    prefs.copy(focusSubjects = updatedSubjects, focusDailyPlan = emptyList(), focusPlanDate = "")
                                }
                                StudyMode.CASUAL -> {
                                    val updatedSubjects = prefs.casualSubjects.filter { it.name != subjectName }
                                    prefs.copy(casualSubjects = updatedSubjects, casualDailyPlan = emptyList(), casualPlanDate = "")
                                }
                            }

                            userPreferencesRepository.saveUserPreferences(updatedPrefs)
                            android.util.Log.d("SubjectsViewModel", "Deleted subject: $subjectName from $mode")

                            loadSubjects(authState.uid)
                        }
                        else {
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }
                    }
                    catch (e: Exception) {
                        android.util.Log.e("SubjectsViewModel", "Failed to delete subject", e)
                    }
                }
            }
        }
    }


    fun addSubject(name: String, priority: Priority, difficulty: Difficulty, mode: StudyMode) {
        viewModelScope.launch {
            val authState = authViewModel.authState.first()
            if (authState is AuthState.Authenticated) {
                withContext(Dispatchers.IO) {
                    try {
                        val prefs = userPreferencesRepository.getUserPreferences(authState.uid).first()
                        if (prefs != null) {
                            val updatedPrefs = when (mode) {
                                StudyMode.EXAM -> prefs.copy(
                                    examSubjects = prefs.examSubjects + Subject(name.trim(), priority, difficulty),
                                    dailyPlan = emptyList(),
                                    dailyPlanDate = ""
                                )
                                StudyMode.FOCUS -> prefs.copy(
                                    focusSubjects = prefs.focusSubjects + FocusSubject(name.trim(), difficulty, priority),
                                    focusDailyPlan = emptyList(),
                                    focusPlanDate = ""
                                )
                                StudyMode.CASUAL -> prefs.copy(
                                    casualSubjects = prefs.casualSubjects + Subject(name.trim(), priority, difficulty),
                                    casualDailyPlan = emptyList(),
                                    casualPlanDate = ""
                                )
                            }
                            userPreferencesRepository.saveUserPreferences(updatedPrefs)
                            loadSubjects(authState.uid)
                        }
                        else {
                            android.util.Log.e("SubjectsViewModel", "Cannot add subject - prefs is null")
                            _uiState.value = _uiState.value.copy(isLoading = false)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SubjectsViewModel", "Failed to add subject", e)
                    }
                }
            }
        }
    }


    fun addTask(name: String, relatedSubject: String, dueDate: Long?, type: TaskType, effort: Effort, mode: StudyMode) {
        viewModelScope.launch {
            val authState = authViewModel.authState.first()
            if (authState is AuthState.Authenticated) {
                withContext(Dispatchers.IO) {
                    try {
                        val prefs = userPreferencesRepository.getUserPreferences(authState.uid).first()
                        if (prefs != null) {
                            val updatedPrefs = when (mode) {
                                StudyMode.FOCUS -> {
                                    val newTask = FocusTask(
                                        name = name.trim(),
                                        type = type,
                                        effort = effort,
                                        dueDate = dueDate,
                                        relatedSubject = if (relatedSubject.isBlank()) null else relatedSubject.trim(),
                                        completed = false
                                    )
                                    prefs.copy(tasks = prefs.tasks + newTask)
                                }
                                StudyMode.CASUAL -> {
                                    val newTask = CasualTask(
                                        name = name.trim(),
                                        relatedSubject = if (relatedSubject.isBlank()) null else relatedSubject.trim(),
                                        dueDate = dueDate,
                                        completed = false
                                    )
                                    prefs.copy(casualTasks = prefs.casualTasks + newTask)
                                }
                                StudyMode.EXAM -> prefs // Exam mode has no tasks
                            }
                            userPreferencesRepository.saveUserPreferences(updatedPrefs)
                            android.util.Log.d("SubjectsViewModel", "Added task: $name to $mode")
                        } else {
                            android.util.Log.e("SubjectsViewModel", "Cannot add task - prefs is null")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SubjectsViewModel", "Failed to add task", e)
                    }
                }
            }
        }
    }
}
