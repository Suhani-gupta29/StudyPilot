package com.example.studypilot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.studypilot.ui.auth.AuthViewModel
import com.example.studypilot.ui.auth.SignInScreen
import com.example.studypilot.ui.auth.SignUpScreen
import com.example.studypilot.ui.home.HomeScreen
import com.example.studypilot.ui.splash.SplashScreen
import com.example.studypilot.ui.splash.SplashViewModel
import com.example.studypilot.ui.theme.StudyPilotTheme
import com.example.studypilot.ui.welcome.WelcomeScreen

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

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            val splashViewModel: SplashViewModel = viewModel()
            SplashScreen(
                splashViewModel = splashViewModel,
                onNavigation = { route ->
                    navController.navigate(route) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
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
                    navController.navigate("home") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen()
        }
    }
}
