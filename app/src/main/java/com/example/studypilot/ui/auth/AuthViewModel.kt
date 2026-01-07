package com.example.studypilot.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val uid: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState = _authState.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth
                    .signInWithEmailAndPassword(email, password)
                    .await()

                val uid = result.user?.uid
                    ?: throw Exception("User ID not found")

                _authState.value = AuthState.Authenticated(uid)

            } catch (e: Exception) {
                _authState.value =
                    AuthState.Error(e.message ?: "Sign-in failed")
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth
                    .createUserWithEmailAndPassword(email, password)
                    .await()

                val uid = result.user?.uid
                    ?: throw Exception("User ID not found")

                _authState.value = AuthState.Authenticated(uid)

            } catch (e: Exception) {
                _authState.value =
                    AuthState.Error(e.message ?: "Sign-up failed")
            }
        }
    }
}
