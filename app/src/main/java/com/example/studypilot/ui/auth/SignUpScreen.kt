package com.example.studypilot.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SignUpScreen(
    authViewModel: AuthViewModel = viewModel(),
    onNavigateToSignIn: () -> Unit,
    onSignUpSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) }
    val authState by authViewModel.authState.collectAsState()

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF90CAF9), Color(0xFF1E88E5))
    )

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onSignUpSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "StudyPilot",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Create your account",
            fontSize = 20.sp,
            color = Color(0xFFE3F2FD)
        )

        Spacer(modifier = Modifier.height(64.dp))

        AuthTextField(value = email, onValueChange = { email = it }, placeholder = "Email")
        Spacer(modifier = Modifier.height(16.dp))
        PasswordTextField(value = password, onValueChange = { password = it }, placeholder = "Password", isError = passwordError)
        Spacer(modifier = Modifier.height(16.dp))
        PasswordTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, placeholder = "Confirm Password", isError = passwordError)

        if (passwordError) {
            Text(
                text = "Passwords do not match",
                color = Color.Red,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        AuthButton(
            text = "Create Account",
            onClick = {
                passwordError = password != confirmPassword
                if (!passwordError) {
                    authViewModel.signUp(email, password)
                }
            },
            isLoading = authState is AuthState.Loading
        )

        if (authState is AuthState.Error) {
            Text(
                text = (authState as AuthState.Error).message,
                color = Color.Red,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        TextButton(onClick = onNavigateToSignIn) {
            Text("Already have an account? Sign In", color = Color.White)
        }
    }
}
