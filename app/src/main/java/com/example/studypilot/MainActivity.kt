
package com.example.studypilot

import android.R.attr.type
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.studypilot.data.StudyPilotDatabase
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
import androidx.compose.runtime.rememberCoroutineScope
import com.example.studypilot.data.SessionRepository
import kotlinx.coroutines.launch
import com.example.studypilot.ui.home.HomeScreenFocus
import com.example.studypilot.ui.analytics.AnalyticsScreen
import com.example.studypilot.ui.analytics.AnalyticsViewModel
import com.example.studypilot.ui.analytics.AnalyticsViewModelFactory
import com.example.studypilot.ui.planner.PlannerScreen
import com.example.studypilot.ui.planner.PlannerViewModel
import com.example.studypilot.ui.planner.PlannerViewModelFactory


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StudyPilotTheme {
                StudyPilotApp()
            }
        }
    }
}

@Composable
fun StudyPilotApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val application = LocalContext.current.applicationContext as Application
    val database = StudyPilotDatabase.getDatabase(application)
    val userPreferencesRepository = UserPreferencesRepository(database.userPreferencesDao())
    val sessionRepository = SessionRepository(database.studySessionDao())
    val firebaseAuth = FirebaseAuth.getInstance()

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
                    navController.navigate("home") {
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
            HomeScreen(
                viewModel = viewModel(
                    factory = HomeViewModelFactory(userPreferencesRepository, authViewModel, sessionRepository)
                ),
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToAnalytics = { navController.navigate("analytics") },
                onNavigateToPlanner = { navController.navigate("planner") },
                onNavigateToFocusSetup = { navController.navigate("focusSetup") },
                onNavigateToCasualSetup = {  // ADD THIS
                    navController.navigate("casualSetup")
                },// Required by HomeScreen wrapper
                onStartSession = { subject: String, mode: String, minutes: Int ->
                    navController.navigate("session/$subject/$mode/$minutes")
                }
            )
        }

        composable("homeCasual") {
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(userPreferencesRepository, authViewModel, sessionRepository)
            )
            val uiState by homeViewModel.uiState.collectAsState()

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
                onEnterSwapMode = { homeViewModel.enterSwapMode() },
                onCancelSwap = { homeViewModel.cancelSwap() },
                onSaveSwap = { homeViewModel.saveSwap() },
                onSessionClickedInSwapMode = { homeViewModel.handleSessionClickInSwapMode(it) },
                onStartSession = onStartSession,
                onNavigateToCasualSetup = {  // ADD THIS
                    navController.navigate("casualSetup")
                }
            )
        }
        // Settings
        composable("settings") {
            SettingsScreen(
                onLogout = {
                    authViewModel.logout(userPreferencesRepository)
                    navController.navigate("welcome") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }


        composable("analytics") {
            val authState by authViewModel.authState.collectAsState()
            val userId = (authState as? AuthState.Authenticated)?.uid

            if (userId != null) {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModelFactory(userPreferencesRepository, authViewModel, sessionRepository)
                )
                val uiState by homeViewModel.uiState.collectAsState()

                val analyticsViewModel: AnalyticsViewModel = viewModel(
                    factory = AnalyticsViewModelFactory(sessionRepository,userPreferencesRepository,userId)
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
                    onNavigateToHome = {
                        // Navigate back to appropriate home screen based on mode
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
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModelFactory(userPreferencesRepository, authViewModel, sessionRepository)
                )
                val uiState by homeViewModel.uiState.collectAsState()

                val plannerViewModel: PlannerViewModel = viewModel(
                    factory = PlannerViewModelFactory(
                        sessionRepository = sessionRepository,
                        preferencesRepository = userPreferencesRepository,
                        userId = userId
                    )
                )

                val modeName = when (uiState.selectedMode) {
                    StudyMode.EXAM -> "Exam Mode"
                    StudyMode.FOCUS -> "Focus Mode"
                    StudyMode.CASUAL -> "Casual Mode"
                    null -> "Your Current Mode"
                }

                PlannerScreen(
                    viewModel = plannerViewModel,
                    onNavigateBack = {
                        // Navigate back to appropriate home screen based on mode
                        val homeRoute = when (uiState.selectedMode) {
                            StudyMode.CASUAL -> "homeCasual"
                            else -> "home"
                        }
                        navController.navigate(homeRoute) {
                            popUpTo("planner") { inclusive = true }
                        }
                    }
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
