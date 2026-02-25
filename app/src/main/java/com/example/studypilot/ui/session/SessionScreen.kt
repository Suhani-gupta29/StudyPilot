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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studypilot.data.SessionRepository
import com.example.studypilot.data.StudyPilotDatabase
import com.example.studypilot.ui.theme.Roboto
import kotlinx.coroutines.delay

// --- Colors ---
private val primaryBlue = Color(0xFF1E88E5)
private val mainBackground = Color(0xFFFAFBFC)
private val cardShadowColor = Color.Black.copy(alpha = 0.08f)
private val buttonGradient = listOf(Color(0xFF2196F3), primaryBlue)
private val textPrimary = Color(0xFF102A43)
private val textSecondary = Color(0xFF627D98)
private val softDivider = Color(0xFFE6ECF5)

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
    onNotesClick: (String) -> Unit = {},
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
            repository = repository,
            context = context,                    // ← ADD
            breakDurationMinutes = 1,             // ← ADD (hardcode 5 for now)
            nextSessionSubject = subjectName,     // ← ADD
            longestSessionEverSeconds = 0
        )
    )

    val uiState by viewModel.uiState.collectAsState()

    // Navigate back when session is saved
    LaunchedEffect(uiState.sessionSaved) {
        if (uiState.sessionSaved) {
            android.util.Log.d("SessionScreen", "Session saved - waiting for DB sync")
            delay(200)  // Give DB time to emit to Flow
            android.util.Log.d("SessionScreen", "Navigating back now")
            onBack()
        }
    }

    SessionScreenTheme {
        Scaffold(
            containerColor = mainBackground,
            topBar = {
                SessionTopBar(mode = "${uiState.modeName.replaceFirstChar { it.uppercase() }} Mode")
            }
        ) { padding ->
            SessionContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                subject = uiState.subjectName,
                mode = uiState.modeName,
                userId = userId,
                timerText = uiState.timerText,
                isRunning = uiState.isRunning,
                canEndSession = uiState.canEndSession,
                onToggleRunning = {
                    android.util.Log.d("SessionScreen", "Toggle running clicked")
                    viewModel.toggleRunning()
                },
                onEndSession = {
                    android.util.Log.d("SessionScreen", "End session clicked")
                    viewModel.endSession()
                },
                onNotesClick = { onNotesClick(uiState.sessionId) },
                onAskAIClick = onAskAIClick
            )
        }
    }
}

// --- Top Bar ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionTopBar(mode: String) {
    Column {
        TopAppBar(
            title = {
                Text(
                    text = "StudyPilot",
                    fontFamily = Roboto,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = primaryBlue
                )
            },
            actions = {
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .border(
                            width = 1.dp,
                            color = primaryBlue,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.uppercase(),
                        color = primaryBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = Roboto
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = mainBackground
            )
        )
        Divider(color = softDivider)
    }
}

// --- Session Content ---
@Composable
private fun SessionContent(
    modifier: Modifier = Modifier,
    subject: String,
    mode: String,
    userId: String = "",
    timerText: String,
    isRunning: Boolean,
    canEndSession: Boolean,
    onToggleRunning: () -> Unit,
    onEndSession: () -> Unit,
    onNotesClick: () -> Unit,
    onAskAIClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE3F2FD),
                        Color(0xFFFAFBFC),
                        Color(0xFFFFFFFF)
                    )
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(24.dp))

            // Subject Name Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = Color.Black.copy(alpha = 0.1f)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = subject.replaceFirstChar { it.uppercase() },
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        textAlign = TextAlign.Center,
                        fontFamily = Roboto
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Active Study Session",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = textSecondary,
                        textAlign = TextAlign.Center,
                        fontFamily = Roboto
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Enhanced Timer Circle
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .shadow(
                        16.dp,
                        CircleShape,
                        spotColor = primaryBlue.copy(alpha = 0.25f),
                        ambientColor = primaryBlue.copy(alpha = 0.15f)
                    )
                    .background(Color.White, CircleShape)
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFE3F2FD),
                                    Color(0xFFBBDEFB),
                                    primaryBlue.copy(alpha = 0.12f)
                                )
                            )
                        )
                        .border(5.dp, primaryBlue.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = timerText,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryBlue,
                        fontFamily = Roboto,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status Indicator
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (isRunning) Color(0xFF4CAF50) else Color(0xFFFF9800),
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "In Progress" else "Paused",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary,
                    fontFamily = Roboto
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Enhanced Play/Pause Button with Pulse Effect
            Box(
                modifier = Modifier.size(90.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer pulse ring
                if (isRunning) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .background(
                                primaryBlue.copy(alpha = 0.15f),
                                CircleShape
                            )
                    )
                }

                FloatingActionButton(
                    onClick = onToggleRunning,
                    containerColor = primaryBlue,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(
                            12.dp,
                            CircleShape,
                            spotColor = primaryBlue.copy(alpha = 0.4f)
                        )
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pause" else "Start",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Enhanced Bottom Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        spotColor = Color.Black.copy(alpha = 0.1f)
                    ),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp, start = 24.dp, end = 24.dp, bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // End Session Button - ALWAYS ENABLED FOR DEMO
                    GradientButton(
                        text = "End Session",
                        gradientColors = if (canEndSession) buttonGradient else listOf(Color.Gray, Color.DarkGray),
                        onClick = {
                            android.util.Log.d("SessionScreen", "End Session button clicked - canEnd: $canEndSession")
                            onEndSession()
                        },
                        enabled = canEndSession,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Notes and Ask AI Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onNotesClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = primaryBlue
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, primaryBlue.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "📝 Notes",
                                fontFamily = Roboto,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            )
                        }

                    }
                }
            }
        }
    }
}

// --- Gradient Button ---
@Composable
private fun GradientButton(
    text: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.shadow(
            8.dp,
            RoundedCornerShape(18.dp),
            spotColor = primaryBlue.copy(alpha = 0.4f)
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
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
                text = text,
                color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
                fontSize = 16.sp,
                fontFamily = Roboto,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}