package com.example.studypilot.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel : ViewModel() {

    private val _isUserNew = MutableStateFlow<Boolean?>(null)
    val isUserNew = _isUserNew.asStateFlow()

    init {
        viewModelScope.launch {
            delay(3500)
            _isUserNew.value = true
        }
    }
}
