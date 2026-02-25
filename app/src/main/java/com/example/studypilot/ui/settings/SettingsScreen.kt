package com.example.studypilot.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studypilot.data.UserPreferencesRepository
import com.example.studypilot.ui.analytics.AnalyticsColors
import com.example.studypilot.ui.auth.AuthViewModel
import com.example.studypilot.ui.home.HomeBottomNavigationBar
import com.example.studypilot.ui.home.HomeViewModel
import com.example.studypilot.ui.mode.CasualSessionLength
import com.example.studypilot.ui.mode.ExamSessionLength
import com.example.studypilot.ui.mode.FocusSessionLength
import com.example.studypilot.ui.mode.StudyMode
import com.example.studypilot.ui.theme.Roboto
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Colors matching SubjectsScreen exactly ────────────────────────────────────
private val primaryBlue    = Color(0xFF1E88E5)
private val gradientTop    = Color(0xFFE3F2FD)
private val gradientBottom = Color(0xFFFFFFFF)
private val cardBackground = Color(0xFFFFFFFF)
private val cardBorder     = Color(0xFFE0E7F1)
private val textPrimary    = Color(0xFF102A43)
private val textSecondary  = Color(0xFF627D98)
private val softDivider    = Color(0xFFE6ECF5)
private val selectionBg    = Color(0xFFDBEAFE)
private val inactiveTrack  = Color(0xFFCBD5E1)
private val dangerRed      = Color(0xFFE53935)
private val dangerRedDim   = Color(0x1AE53935)

// ── Entry point ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userPreferencesRepository: UserPreferencesRepository,
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSubjects: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(userPreferencesRepository, authViewModel)
    )
) {
    val state by settingsViewModel.uiState.collectAsState()
    val homeState by homeViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Keep SettingsViewModel in sync with the active mode
    val activeMode = homeState.selectedMode
    LaunchedEffect(activeMode) {
        settingsViewModel.setActiveMode(activeMode)
    }

    // Mode label shown in top bar — mirrors SubjectsScreen pattern
    val modeName = when (activeMode) {
        StudyMode.EXAM   -> "Exam Mode"
        StudyMode.FOCUS  -> "Focus Mode"
        StudyMode.CASUAL -> "Casual Mode"
    }

    LaunchedEffect(Unit) {
        settingsViewModel.effects.collectLatest { effect ->
            when (effect) {
                is SettingsEffect.NavigateToLogin -> onNavigateToLogin()
                is SettingsEffect.ShowSnackbar    -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        containerColor = Color.White,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = textPrimary,
                    contentColor = Color.White,
                    actionColor = primaryBlue,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        topBar = {
            // Identical structure to SubjectsScreen top bar
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "StudyPilot",
                            fontFamily = Roboto,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = AnalyticsColors.primaryBlue
                        )
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .border(
                                    width = 1.dp,
                                    color = AnalyticsColors.primaryBlue,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = modeName.uppercase(),
                                color = AnalyticsColors.primaryBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = Roboto
                            )
                        }
                    },
                    modifier = Modifier.background(Color.White),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        scrolledContainerColor = Color.White,
                        titleContentColor = AnalyticsColors.primaryBlue,
                        actionIconContentColor = AnalyticsColors.primaryBlue
                    )
                )
                Divider(color = AnalyticsColors.softDivider)
            }
        },
        bottomBar = {
            HomeBottomNavigationBar(
                onNavigateToHome = onNavigateToHome,
                onNavigateToPlanner = onNavigateToPlanner,
                onNavigateToAnalytics = onNavigateToAnalytics,
                onNavigateToSubjects = onNavigateToSubjects,
                onNavigateToSettings = { /* already here */ },
                activeIndex = 4
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = primaryBlue, strokeWidth = 2.dp)
            }
        } else {
            // Same gradient background as SubjectsScreen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(gradientTop, gradientBottom)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(
                            top = 16.dp,
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 80.dp  // same as SubjectsScreen contentPadding bottom
                        ),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    ProfileSection(
                        state = state,
                        onEditName = settingsViewModel::openNameEditDialog
                    )

                    StudyPreferencesSection(
                        state = state,
                        activeMode = activeMode,
                        onExamHoursChanged = settingsViewModel::onExamDailyHoursChanged,
                        onExamSessionLengthChanged = settingsViewModel::onExamSessionLengthChanged,
                        onFocusHoursChanged = settingsViewModel::onFocusDailyHoursChanged,
                        onFocusSessionLengthChanged = settingsViewModel::onFocusSessionLengthChanged,
                        onCasualHoursChanged = settingsViewModel::onCasualDailyHoursChanged,
                        onCasualSessionLengthChanged = settingsViewModel::onCasualSessionLengthChanged,
                        onChangeExamDate = settingsViewModel::openExamDateDialog,
                        onSave = settingsViewModel::saveStudyPreferences
                    )

                    AccountSection(onSignOut = settingsViewModel::onSignOutClicked)
                }
            }
        }
    }

    if (state.showExamDateDialog) {
        ExamDatePickerDialog(
            currentDateMillis = state.examDate,
            onConfirm = settingsViewModel::saveExamDate,
            onDismiss = settingsViewModel::cancelExamDateEdit
        )
    }

    if (state.showNameEditDialog) {
        EditNameDialog(
            name = state.pendingDisplayName,
            nameError = state.nameError,
            isSaving = state.isSaving,
            onNameChanged = settingsViewModel::onPendingNameChanged,
            onConfirm = settingsViewModel::saveDisplayName,
            onDismiss = settingsViewModel::cancelNameEdit
        )
    }

    if (state.showSignOutDialog) {
        SignOutDialog(
            onConfirm = settingsViewModel::confirmSignOut,
            onDismiss = settingsViewModel::cancelSignOut
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// SECTION HELPERS
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        color = Color.Black,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
        fontFamily = Roboto,
        modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.06f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(content = content)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 1. PROFILE SECTION
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun ProfileSection(state: SettingsState, onEditName: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Profile")
        SettingsCard {
            // Avatar + name row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Initials avatar
                val initials = state.displayName
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.first().uppercaseChar().toString() }
                    .ifEmpty { "?" }

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            brush = Brush.linearGradient(listOf(primaryBlue, Color(0xFF1565C0))),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Roboto
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.displayName.ifBlank { "Add your name" },
                        color = if (state.displayName.isBlank()) Color(0xFFB0BEC5) else textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Roboto
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = state.email.ifBlank { "—" },
                        color = textSecondary,
                        fontSize = 13.sp,
                        fontFamily = Roboto,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(selectionBg, CircleShape)
                        .clickable(onClick = onEditName),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit name",
                        tint = primaryBlue,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Divider(color = softDivider)

            // Email row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Outlined.Email, contentDescription = null, tint = textSecondary, modifier = Modifier.size(18.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Email", color = textSecondary, fontSize = 11.sp, fontFamily = Roboto)
                    Text(
                        text = state.email.ifBlank { "—" },
                        color = textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = Roboto
                    )
                }
                Box(
                    modifier = Modifier
                        .border(1.dp, primaryBlue, RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "READ ONLY",
                        color = primaryBlue,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = Roboto
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 2. STUDY PREFERENCES SECTION — shows only active mode, no toggle
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun StudyPreferencesSection(
    state: SettingsState,
    activeMode: StudyMode,
    onExamHoursChanged: (Float) -> Unit,
    onExamSessionLengthChanged: (ExamSessionLength) -> Unit,
    onFocusHoursChanged: (Float) -> Unit,
    onFocusSessionLengthChanged: (FocusSessionLength) -> Unit,
    onCasualHoursChanged: (Float) -> Unit,
    onCasualSessionLengthChanged: (CasualSessionLength) -> Unit,
    onChangeExamDate: () -> Unit,
    onSave: () -> Unit
) {
    val modeEmoji = when (activeMode) {
        StudyMode.EXAM   -> "📚"
        StudyMode.FOCUS  -> "🎯"
        StudyMode.CASUAL -> "☕"
    }
    val modeLabel = when (activeMode) {
        StudyMode.EXAM   -> "Exam Mode Preferences"
        StudyMode.FOCUS  -> "Focus Mode Preferences"
        StudyMode.CASUAL -> "Casual Mode Preferences"
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Study Preferences")
        SettingsCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Mode label — styled like ModeHeader in SubjectsScreen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$modeEmoji $modeLabel",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        fontFamily = Roboto
                    )
                }

                Divider(color = softDivider)

                // Show only the active mode content
                when (activeMode) {
                    StudyMode.EXAM -> ExamPrefContent(
                        hours = state.examDailyStudyHours,
                        sessionLength = state.examSessionLength,
                        examDate = state.examDate,
                        examName = state.examName,
                        onHoursChanged = onExamHoursChanged,
                        onSessionLengthChanged = onExamSessionLengthChanged,
                        onChangeExamDate = onChangeExamDate
                    )
                    StudyMode.FOCUS -> FocusPrefContent(
                        hours = state.focusDailyStudyHours,
                        sessionLength = state.focusSessionLength,
                        onHoursChanged = onFocusHoursChanged,
                        onSessionLengthChanged = onFocusSessionLengthChanged
                    )
                    StudyMode.CASUAL -> CasualPrefContent(
                        hours = state.casualDailyStudyHours,
                        sessionLength = state.casualSessionLength,
                        onHoursChanged = onCasualHoursChanged,
                        onSessionLengthChanged = onCasualSessionLengthChanged
                    )
                }

                Divider(color = softDivider)

                // Save button — same style as SubjectsScreen's OutlinedButton actions
                Button(
                    onClick = onSave,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryBlue,
                        contentColor = Color.White,
                        disabledContainerColor = primaryBlue.copy(alpha = 0.3f)
                    )
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            "Save Preferences",
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = Roboto,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

// ── Per-mode pref content ─────────────────────────────────────────────────────
@Composable
private fun ExamPrefContent(
    hours: Float,
    sessionLength: ExamSessionLength,
    examDate: Long?,
    examName: String?,
    onHoursChanged: (Float) -> Unit,
    onSessionLengthChanged: (ExamSessionLength) -> Unit,
    onChangeExamDate: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // ── Exam date row ──────────────────────────────────────────────────
        val dateLabel = if (examDate != null) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(examDate))
        } else {
            "Not set"
        }
        val daysLeft = if (examDate != null) {
            val diff = examDate - System.currentTimeMillis()
            val days = (diff / (1000 * 60 * 60 * 24)).toInt()
            when {
                days < 0  -> "Exam passed"
                days == 0 -> "Today!"
                days == 1 -> "Tomorrow"
                else      -> "$days days left"
            }
        } else null

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Exam Date",
                color = textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = Roboto
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                    .clickable(onClick = onChangeExamDate)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = primaryBlue,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (!examName.isNullOrBlank()) examName else "Exam",
                        color = textSecondary,
                        fontSize = 11.sp,
                        fontFamily = Roboto
                    )
                    Text(
                        text = dateLabel,
                        color = textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = Roboto
                    )
                }
                if (daysLeft != null) {
                    val chipColor = when {
                        daysLeft == "Exam passed" -> dangerRed
                        daysLeft == "Today!" || daysLeft == "Tomorrow" -> Color(0xFFF57C00)
                        else -> primaryBlue
                    }
                    Box(
                        modifier = Modifier
                            .border(1.dp, chipColor, RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = daysLeft,
                            color = chipColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = Roboto
                        )
                    }
                }
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Change exam date",
                    tint = textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        HoursSlider(hours = hours, onChanged = onHoursChanged, range = 3f..10f, steps = 13)
        ExamSessionLengthPills(selected = sessionLength, onSelected = onSessionLengthChanged)
    }
}

@Composable
private fun FocusPrefContent(
    hours: Float,
    sessionLength: FocusSessionLength,
    onHoursChanged: (Float) -> Unit,
    onSessionLengthChanged: (FocusSessionLength) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        HoursSlider(hours = hours, onChanged = onHoursChanged, range = 1f..6f, steps = 9)
        FocusSessionLengthPills(selected = sessionLength, onSelected = onSessionLengthChanged)
    }
}

@Composable
private fun CasualPrefContent(
    hours: Float,
    sessionLength: CasualSessionLength,
    onHoursChanged: (Float) -> Unit,
    onSessionLengthChanged: (CasualSessionLength) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        HoursSlider(hours = hours, onChanged = onHoursChanged, range = 0f..4f, steps = 7)
        CasualSessionLengthPills(selected = sessionLength, onSelected = onSessionLengthChanged)
    }
}

// ── Session length pill sets — concrete per enum, no T inference issues ────────
@Composable
private fun ExamSessionLengthPills(selected: ExamSessionLength, onSelected: (ExamSessionLength) -> Unit) {
    PillsRow(
        label = "Session Length",
        items = ExamSessionLength.values().toList(),
        selected = selected,
        onSelected = onSelected,
        minutesOf = { it.minutes }
    )
}

@Composable
private fun FocusSessionLengthPills(selected: FocusSessionLength, onSelected: (FocusSessionLength) -> Unit) {
    PillsRow(
        label = "Session Length",
        items = FocusSessionLength.values().toList(),
        selected = selected,
        onSelected = onSelected,
        minutesOf = { it.minutes }
    )
}

@Composable
private fun CasualSessionLengthPills(selected: CasualSessionLength, onSelected: (CasualSessionLength) -> Unit) {
    PillsRow(
        label = "Session Length",
        items = CasualSessionLength.values().toList(),
        selected = selected,
        onSelected = onSelected,
        minutesOf = { it.minutes }
    )
}

@Composable
private fun <T> PillsRow(
    label: String,
    items: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    minutesOf: (T) -> Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = Roboto)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { option ->
                val isSelected = option == selected
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) primaryBlue else inactiveTrack,
                    label = "pill_border_${minutesOf(option)}"
                )
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) selectionBg else cardBackground,
                    label = "pill_bg_${minutesOf(option)}"
                )
                OutlinedButton(
                    onClick = { onSelected(option) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(40.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = bgColor,
                        contentColor = if (isSelected) primaryBlue else textSecondary
                    ),
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(3.dp))
                    }
                    Text(
                        text = "${minutesOf(option)}m",
                        fontSize = 13.sp,
                        fontFamily = Roboto,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

// ── Hours slider ──────────────────────────────────────────────────────────────
@Composable
private fun HoursSlider(
    hours: Float,
    onChanged: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Daily Study Hours",
                color = textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = Roboto
            )
            Box(
                modifier = Modifier
                    .border(1.dp, primaryBlue, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = formatHours(hours),
                    color = primaryBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Roboto
                )
            }
        }
        Slider(
            value = hours,
            onValueChange = onChanged,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = primaryBlue,
                activeTrackColor = primaryBlue,
                inactiveTrackColor = inactiveTrack
            ),
            modifier = Modifier.heightIn(min = 48.dp)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = if (range.start == 0f) "Off" else "${range.start.toInt()}h",
                color = textSecondary,
                fontSize = 11.sp,
                fontFamily = Roboto
            )
            Text(
                text = "${range.endInclusive.toInt()}h",
                color = textSecondary,
                fontSize = 11.sp,
                fontFamily = Roboto
            )
        }
    }
}

private fun formatHours(hours: Float): String {
    if (hours == 0f) return "Off"
    val h = hours.toInt()
    val m = ((hours - h) * 60).toInt()
    return when {
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else   -> "${h}h ${m}m"
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 3. ACCOUNT SECTION
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun AccountSection(onSignOut: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Account")
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSignOut)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(dangerRedDim, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Logout, contentDescription = null, tint = dangerRed, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = "Sign Out",
                    color = dangerRed,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = Roboto,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// DIALOGS
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun EditNameDialog(
    name: String,
    nameError: String?,
    isSaving: Boolean,
    onNameChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBackground,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text("Edit Name", color = textPrimary, fontWeight = FontWeight.Bold, fontFamily = Roboto, fontSize = 17.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChanged,
                    placeholder = { Text("Your name", color = textSecondary, fontFamily = Roboto) },
                    isError = nameError != null,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryBlue,
                        unfocusedBorderColor = cardBorder,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        cursorColor = primaryBlue,
                        errorBorderColor = dangerRed
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (nameError != null) {
                    Text(nameError, color = dangerRed, fontSize = 12.sp, fontFamily = Roboto)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isSaving
            ) {
                Text("Save", color = primaryBlue, fontWeight = FontWeight.SemiBold, fontFamily = Roboto)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textSecondary, fontFamily = Roboto)
            }
        }
    )
}

@Composable
private fun SignOutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBackground,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text("Sign Out?", color = textPrimary, fontWeight = FontWeight.Bold, fontFamily = Roboto, fontSize = 17.sp)
        },
        text = {
            Text(
                "You'll be returned to the login screen. Your study data will be kept safe.",
                color = textSecondary,
                fontSize = 14.sp,
                fontFamily = Roboto,
                lineHeight = 21.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Sign Out", color = dangerRed, fontWeight = FontWeight.SemiBold, fontFamily = Roboto)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textSecondary, fontFamily = Roboto)
            }
        }
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// EXAM DATE PICKER DIALOG
// ══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamDatePickerDialog(
    currentDateMillis: Long?,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = currentDateMillis
            ?: (System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = true
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { onConfirm(it) }
                },
                enabled = datePickerState.selectedDateMillis != null
            ) {
                Text("Confirm", color = primaryBlue, fontWeight = FontWeight.SemiBold, fontFamily = Roboto)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textSecondary, fontFamily = Roboto)
            }
        },
        colors = DatePickerDefaults.colors(
            containerColor = cardBackground,
            titleContentColor = textPrimary,
            headlineContentColor = textPrimary,
            weekdayContentColor = textSecondary,
            subheadContentColor = textSecondary,
            navigationContentColor = textPrimary,
            yearContentColor = textPrimary,
            currentYearContentColor = primaryBlue,
            selectedYearContainerColor = primaryBlue,
            selectedYearContentColor = Color.White,
            dayContentColor = textPrimary,
            selectedDayContainerColor = primaryBlue,
            selectedDayContentColor = Color.White,
            todayContentColor = primaryBlue,
            todayDateBorderColor = primaryBlue
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    "  Change Exam Date",
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Roboto,
                    fontSize = 17.sp
                )
            },
            headline = {
                val formatted = datePickerState.selectedDateMillis?.let {
                    SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(it))
                } ?: "Select a date"
                Text(
                    formatted,
                    modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
                    color = primaryBlue,
                    fontFamily = Roboto,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            },
            showModeToggle = true
        )
    }
}