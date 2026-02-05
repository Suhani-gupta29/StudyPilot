package com.example.studypilot.ui.mode

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt

// --- Color Palette ---
private val backgroundGradient = Brush.verticalGradient(colors = listOf(Color(0xFF8EC5FC), Color(0xFF4FACFE)))
private val primaryAccent = Color(0xFF2563EB)
private val surfaceBackground = Color.White
private val primaryTextColor = Color(0xFF0F172A)
private val secondaryTextColor = Color(0xFF64748B)
private val headerTextColor = Color.White
private val subheaderTextColor = Color(0xFFE0F2FE)
private val selectionBackground = Color(0xFFDBEAFE)
private val sliderInactiveTrackColor = Color(0xFFCBD5E1)
private val segmentedControlBackground = Color(0xFFE0F2FE)

@Composable
fun ModeSelectionScreen(
    viewModel: ModeSelectionViewModel = viewModel(),
    onNavigateToDashboard: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(500)) + slideInVertically(animationSpec = tween(500))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    ScreenHeader()
                    ModeSelector(
                        selectedMode = uiState.selectedMode,
                        onModeSelect = viewModel::selectMode
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(500, 200)) +
                        slideInVertically(animationSpec = tween(500, 200)) { it / 2 }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    SettingsCard(uiState, viewModel)
                    HelperText(uiState)
                }
            }
        }

        PrimaryActionButton(
            onClick = {
                viewModel.savePreferences()
                onNavigateToDashboard()
            }
        )
    }
}

@Composable
private fun ScreenHeader() {
    Spacer(modifier = Modifier.height(64.dp)) // Increased spacer
    Text(
        text = "Select Your Study Style",
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        color = headerTextColor
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Choose a mode that matches your goals.",
        fontSize = 14.sp,
        color = subheaderTextColor
    )
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun ModeSelector(selectedMode: StudyMode, onModeSelect: (StudyMode) -> Unit) {
    Card(
        modifier = Modifier.height(48.dp),
        shape = CircleShape,
        colors = CardDefaults.cardColors(containerColor = segmentedControlBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            StudyMode.values().forEach { mode ->
                val isSelected = selectedMode == mode
                val animatedColor by animateColorAsState(if (isSelected) primaryAccent else secondaryTextColor)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(if (isSelected) surfaceBackground else Color.Transparent)
                        .clickable { onModeSelect(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.name,
                        color = animatedColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(uiState: ModeSelectionState, viewModel: ModeSelectionViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(0.9f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        AnimatedContent(
            targetState = uiState.selectedMode,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(300))
            },
            label = "SettingsCardAnimation"
        ) { targetMode ->
            Column(modifier = Modifier.padding(20.dp)) {
                when (targetMode) {
                    StudyMode.EXAM -> ExamModePreferencesView(
                        prefs = uiState.examPreferences,
                        onDailyStudyHoursChange = viewModel::updateExamDailyStudyHours,
                        onSessionLengthChange = viewModel::updateExamSessionLength,
                        onBreakPreferenceChange = viewModel::updateExamBreakPreference,
                        onNotificationPreferenceChange = viewModel::updateExamNotificationPreference
                    )

                    StudyMode.FOCUS -> FocusModePreferencesView(
                        prefs = uiState.focusPreferences,
                        onDailyStudyHoursChange = viewModel::updateFocusDailyStudyHours,
                        onSessionLengthChange = viewModel::updateFocusSessionLength,
                        onBreakPreferenceChange = viewModel::updateFocusBreakPreference,
                        onNotificationPreferenceChange = viewModel::updateFocusNotificationPreferences
                    )

                    StudyMode.CASUAL -> CasualModePreferencesView(
                        prefs = uiState.casualPreferences,
                        onDailyStudyHoursChange = viewModel::updateCasualDailyStudyHours,
                        onSessionLengthChange = viewModel::updateCasualSessionLength,
                        onNotificationsEnabledChange = viewModel::updateCasualNotificationsEnabled
                    )
                }
            }
        }
    }
}

@Composable
private fun HelperText(uiState: ModeSelectionState) {
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = generatePreviewText(uiState),
        color = Color.White,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    )
}

@Composable
private fun BoxScope.PrimaryActionButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
            .height(56.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = surfaceBackground)
    ) {
        Text(
            text = "Continue to Dashboard",
            color = primaryAccent,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

// --- Preference Views ---

@Composable
private fun ExamModePreferencesView(
    prefs: ExamModePreferences,
    onDailyStudyHoursChange: (Float) -> Unit,
    onSessionLengthChange: (ExamSessionLength) -> Unit,
    onBreakPreferenceChange: (ExamBreakPreference) -> Unit,
    onNotificationPreferenceChange: (ExamNotificationPreference) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        CustomSlider(
            title = "Daily Study Hours",
            value = prefs.dailyStudyHours,
            onValueChange = onDailyStudyHoursChange,
            range = 3f..10f, // Changed
            steps = 13, // Changed
            valueLabel = { "${it} hrs" }
        )

        OutlinedPillSelector(
            title = "Preferred Session Length",
            options = ExamSessionLength.values().toList(),
            selectedOption = prefs.sessionLength,
            onOptionSelect = onSessionLengthChange,
            optionLabel = { "${it.minutes} min" }
        )

        RadioPillSelector(
            title = "Break Preference",
            options = ExamBreakPreference.values().toList(),
            selectedOption = prefs.breakPreference,
            onOptionSelect = onBreakPreferenceChange,
            optionLabel = {
                when (it) {
                    ExamBreakPreference.FIVE_MIN_AFTER_EVERY_SESSION -> "5 min after every session"
                    ExamBreakPreference.TEN_MIN_AFTER_TWO_SESSIONS -> "10 min after 2 sessions"
                }
            }
        )

        BinaryOutlinedPillSelector(
            title = "Notification Preference",
            options = ExamNotificationPreference.values().toList(),
            selectedOption = prefs.notificationPreference,
            onOptionSelect = onNotificationPreferenceChange,
            optionLabel = { it.name.lowercase().replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } }
        )
    }
}

@Composable
private fun FocusModePreferencesView(
    prefs: FocusModePreferences,
    onDailyStudyHoursChange: (Float) -> Unit,
    onSessionLengthChange: (FocusSessionLength) -> Unit,
    onBreakPreferenceChange: (FocusBreakPreference) -> Unit,
    onNotificationPreferenceChange: (FocusNotificationType, Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        CustomSlider(
            title = "Daily Study Hours",
            value = prefs.dailyStudyHours,
            onValueChange = onDailyStudyHoursChange,
            range = 1f..8f, // Changed
            steps = 6, // Changed
            valueLabel = { "${it.roundToInt()} hrs" }
        )

        OutlinedPillSelector(
            title = "Preferred Session Length",
            options = FocusSessionLength.values().toList(),
            selectedOption = prefs.sessionLength,
            onOptionSelect = onSessionLengthChange,
            optionLabel = { "${it.minutes} min" }
        )

        RadioPillSelector(
            title = "Break Preference",
            options = FocusBreakPreference.values().toList(),
            selectedOption = prefs.breakPreference,
            onOptionSelect = onBreakPreferenceChange,
            optionLabel = {
                when (it) {
                    FocusBreakPreference.FIVE_MIN_AFTER_EACH_SESSION -> "5 min after each session"
                    FocusBreakPreference.TEN_MIN_AFTER_EACH_SESSION -> "10 min after each session"
                }
            }
        )

        NotificationToggle(
            title = "Notification Preferences",
            options = prefs.notificationPreferences,
            onSelectionChange = onNotificationPreferenceChange
        )
    }
}

@Composable
private fun CasualModePreferencesView(
    prefs: CasualModePreferences,
    onDailyStudyHoursChange: (Float) -> Unit,
    onSessionLengthChange: (CasualSessionLength) -> Unit,
    onNotificationsEnabledChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        CustomSlider(
            title = "Daily Study Hours (Optional)",
            value = prefs.dailyStudyHours,
            onValueChange = onDailyStudyHoursChange,
            range = 0f..4f,
            steps = 7,
            valueLabel = { if (it > 0f) "${it} hrs" else "Off" }
        )

        OutlinedPillSelector(
            title = "Preferred Session Length",
            options = CasualSessionLength.values().toList(),
            selectedOption = prefs.sessionLength,
            onOptionSelect = onSessionLengthChange,
            optionLabel = { "${it.minutes} min" }
        )

        PreferenceSwitch(
            title = "Enable Notifications",
            checked = prefs.notificationsEnabled,
            onCheckedChange = onNotificationsEnabledChange
        )
    }
}

// --- Generic UI Components ---

@Composable
private fun CustomSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: (Float) -> String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = primaryTextColor, fontWeight = FontWeight.Medium)
            Text(valueLabel(value), style = MaterialTheme.typography.bodyMedium, color = secondaryTextColor)
        }

        Spacer(Modifier.height(4.dp))

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = primaryAccent,
                activeTrackColor = primaryAccent,
                inactiveTrackColor = sliderInactiveTrackColor
            ),
            modifier = Modifier.heightIn(min = 48.dp)
        )
    }
}

@Composable
private fun <T> OutlinedPillSelector(
    title: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelect: (T) -> Unit,
    optionLabel: (T) -> String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = primaryTextColor, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = selectedOption == option

                OutlinedButton(
                    onClick = { onOptionSelect(option) },
                    shape = CircleShape,
                    modifier = Modifier.height(40.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) selectionBackground else Color.Transparent,
                        contentColor = if (isSelected) primaryAccent else secondaryTextColor
                    ),
                    border = BorderStroke(1.dp, if (isSelected) primaryAccent else sliderInactiveTrackColor)
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(optionLabel(option), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun <T> BinaryOutlinedPillSelector(
    title: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelect: (T) -> Unit,
    optionLabel: (T) -> String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = primaryTextColor, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            require(options.size == 2)
            options.forEach { option ->
                val isSelected = selectedOption == option

                OutlinedButton(
                    onClick = { onOptionSelect(option) },
                    shape = CircleShape,
                    modifier = Modifier
                        .height(40.dp)
                        .weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) selectionBackground else Color.Transparent,
                        contentColor = if (isSelected) primaryAccent else secondaryTextColor
                    ),
                    border = BorderStroke(1.dp, if (isSelected) primaryAccent else sliderInactiveTrackColor)
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(optionLabel(option), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}


@Composable
private fun <T> RadioPillSelector(
    title: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelect: (T) -> Unit,
    optionLabel: (T) -> String
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = primaryTextColor, fontWeight = FontWeight.Medium)

        options.forEach { option ->
            val isSelected = selectedOption == option
            val border by animateColorAsState(if (isSelected) primaryAccent else sliderInactiveTrackColor)
            val background by animateColorAsState(if (isSelected) selectionBackground else surfaceBackground)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(CircleShape)
                    .clickable { onOptionSelect(option) },
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = background),
                border = BorderStroke(1.dp, border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(optionLabel(option), color = primaryTextColor, fontWeight = FontWeight.Medium)
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = primaryAccent)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationToggle(
    title: String,
    options: Set<FocusNotificationType>,
    onSelectionChange: (FocusNotificationType, Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = primaryTextColor, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { // Changed to Column
            FocusNotificationType.values().forEach { type ->
                PreferenceSwitch(
                    title = type.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                    checked = options.contains(type),
                    onCheckedChange = { isChecked ->
                        onSelectionChange(type, isChecked)
                    }
                )
            }
        }
    }
}

@Composable
private fun PreferenceSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = primaryTextColor)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = surfaceBackground,
                checkedTrackColor = primaryAccent,
                uncheckedThumbColor = surfaceBackground,
                uncheckedTrackColor = sliderInactiveTrackColor
            )
        )
    }
}

private fun generatePreviewText(uiState: ModeSelectionState): String {
    return when (uiState.selectedMode) {
        StudyMode.EXAM -> {
            val hours = uiState.examPreferences.dailyStudyHours
            "You’ll receive reminders for ${hours} hours of study in ${uiState.examPreferences.sessionLength.minutes}-minute sessions."
        }
        StudyMode.FOCUS -> {
            val hours = uiState.focusPreferences.dailyStudyHours.roundToInt()
            "StudyPilot will schedule ${hours} focused sessions per day with gentle reminders."
        }
        StudyMode.CASUAL -> {
            val hours = uiState.casualPreferences.dailyStudyHours
            if (hours > 0f) {
                "Suggestions for about ${hours} hour(s) of low-pressure study will appear on your dashboard."
            } else {
                "Study sessions will be entirely optional and self-directed."
            }
        }
    }
}
