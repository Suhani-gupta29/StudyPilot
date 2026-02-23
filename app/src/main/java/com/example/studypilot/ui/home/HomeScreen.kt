package com.example.studypilot.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.studypilot.ui.mode.StudyMode

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToSubjects: () -> Unit,
    onNavigateToCasualSetup: () -> Unit,
    onNavigateToFocusSetup: () -> Unit,
    onStartSession: (subject: String, mode: String, minutes: Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        android.util.Log.d("HomeScreen", "🏠 Refreshing from database")
        viewModel.refreshFromDatabase()
    }

    when (uiState.selectedMode) {
        StudyMode.EXAM -> HomeScreenExam(
            examDetails = uiState.examDetails,
            sessions = uiState.studySessions,
            metrics = uiState.accountabilityMetrics,
            alerts = uiState.alerts,
            studyStreak = uiState.studyStreak,
            isSwapMode = uiState.isSwapMode,
            swapSourceIndex = uiState.swapSourceIndex,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToAnalytics = onNavigateToAnalytics,
            onNavigateToPlanner = onNavigateToPlanner,
            onNavigateToSubjects = onNavigateToSubjects,
            onEnterSwapMode = { viewModel.enterSwapMode() },
            onCancelSwap = { viewModel.cancelSwap() },
            onSaveSwap = { viewModel.saveSwap() },
            onSessionClickedInSwapMode = { viewModel.handleSessionClickInSwapMode(it) },
            onStartSession = onStartSession,
            isSubjectChangeMode = uiState.isSubjectChangeMode,
            subjectChangeSessionIndex = uiState.subjectChangeSessionIndex,
            onEnterSubjectChangeMode = { viewModel.enterSubjectChangeMode(it) },
            onCancelSubjectChange = { viewModel.cancelSubjectChange() },
            onChangeSessionSubject = { index, subject -> viewModel.changeSessionSubject(index, subject) },
            showExamOverDialog = uiState.showExamOverDialog,
            onSaveExamSubjectReset = { viewModel.saveExamSubjectReset(it) },
            onDismissExamOverDialog = { viewModel.dismissExamOverDialog() }
        )
        StudyMode.FOCUS -> HomeScreenFocus(
            focusDetails = uiState.focusDetails,
            sessions = uiState.focusSessions,
            metrics = uiState.focusMetrics,
            alerts = uiState.focusAlerts,
            isSwapMode = uiState.isFocusSwapMode,
            swapSourceIndex = uiState.focusSwapSourceIndex,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToAnalytics = onNavigateToAnalytics,
            onNavigateToPlanner = onNavigateToPlanner,
            onNavigateToSubjects = onNavigateToSubjects,
            onEnterSwapMode = { viewModel.enterSwapMode() },
            onCancelSwap = { viewModel.cancelSwap() },
            onSaveSwap = { viewModel.saveSwap() },
            onSessionClickedInSwapMode = { viewModel.handleSessionClickInSwapMode(it) },
            onStartSession = onStartSession,
            onNavigateToFocusSetup = onNavigateToFocusSetup,
            onTaskCompletionToggled = { taskId, isCompleted ->
                viewModel.toggleTaskCompletion(taskId, isCompleted)
            },
            // ── NEW callbacks ──────────────────────────────────────────────
            isSubjectChangeMode = uiState.isSubjectChangeMode,
            subjectChangeSessionIndex = uiState.subjectChangeSessionIndex,
            onEnterSubjectChangeMode = { viewModel.enterSubjectChangeMode(it) },
            onCancelSubjectChange = { viewModel.cancelSubjectChange() },
            onChangeSessionSubject = { index, subject ->
                viewModel.changeSessionSubject(index, subject)
            },
            showWeeklyPriorityDialog = uiState.showWeeklyPriorityDialog,
            onSaveWeeklyPriorities = { viewModel.saveWeeklyPriorities(it) },
            onDismissWeeklyPriorityDialog = { viewModel.dismissWeeklyPriorityDialog() }
            // ──────────────────────────────────────────────────────────────
        )
        StudyMode.CASUAL -> HomeScreenCasual(
            casualDetails = uiState.casualDetails,
            sessions = uiState.casualSessions,
            metrics = uiState.casualMetrics,
            alerts = uiState.casualAlerts,
            isSwapMode = uiState.isCasualSwapMode,
            swapSourceIndex = uiState.casualSwapSourceIndex,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToAnalytics = onNavigateToAnalytics,
            onNavigateToPlanner = onNavigateToPlanner,
            onNavigateToSubjects = onNavigateToSubjects,
            onEnterSwapMode = { viewModel.enterSwapMode() },
            onCancelSwap = { viewModel.cancelSwap() },
            onSaveSwap = { viewModel.saveSwap() },
            onSessionClickedInSwapMode = { viewModel.handleSessionClickInSwapMode(it) },
            onStartSession = onStartSession,
            onNavigateToCasualSetup = onNavigateToCasualSetup,
            onTaskCompletionToggled = { taskId, isCompleted ->
                viewModel.toggleTaskCompletion(taskId, isCompleted)
            },
            // ── NEW callbacks ──────────────────────────────────────────────
            isSubjectChangeMode = uiState.isSubjectChangeMode,
            subjectChangeSessionIndex = uiState.subjectChangeSessionIndex,
            onEnterSubjectChangeMode = { viewModel.enterSubjectChangeMode(it) },
            onCancelSubjectChange = { viewModel.cancelSubjectChange() },
            onChangeSessionSubject = { index, subject ->
                viewModel.changeSessionSubject(index, subject)
            },
            showWeeklyPriorityDialog = uiState.showWeeklyPriorityDialog,
            onSaveWeeklyPriorities = { viewModel.saveWeeklyPriorities(it) },
            onDismissWeeklyPriorityDialog = { viewModel.dismissWeeklyPriorityDialog() }
            // ──────────────────────────────────────────────────────────────
        )
    }
}