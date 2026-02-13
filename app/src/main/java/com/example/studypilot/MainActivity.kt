package com.example.studypilot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.studypilot.data.SessionRepository
import com.example.studypilot.data.UserPreferencesRepository
import com.example.studypilot.ui.auth.AuthState
import com.example.studypilot.ui.auth.AuthViewModel
import com.example.studypilot.ui.auth.SignInScreen
import com.example.studypilot.ui.auth.SignUpScreen
import com.example.studypilot.ui.casual.CasualModeSetupScreen
import com.example.studypilot.ui.casual.CasualModeSetupViewModel
import com.example.studypilot.ui.casual.CasualModeSetupViewModelFactory
import com.example.studypilot.ui.exam.ExamScreen
import com.example.studypilot.ui.exam.ExamViewModelFactory
import com.example.studypilot.ui.focus.FocusModeSetupScreen
import com.example.studypilot.ui.focus.FocusModeSetupViewModelFactory
import com.example.studypilot.ui.home.HomeScreen
import com.example.studypilot.ui.home.HomeScreenCasual
import com.example.studypilot.ui.home.HomeViewModel
import com.example.studypilot.ui.mode.ModeSelectionScreen
import com.example.studypilot.ui.mode.ModeSelectionViewModel
import com.example.studypilot.ui.mode.StudyMode
import com.example.studypilot.ui.session.SessionScreen
import com.example.studypilot.ui.settings.SettingsScreen
import com.example.studypilot.ui.splash.SplashScreen
import com.example.studypilot.ui.splash.SplashDestination
import com.example.studypilot.ui.splash.SplashViewModel
import com.example.studypilot.ui.splash.SplashViewModelFactory
import com.example.studypilot.ui.theme.StudyPilotTheme
import com.example.studypilot.ui.welcome.WelcomeScreen
import com.google.firebase.auth.FirebaseAuth
import com.example.studypilot.ui.analytics.AnalyticsScreen
import com.example.studypilot.ui.analytics.AnalyticsViewModel
import com.example.studypilot.ui.analytics.AnalyticsViewModelFactory
import com.example.studypilot.ui.planner.PlannerScreen
import com.example.studypilot.ui.planner.PlannerViewModel
import com.example.studypilot.ui.planner.PlannerViewModelFactory
import com.example.studypilot.ui.subjects.SubjectsScreen
import com.example.studypilot.ui.subjects.SubjectsViewModel
import com.example.studypilot.ui.subjects.SubjectsViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set content immediately - don't block here
        setContent {
            StudyPilotTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StudyPilotApp()
                }
            }
        }
    }
}

@Composable
fun StudyPilotApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val application = context.applicationContext as StudyPilotApplication

    // Use lazy initialization - only create when needed
    val authViewModel: AuthViewModel = viewModel()

    // Get repositories from Application - already initialized lazily
    val userPreferencesRepository = remember { application.userPreferencesRepository }
    val sessionRepository = remember { application.sessionRepository }

    // FirebaseAuth instance - lightweight, safe to create
    val firebaseAuth = remember { FirebaseAuth.getInstance() }

    // 🚨 CRITICAL FIX: Create HomeViewModel ONCE at NavHost level
    // This ensures it survives navigation and Flow collectors stay alive
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(userPreferencesRepository, authViewModel, sessionRepository)
    )

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            val splashViewModel: SplashViewModel = viewModel(
                factory = SplashViewModelFactory(userPreferencesRepository, firebaseAuth)
            )
            SplashScreen(viewModel = splashViewModel) { destination ->
                val route = when (destination) {
                    SplashDestination.Welcome -> "welcome"
                    SplashDestination.SignIn -> "signIn"
                    SplashDestination.SignUp -> "signUp"
                    SplashDestination.ModeSelection -> "modeSelection"
                    SplashDestination.Home -> "home"
                    SplashDestination.HomeCasual -> "homeCasual"
                    SplashDestination.ExamSetup -> "exam"
                    SplashDestination.FocusSetup -> "focusSetup"
                    SplashDestination.CasualSetup -> "casualSetup"
                }
                navController.navigate(route) {
                    popUpTo("splash") { inclusive = true }
                }
            }
        }

        // Auth Screens
        composable("welcome") {
            WelcomeScreen(
                onNavigateToSignIn = { navController.navigate("signIn") },
                onNavigateToSignUp = { navController.navigate("signUp") }
            )
        }

        composable("signIn") {
            SignInScreen(
                authViewModel = authViewModel,
                onNavigateToSignUp = {
                    navController.navigate("signUp") {
                        popUpTo("signIn") { inclusive = true }
                    }
                },
                onSignInSuccess = {
                    // Navigate through splash to properly load preferences before showing home
                    navController.navigate("splash") {
                        popUpTo("signIn") { inclusive = true }
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }

        composable("signUp") {
            SignUpScreen(
                authViewModel = authViewModel,
                onNavigateToSignIn = {
                    navController.navigate("signIn") {
                        popUpTo("signUp") { inclusive = true }
                    }
                },
                onSignUpSuccess = {
                    navController.navigate("modeSelection") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }

        // Mode Selection
        composable("modeSelection") {
            val authState by authViewModel.authState.collectAsState()
            val userId = (authState as? AuthState.Authenticated)?.uid
            if (userId != null) {
                val modeSelectionViewModel: ModeSelectionViewModel = viewModel(
                    factory = ModeSelectionViewModelFactory(
                        repository = userPreferencesRepository,
                        userId = userId
                    )
                )
                ModeSelectionScreen(
                    viewModel = modeSelectionViewModel,
                    onNavigateToDashboard = {
                        modeSelectionViewModel.savePreferences()
                        val mode = modeSelectionViewModel.uiState.value.selectedMode
                        val route = when (mode) {
                            StudyMode.EXAM -> "exam"
                            StudyMode.FOCUS -> "focusSetup"
                            StudyMode.CASUAL -> "casualSetup"
                        }
                        navController.navigate(route) {
                            popUpTo("modeSelection") { inclusive = true }
                        }
                    }
                )
            }
        }

        // Setup Screens
        composable("exam") {
            val examViewModel: com.example.studypilot.ui.exam.ExamViewModel = viewModel(
                factory = ExamViewModelFactory(userPreferencesRepository, authViewModel)
            )
            ExamScreen(
                viewModel = examViewModel,
                onContinueClick = {
                    examViewModel.saveExamDetails()
                    navController.navigate("home") {
                        popUpTo("exam") { inclusive = true }
                    }
                },
                onCancelClick = {
                    navController.navigate("modeSelection") {
                        popUpTo("exam") { inclusive = true }
                    }
                }
            )
        }

        composable("focusSetup") {
            val focusViewModel: com.example.studypilot.ui.focus.FocusModeSetupViewModel = viewModel(
                factory = FocusModeSetupViewModelFactory(userPreferencesRepository, authViewModel)
            )
            FocusModeSetupScreen(
                viewModel = focusViewModel,
                onContinueClick = {
                    focusViewModel.saveFocusSetup()
                    navController.navigate("home") {
                        popUpTo("focusSetup") { inclusive = true }
                    }
                },
                onBackClick = {
                    navController.navigate("modeSelection") {
                        popUpTo("focusSetup") { inclusive = true }
                    }
                }
            )
        }

        composable("casualSetup") {
            val casualViewModel: CasualModeSetupViewModel = viewModel(
                factory = CasualModeSetupViewModelFactory(userPreferencesRepository, authViewModel)
            )
            CasualModeSetupScreen(
                viewModel = casualViewModel,
                onContinueClick = {
                    casualViewModel.savePreferences()
                    navController.navigate("homeCasual") {
                        popUpTo("casualSetup") { inclusive = true }
                    }
                },
                onBackClick = {
                    navController.navigate("modeSelection") {
                        popUpTo("casualSetup") { inclusive = true }
                    }
                }
            )
        }

        // Home Screens
        composable("home") {
            // ✅ REUSE the shared homeViewModel instance - NO new creation
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToAnalytics = { navController.navigate("analytics") },
                onNavigateToPlanner = { navController.navigate("planner") },
                onNavigateToSubjects = { navController.navigate("subjects") },
                onNavigateToFocusSetup = { navController.navigate("focusSetup") },
                onNavigateToCasualSetup = {
                    navController.navigate("casualSetup")
                },
                onStartSession = { subject: String, mode: String, minutes: Int ->
                    navController.navigate("session/$subject/$mode/$minutes")
                }
            )
        }

        composable("homeCasual") {
            // ✅ REUSE the shared homeViewModel instance - NO new creation
            val uiState by homeViewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                android.util.Log.d("HomeScreenCasual", "🏠 Refreshing from database")
                homeViewModel.refreshFromDatabase()
            }

            val onStartSession = { subject: String, mode: String, minutes: Int ->
                navController.navigate("session/$subject/$mode/$minutes")
            }

            HomeScreenCasual(
                casualDetails = uiState.casualDetails,
                sessions = uiState.casualSessions,
                metrics = uiState.casualMetrics,
                alerts = uiState.casualAlerts,
                isSwapMode = uiState.isCasualSwapMode,
                swapSourceIndex = uiState.casualSwapSourceIndex,
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToAnalytics = { navController.navigate("analytics") },
                onNavigateToPlanner = { navController.navigate("planner") },
                onNavigateToSubjects = { navController.navigate("subjects") },
                onEnterSwapMode = { homeViewModel.enterSwapMode() },
                onCancelSwap = { homeViewModel.cancelSwap() },
                onSaveSwap = { homeViewModel.saveSwap() },
                onSessionClickedInSwapMode = { homeViewModel.handleSessionClickInSwapMode(it) },
                onStartSession = onStartSession,
                onNavigateToCasualSetup = {
                    navController.navigate("casualSetup")
                },
                onTaskCompletionToggled = { taskId, isCompleted ->
                    homeViewModel.toggleTaskCompletion(taskId, isCompleted)
                }
            )
        }

        // Settings
        composable("settings") {
            SettingsScreen(
                onLogout = {
                    authViewModel.logout(userPreferencesRepository)
                    navController.navigate("welcome") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("analytics") {
            val authState by authViewModel.authState.collectAsState()
            val userId = (authState as? AuthState.Authenticated)?.uid

            if (userId != null) {
                // ✅ REUSE the shared homeViewModel instance - NO new creation
                val uiState by homeViewModel.uiState.collectAsState()

                val analyticsViewModel: AnalyticsViewModel = viewModel(
                    factory = AnalyticsViewModelFactory(sessionRepository, userPreferencesRepository, userId)
                )

                val modeName = when (uiState.selectedMode) {
                    StudyMode.EXAM -> "Exam Mode"
                    StudyMode.FOCUS -> "Focus Mode"
                    StudyMode.CASUAL -> "Casual Mode"
                    null -> "Your Current Mode"
                }

                AnalyticsScreen(
                    modeName = modeName,
                    viewModel = analyticsViewModel,
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToPlanner = { navController.navigate("planner") },
                    onNavigateToSubjects = { navController.navigate("subjects") },
                    onNavigateToHome = {
                        val homeRoute = when (uiState.selectedMode) {
                            StudyMode.CASUAL -> "homeCasual"
                            else -> "home"
                        }
                        navController.navigate(homeRoute) {
                            popUpTo("analytics") { inclusive = true }
                        }
                    }
                )
            }
        }

        composable("planner") {
            val authState by authViewModel.authState.collectAsState()
            val userId = (authState as? AuthState.Authenticated)?.uid

            if (userId != null) {
                // ✅ REUSE the shared homeViewModel instance - NO new creation
                val uiState by homeViewModel.uiState.collectAsState()

                val plannerViewModel: PlannerViewModel = viewModel(
                    factory = PlannerViewModelFactory(
                        sessionRepository = sessionRepository,
                        preferencesRepository = userPreferencesRepository,
                        userId = userId
                    )
                )

                val homeRoute = when (uiState.selectedMode) {
                    StudyMode.CASUAL -> "homeCasual"
                    else -> "home"
                }

                PlannerScreen(
                    viewModel = plannerViewModel,
                    onNavigateBack = {
                        navController.navigate(homeRoute) {
                            popUpTo("planner") { inclusive = true }
                        }
                    },
                    onNavigateToHome = {
                        navController.navigate(homeRoute) {
                            popUpTo("planner") { inclusive = true }
                        }
                    },
                    onNavigateToAnalytics = { navController.navigate("analytics") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToSubjects = { navController.navigate("subjects") },
                    onDoItToday = { sessionIndex ->
                        plannerViewModel.doItToday(sessionIndex, plannerViewModel.uiState.value.selectedDate)
                    }
                )
            }
        }

        composable("subjects") {
            val authState by authViewModel.authState.collectAsState()
            val userId = (authState as? AuthState.Authenticated)?.uid

            if (userId != null) {
                // ✅ REUSE the shared homeViewModel instance - NO new creation
                val uiState by homeViewModel.uiState.collectAsState()

                val subjectsViewModel: SubjectsViewModel = viewModel(
                    factory = SubjectsViewModelFactory(
                        userPreferencesRepository = userPreferencesRepository,
                        authViewModel = authViewModel
                    )
                )

                val modeName = when (uiState.selectedMode) {
                    StudyMode.EXAM -> "Exam Mode"
                    StudyMode.FOCUS -> "Focus Mode"
                    StudyMode.CASUAL -> "Casual Mode"
                    null -> "Your Current Mode"
                }

                val homeRoute = when (uiState.selectedMode) {
                    StudyMode.CASUAL -> "homeCasual"
                    else -> "home"
                }

                SubjectsScreen(
                    viewModel = subjectsViewModel,
                    modeName = modeName,
                    onNavigateToAddSubject = { mode ->
                        // TODO: Navigate to add subject screen when implemented
                    },
                    onNavigateToViewNotes = { subjectName, mode ->
                        // TODO: Navigate to notes screen when implemented
                    },
                    onNavigateToHome = {
                        navController.navigate(homeRoute) {
                            popUpTo("subjects") { inclusive = true }
                        }
                    },
                    onNavigateToPlanner = { navController.navigate("planner") },
                    onNavigateToAnalytics = { navController.navigate("analytics") },
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
        }




        composable(
            route = "session/{subject}/{mode}/{minutes}",
            arguments = listOf(
                navArgument("subject") { type = NavType.StringType },
                navArgument("mode") { type = NavType.StringType },
                navArgument("minutes") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val subject = backStackEntry.arguments?.getString("subject") ?: ""
            val mode = backStackEntry.arguments?.getString("mode") ?: ""
            val minutes = backStackEntry.arguments?.getInt("minutes") ?: 0

            val authState by authViewModel.authState.collectAsState()
            val userId = (authState as? AuthState.Authenticated)?.uid ?: ""

            SessionScreen(
                subjectName = subject,
                modeName = mode,
                minutes = minutes,
                userId = userId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}


class ModeSelectionViewModelFactory(
    private val repository: UserPreferencesRepository,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ModeSelectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ModeSelectionViewModel(repository, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class HomeViewModelFactory(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authViewModel: AuthViewModel,
    private val repository: SessionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(userPreferencesRepository, authViewModel, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}