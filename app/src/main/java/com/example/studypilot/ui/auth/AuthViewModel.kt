package com.example.studypilot.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studypilot.data.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(val email: String?, val uid: String) : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState = _authState.asStateFlow()

    private val firebaseAuth = FirebaseAuth.getInstance()

    init {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            _authState.value = AuthState.Authenticated(currentUser.email, currentUser.uid)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                _authState.value = AuthState.Authenticated(result.user?.email, result.user?.uid!!)
            } catch (e: FirebaseAuthException) {
                _authState.value = AuthState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                _authState.value = AuthState.Authenticated(result.user?.email, result.user?.uid!!)
            } catch (e: FirebaseAuthException) {
                _authState.value = AuthState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }

    fun logout(userPreferencesRepository: UserPreferencesRepository) {
        viewModelScope.launch {
            val authState = _authState.first()
            if (authState is AuthState.Authenticated) {
                val currentPrefs = userPreferencesRepository.getUserPreferences(authState.uid).first()
                if (currentPrefs != null) {
                    userPreferencesRepository.saveUserPreferences(currentPrefs.copy(lastRoute = null))
                }
            }
            firebaseAuth.signOut()
            _authState.value = AuthState.Unauthenticated
        }
    }
}