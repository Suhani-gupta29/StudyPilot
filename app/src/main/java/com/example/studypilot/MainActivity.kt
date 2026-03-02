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
import com.example.studypilot.ui.notes.NotesScreen
import com.example.studypilot.ui.planner.PlannerScreen
import com.example.studypilot.ui.planner.PlannerViewModel
import com.example.studypilot.ui.planner.PlannerViewModelFactory
import com.example.studypilot.ui.subjects.SubjectsScreen
import com.example.studypilot.ui.subjects.SubjectsViewModel
import com.example.studypilot.ui.subjects.SubjectsViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.studypilot.notifications.NotificationScheduler
import com.example.studypilot.ui.chat.ChatViewModel
import com.example.studypilot.ui.chat.ChatViewModelFactory
import com.example.studypilot.ui.chat.Room
import com.example.studypilot.ui.chat.RoomDirectoryScreen
import com.example.studypilot.ui.chat.ChatScreen


class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Optional: show a rationale snackbar if !isGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

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

    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Authenticated -> NotificationScheduler.scheduleAll(context, state.uid)
            is AuthState.Unauthenticated -> NotificationScheduler.cancelAll(context)
            else -> {}
        }
    }

    // 🚨 CRITICAL FIX: Create HomeViewModel ONCE at NavHost level
    // This ensures it survives navigation and Flow collectors stay alive
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(userPreferencesRepository, authViewModel, sessionRepository)
    )


    val currentUser = firebaseAuth.currentUser
    val currentUserId = currentUser?.uid ?: ""

// Collect displayName from UserPreferences (set in Settings screen)
    val userPrefsState = userPreferencesRepository
        .getUserPreferences(currentUserId)
        .collectAsState(initial = null)

    val displayName = userPrefsState.value?.displayName
        ?.takeIf { it.isNotBlank() }
        ?: currentUser?.email?.substringBefore("@")
        ?: "User"

    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(
            userId   = currentUserId,
            userName = displayName
        )
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
                onNavigateToRooms     = { navController.navigate("rooms") },
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
                onNavigateToRooms     = { navController.navigate("rooms") },
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
                },
                isSubjectChangeMode = uiState.isSubjectChangeMode,
                subjectChangeSessionIndex = uiState.subjectChangeSessionIndex,
                onEnterSubjectChangeMode = { homeViewModel.enterSubjectChangeMode(it) },
                onCancelSubjectChange = { homeViewModel.cancelSubjectChange() },
                onChangeSessionSubject = { index, subject ->
                    homeViewModel.changeSessionSubject(index, subject)
                },
                showWeeklyPriorityDialog = uiState.showWeeklyPriorityDialog,
                onSaveWeeklyPriorities = { homeViewModel.saveWeeklyPriorities(it) },
                onDismissWeeklyPriorityDialog = { homeViewModel.dismissWeeklyPriorityDialog() }
            )
        }

        // Settings
        // ── Drop this composable inside your NavHost alongside "subjects", "home" etc. ──

        // ── Drop inside your NavHost ──────────────────────────────────────────────────

        composable("settings") {
            val authState by authViewModel.authState.collectAsState()
            val userId = (authState as? AuthState.Authenticated)?.uid

            if (userId != null) {
                val uiState by homeViewModel.uiState.collectAsState()

                val homeRoute = when (uiState.selectedMode) {
                    StudyMode.CASUAL -> "homeCasual"
                    else -> "home"
                }

                SettingsScreen(
                    userPreferencesRepository = userPreferencesRepository,
                    authViewModel = authViewModel,
                    homeViewModel = homeViewModel,           // ← pass shared homeViewModel
                    onNavigateToLogin = {
                        navController.navigate("welcome") {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToHome = {
                        navController.navigate(homeRoute) {
                            popUpTo("settings") { inclusive = true }
                        }
                    },
                    onNavigateToPlanner = { navController.navigate("planner") },
                    onNavigateToAnalytics = { navController.navigate("analytics") },
                    onNavigateToSubjects = { navController.navigate("subjects") }
                )
            }
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
                        authViewModel = authViewModel,
                        sessionRepository = sessionRepository
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
                        navController.navigate("notes/$userId/$subjectName/${mode.name}")
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
            route = "notes/{userId}/{subjectName}/{mode}?sessionId={sessionId}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("subjectName") { type = NavType.StringType },
                navArgument("mode") { type = NavType.StringType },
                navArgument("sessionId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val subjectName = backStackEntry.arguments?.getString("subjectName") ?: ""
            val mode = backStackEntry.arguments?.getString("mode") ?: ""
            val sessionId = backStackEntry.arguments?.getString("sessionId")

            NotesScreen(
                userId = userId,
                subjectName = subjectName,
                mode = mode,
                sessionId = if (sessionId == "null") null else sessionId,
                onBack = { navController.popBackStack() }
            )
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
                onBack = { navController.popBackStack() },
                onNotesClick = { sessionId ->
                    navController.navigate("notes/$userId/$subject/$mode?sessionId=$sessionId")
                }
            )
        }

        // ── Room Directory ────────────────────────────────────────────────────────
        composable("rooms") {
            RoomDirectoryScreen(
                viewModel = chatViewModel,
                onNavigateToChat = { room ->
                    navController.navigate("chat/${room.id}/${room.name}/${room.memberCount}")
                },
                onBack = { navController.popBackStack() }
            )
        }

// ── Chat Screen ───────────────────────────────────────────────────────────
        composable(
            route = "chat/{roomId}/{roomName}/{memberCount}",
            arguments = listOf(
                navArgument("roomId")      { type = NavType.StringType },
                navArgument("roomName")    { type = NavType.StringType },
                navArgument("memberCount") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val roomId      = backStackEntry.arguments?.getString("roomId") ?: ""
            val roomName    = backStackEntry.arguments?.getString("roomName") ?: ""
            val memberCount = backStackEntry.arguments?.getInt("memberCount") ?: 0
            val authState by authViewModel.authState.collectAsState()
            val userId = (authState as? AuthState.Authenticated)?.uid ?: ""

            ChatScreen(
                viewModel     = chatViewModel,
                room          = Room(id = roomId, name = roomName, memberCount = memberCount),
                currentUserId = userId,
                onBack        = { navController.popBackStack() }
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