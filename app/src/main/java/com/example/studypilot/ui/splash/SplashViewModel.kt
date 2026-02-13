package com.example.studypilot.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.studypilot.data.UserPreferencesRepository
import com.example.studypilot.ui.mode.StudyMode
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SplashDestination {
    Welcome,        // For logged-out / first-time users
    SignIn,         // Explicit sign-in screen
    SignUp,         // Explicit sign-up screen
    ModeSelection,  // New user after signup
    Home,           // Resume Home dashboard
    HomeCasual,     // Resume Casual Home
    ExamSetup,      // Resume Exam setup/input
    FocusSetup,     // Resume Focus setup/input
    CasualSetup     // Resume Casual setup/input
}

class SplashViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination?>(null)
    val destination = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val minSplashTime = 1000L  // 1 second for better UX
                val startTime = System.currentTimeMillis()

                val targetDestination = withContext(Dispatchers.IO) {
                    val currentUser = firebaseAuth.currentUser

                    android.util.Log.d("SplashViewModel", "Checking auth state - User: ${currentUser?.uid ?: "null"}")

                    if (currentUser == null) {
                        android.util.Log.d("SplashViewModel", "No user logged in → Welcome")
                        return@withContext SplashDestination.Welcome
                    }

                    // User is logged in - try to get preferences with retry
                    var prefs = userPreferencesRepository
                        .getUserPreferences(currentUser.uid)
                        .firstOrNull()

                    // Retry once if null (sometimes Firestore takes a moment)
                    if (prefs == null) {
                        android.util.Log.d("SplashViewModel", "Prefs null on first try, waiting 300ms and retrying...")
                        delay(300)
                        prefs = userPreferencesRepository
                            .getUserPreferences(currentUser.uid)
                            .firstOrNull()
                    }

                    android.util.Log.d("SplashViewModel", "Loaded prefs: mode=${prefs?.selectedMode}, examSubjects=${prefs?.examSubjects?.size}, focusSubjects=${prefs?.focusSubjects?.size}, casualSubjects=${prefs?.casualSubjects?.size}")

                    // User is logged in but no preferences found
                    if (prefs == null) {
                        android.util.Log.d("SplashViewModel", "User logged in but no prefs → ModeSelection")
                        return@withContext SplashDestination.ModeSelection
                    }

                    // User has preferences - check if setup is complete
                    when (prefs.selectedMode) {
                        StudyMode.EXAM -> {
                            // Check if exam setup is complete
                            val hasExamDetails = prefs.examSubjects.isNotEmpty() &&
                                    prefs.examName?.isNotBlank() == true

                            if (hasExamDetails) {
                                android.util.Log.d("SplashViewModel", "Exam mode setup complete → Home")
                                SplashDestination.Home
                            } else {
                                android.util.Log.d("SplashViewModel", "Exam mode incomplete → ExamSetup")
                                SplashDestination.ExamSetup
                            }
                        }

                        StudyMode.FOCUS -> {
                            // Check if focus setup is complete
                            val hasFocusDetails = prefs.focusSubjects.isNotEmpty()

                            if (hasFocusDetails) {
                                android.util.Log.d("SplashViewModel", "Focus mode setup complete → Home")
                                SplashDestination.Home
                            } else {
                                android.util.Log.d("SplashViewModel", "Focus mode incomplete → FocusSetup")
                                SplashDestination.FocusSetup
                            }
                        }

                        StudyMode.CASUAL -> {
                            // Check if casual setup is complete
                            val hasCasualDetails = prefs.casualSubjects.isNotEmpty()

                            if (hasCasualDetails) {
                                android.util.Log.d("SplashViewModel", "Casual mode setup complete → HomeCasual")
                                SplashDestination.HomeCasual
                            } else {
                                android.util.Log.d("SplashViewModel", "Casual mode incomplete → CasualSetup")
                                SplashDestination.CasualSetup
                            }
                        }

                        null -> {
                            // Mode is null - user needs to select a mode
                            android.util.Log.d("SplashViewModel", "No mode selected → ModeSelection")
                            SplashDestination.ModeSelection
                        }
                    }
                }

                // Ensure minimum splash time for smooth transition
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < minSplashTime) {
                    delay(minSplashTime - elapsed)
                }

                android.util.Log.d("SplashViewModel", "Final destination: $targetDestination")
                _destination.value = targetDestination

            } catch (e: Exception) {
                android.util.Log.e("SplashViewModel", "Error in splash logic", e)
                e.printStackTrace()
                // On error, safely go to welcome
                _destination.value = SplashDestination.Welcome
            }
        }
    }
}

class SplashViewModelFactory(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SplashViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SplashViewModel(userPreferencesRepository, firebaseAuth) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}