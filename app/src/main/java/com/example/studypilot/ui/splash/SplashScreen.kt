package com.example.studypilot.ui.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studypilot.R

@Composable
fun SplashScreen(
    splashViewModel: SplashViewModel = viewModel(),
    onNavigation: (String) -> Unit
) {
    val isUserNew by splashViewModel.isUserNew.collectAsState()

    LaunchedEffect(
        isUserNew) {
        isUserNew?.let {
            if (it) {
                onNavigation("welcome")
            } else {
                onNavigation("home")
            }
        }
    }


    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1200)
    )


    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = tween(durationMillis = 1200)
    )

    LaunchedEffect(Unit) {
        startAnimation = true
    }


    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF90CAF9), Color(0xFF1E88E5))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(100.dp)
                .scale(scaleAnim)
        )

        Spacer(modifier = Modifier.height(24.dp))


        Text(
            text = "StudyPilot",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = Color.White,
            modifier = Modifier.alpha(alphaAnim)
        )

        Spacer(modifier = Modifier.height(12.dp))


        Text(
            text = "Your personalized study planner for academic success.",
            fontSize = 16.sp,
            color = Color(0xFFE3F2FD),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .alpha(alphaAnim)
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(60.dp))


        CircularProgressIndicator(
            color = Color.White,
            strokeWidth = 4.dp,
            modifier = Modifier.size(40.dp)
        )
    }
}
