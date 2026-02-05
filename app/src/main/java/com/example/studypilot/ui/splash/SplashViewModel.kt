package com.example.studypilot.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.studypilot.data.UserPreferencesRepository
import com.example.studypilot.ui.mode.StudyMode
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
            val currentUser = firebaseAuth.currentUser

            if (currentUser == null) {
                // User not signed in → go to Welcome
                _destination.value = SplashDestination.Welcome
                return@launch
            }

            // User is signed in → check prefs
            val prefs = userPreferencesRepository
                .getUserPreferences(currentUser.uid)
                .filterNotNull()
                .first()

            if (prefs.selectedMode == null) {
                _destination.value = SplashDestination.ModeSelection
                return@launch
            }

            _destination.value = when (prefs.selectedMode) {
                StudyMode.CASUAL -> SplashDestination.HomeCasual
                else -> SplashDestination.Home
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
