package com.example.studypilot.ui.session

import TopWaveCutoutShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studypilot.data.SessionRepository
import com.example.studypilot.data.StudyPilotDatabase
import com.example.studypilot.ui.theme.Roboto
import androidx.compose.runtime.LaunchedEffect

// --- Colors ---
private val primaryBlue = Color(0xFF1E88E5)
private val mainBackground = Color(0xFFFFFFFF)
private val cardShadowColor = Color.Black.copy(alpha = 0.08f)
private val buttonGradient = listOf(Color(0xFF2196F3), primaryBlue)
private val textPrimary = Color(0xFF102A43)

// --- Timer Formatter ---
private fun formatMinutesToHMS(minutes: Int): String {
    val totalSeconds = minutes * 60
    val hours = totalSeconds / 3600
    val remainingSeconds = totalSeconds % 3600
    val mins = remainingSeconds / 60
    val secs = remainingSeconds % 60
    return "%02d:%02d:%02d".format(hours, mins, secs)
}

// --- Theme ---
@Composable
private fun SessionScreenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = primaryBlue,
            background = mainBackground,
            surface = Color.White,
            onPrimary = Color.White,
            onBackground = textPrimary
        ),
        typography = Typography(),
        content = content
    )
}

// --- Main Session Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    subjectName: String,
    modeName: String,
    minutes: Int,
    userId: String,
    onBack: () -> Unit,
    onNotesClick: () -> Unit = {},
    onAskAIClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val database = StudyPilotDatabase.getDatabase(context)
    val repository = SessionRepository(database.studySessionDao())

    val viewModel: SessionViewModel = viewModel(
        factory = SessionViewModelFactory(
            subject = subjectName,
            mode = modeName,
            initialMinutes = minutes,
            userId = userId,
            repository = repository
        )
    )

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.sessionSaved) {
        if (uiState.sessionSaved) {
            onBack()
        }
    }

    SessionScreenTheme {
        Scaffold(containerColor = mainBackground) { padding ->
            SessionContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                subject = uiState.subjectName,
                mode = uiState.modeName,
                timerText = uiState.timerText,  // Changed from formatMinutesToHMS
                isRunning = uiState.isRunning,
                onToggleRunning = { viewModel.toggleRunning() },
                onEndSession = {
                    viewModel.endSession()
                },
                onNotesClick = onNotesClick,
                onAskAIClick = onAskAIClick
            )
        }
    }
}

// --- Top Bar ---
@Composable
private fun SessionTopBar(mode: String) {
    Surface(
        shadowElevation = 8.dp, // Gives proper shadow
        color = Color.White,    // App bar background
        modifier = Modifier
            .fillMaxWidth()    // Full width
            .height(56.dp)     // Standard app bar height
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp), // Internal padding for content only
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Name on left
            Text(
                text = "StudyPilot",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = primaryBlue
            )

            // Mode Capsule on right
            Box(
                modifier = Modifier
                    .background(
                        primaryBlue.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = mode.uppercase(),
                    color = primaryBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}


// --- Session Content ---
@Composable
private fun SessionContent(
    modifier: Modifier = Modifier,
    subject: String,
    mode: String,
    timerText: String,
    isRunning: Boolean,
    onToggleRunning: () -> Unit,
    onEndSession: () -> Unit,
    onNotesClick: () -> Unit,
    onAskAIClick: () -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SessionTopBar(mode = mode)
            Spacer(modifier = Modifier.height(16.dp))

            // Subject Name
            Text(
                text = subject.replaceFirstChar { it.uppercase() },
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                textAlign = TextAlign.Center,
                fontFamily = Roboto
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Timer Circle
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFBBDEFB), Color(0xFFE3F2FD))
                        )
                    )
                    .border(6.dp, primaryBlue.copy(alpha = 0.5f), CircleShape)
                    .shadow(
                        8.dp,
                        CircleShape,
                        spotColor = cardShadowColor,
                        ambientColor = cardShadowColor
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = timerText,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = primaryBlue,
                    fontFamily = Roboto,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Play/Pause Button
            FloatingActionButton(
                onClick = onToggleRunning,
                containerColor = primaryBlue,
                shape = CircleShape,
                modifier = Modifier.size(80.dp),
                elevation = FloatingActionButtonDefaults.elevation(10.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Start",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Details Card Box with Custom Cutout Shape
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
                    .shadow(16.dp, TopWaveCutoutShape(), clip = false)
                    .clip(TopWaveCutoutShape())
                    .background(Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 130.dp, start = 24.dp, end = 24.dp, bottom = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // End Session Button
                    GradientButton(
                        text = "End Session",
                        gradientColors = buttonGradient,
                        onClick = onEndSession,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Bottom Utilities
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(
                            "📝 Notes",
                            color = primaryBlue,
                            fontFamily = Roboto,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            modifier = Modifier.clickable { onNotesClick() }
                        )
                        Text(
                            "🤖 Ask AI",
                            color = primaryBlue,
                            fontFamily = Roboto,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            modifier = Modifier.clickable { onAskAIClick() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// --- Gradient Button ---
@Composable
private fun GradientButton(
    text: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.shadow(
            8.dp,
            RoundedCornerShape(18.dp),
            spotColor = primaryBlue.copy(alpha = 0.4f)
        ),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(gradientColors)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = Roboto,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

