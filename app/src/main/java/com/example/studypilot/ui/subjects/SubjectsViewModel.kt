package com.example.studypilot.ui.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studypilot.data.UserPreferencesRepository
import com.example.studypilot.ui.auth.AuthViewModel
import com.example.studypilot.ui.auth.AuthState
import com.example.studypilot.ui.mode.StudyMode
import com.example.studypilot.ui.shared.Difficulty
import com.example.studypilot.ui.shared.FocusSubject
import com.example.studypilot.ui.shared.Priority
import com.example.studypilot.ui.shared.Subject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SubjectsUiState(
    val examSubjects: List<SubjectWithStats> = emptyList(),
    val focusSubjects: List<SubjectWithStats> = emptyList(),
    val casualSubjects: List<SubjectWithStats> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: SubjectFilter = SubjectFilter.ALL,
    val isLoading: Boolean = true
)

data class SubjectWithStats(
    val name: String,
    val priority: Priority,
    val difficulty: Difficulty,
    val mode: StudyMode,
    val notesCount: Int = 0,
    val lastNoteDate: Long? = null // milliseconds
)

enum class SubjectFilter {
    ALL,
    HIGH_PRIORITY,
    MEDIUM_PRIORITY,
    LOW_PRIORITY,
    HARD,
    MEDIUM,
    EASY
}

class SubjectsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authViewModel: AuthViewModel
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
                    // Convert to SubjectWithStats
                    val examSubjectsWithStats = prefs.examSubjects.map { subject ->
                        SubjectWithStats(
                            name = subject.name,
                            priority = subject.priority,
                            difficulty = subject.difficulty,
                            mode = StudyMode.EXAM,
                            notesCount = 0, // TODO: Get from notes repository when implemented
                            lastNoteDate = null
                        )
                    }

                    val focusSubjectsWithStats = prefs.focusSubjects.map { subject ->
                        SubjectWithStats(
                            name = subject.name,
                            priority = subject.priority,
                            difficulty = subject.difficulty,
                            mode = StudyMode.FOCUS,
                            notesCount = 0,
                            lastNoteDate = null
                        )
                    }

                    val casualSubjectsWithStats = prefs.casualSubjects.map { subject ->
                        SubjectWithStats(
                            name = subject.name,
                            priority = subject.priority,
                            difficulty = subject.difficulty,
                            mode = StudyMode.CASUAL,
                            notesCount = 0,
                            lastNoteDate = null
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

    fun getFilteredSubjects(subjects: List<SubjectWithStats>): List<SubjectWithStats> {
        val query = _uiState.value.searchQuery.lowercase().trim()
        val filter = _uiState.value.selectedFilter

        return subjects
            .filter { subject ->
                // Search filter
                if (query.isNotEmpty()) {
                    subject.name.lowercase().contains(query)
                } else {
                    true
                }
            }
            .filter { subject ->
                // Priority/Difficulty filter
                when (filter) {
                    SubjectFilter.ALL -> true
                    SubjectFilter.HIGH_PRIORITY -> subject.priority == Priority.High
                    SubjectFilter.MEDIUM_PRIORITY -> subject.priority == Priority.Medium
                    SubjectFilter.LOW_PRIORITY -> subject.priority == Priority.Low
                    SubjectFilter.HARD -> subject.difficulty == Difficulty.Hard
                    SubjectFilter.MEDIUM -> subject.difficulty == Difficulty.Medium
                    SubjectFilter.EASY -> subject.difficulty == Difficulty.Easy
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
                                    prefs.copy(examSubjects = updatedSubjects)
                                }
                                StudyMode.FOCUS -> {
                                    val updatedSubjects = prefs.focusSubjects.filter { it.name != subjectName }
                                    prefs.copy(focusSubjects = updatedSubjects)
                                }
                                StudyMode.CASUAL -> {
                                    val updatedSubjects = prefs.casualSubjects.filter { it.name != subjectName }
                                    prefs.copy(casualSubjects = updatedSubjects)
                                }
                            }

                            userPreferencesRepository.saveUserPreferences(updatedPrefs)
                            android.util.Log.d("SubjectsViewModel", "Deleted subject: $subjectName from $mode")

                            // Reload subjects
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
}