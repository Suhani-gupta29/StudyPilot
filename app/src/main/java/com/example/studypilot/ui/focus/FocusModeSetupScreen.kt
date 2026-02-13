package com.example.studypilot.ui.focus

import android.app.DatePickerDialog
import android.util.Log
import android.widget.DatePicker
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studypilot.R
import com.example.studypilot.ui.shared.Difficulty
import com.example.studypilot.ui.shared.Effort
import com.example.studypilot.ui.shared.FocusSubject
import com.example.studypilot.ui.shared.FocusTask
import com.example.studypilot.ui.shared.Priority
import com.example.studypilot.ui.shared.TaskType
import java.text.SimpleDateFormat
import java.util.*

private val primaryBlue = Color(0xFF1E88E5)
private val lightBlue = Color(0xFF90CAF9)
private val softBlueTint = Color(0xFFE3F2FD)
private val textDarkBlue = Color(0xFF0D47A1)
private val dividerBlue = Color(0xFFBBDEFB)
private val gradientTop = Color(0xFFE3F2FD)
private val gradientBottom = Color(0xFFFFFFFF)
private val gradientButtonStart = Color(0xFF1E88E5)
private val gradientButtonEnd = Color(0xFF1565C0)
private val inputBackground = Color(0xFFF5FAFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusModeSetupScreen(
    viewModel: FocusModeSetupViewModel = viewModel(),
    onContinueClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            BottomActionBar(
                onContinueClick = onContinueClick,
                onBackClick = onBackClick,
                isContinueEnabled = true
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(gradientTop, gradientBottom)))
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            TopVisualSection()
            Spacer(modifier = Modifier.height(24.dp))
            Header()
            Spacer(modifier = Modifier.height(16.dp))
            SubjectsSection(uiState, viewModel)
            Spacer(modifier = Modifier.height(24.dp))
            TasksSection(uiState, viewModel)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TopVisualSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.image_5),
            contentDescription = "Focus Mode Illustration",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Set up your study context",
            color = textDarkBlue,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This helps us balance your days — you can change this anytime.",
            color = lightBlue,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SubjectsSection(uiState: FocusModeSetupState, viewModel: FocusModeSetupViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = primaryBlue.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Subjects you study",
                style = MaterialTheme.typography.titleMedium,
                color = textDarkBlue,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Used to balance your daily sessions — not to enforce rules.",
                style = MaterialTheme.typography.bodySmall,
                color = lightBlue
            )
            Spacer(modifier = Modifier.height(16.dp))

            uiState.subjects.forEachIndexed { index, subject ->
                FocusSubjectCard(
                    subject = subject,
                    viewModel = viewModel,
                    index = index,
                    onRemove = { viewModel.removeSubject(index) },
                    isRemoveEnabled = uiState.subjects.size > 1
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedButton(
                onClick = { viewModel.addSubject() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, primaryBlue)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Subject", tint = primaryBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Subject", color = primaryBlue)
            }
        }
    }
}

@Composable
private fun FocusSubjectCard(
    subject: FocusSubject,
    viewModel: FocusModeSetupViewModel,
    index: Int,
    onRemove: () -> Unit,
    isRemoveEnabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(softBlueTint, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Subject",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = textDarkBlue
            )
            if (isRemoveEnabled) {
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove Subject",
                        tint = textDarkBlue.copy(alpha = 0.7f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        CustomTextField(
            value = subject.name,
            onValueChange = { viewModel.onSubjectNameChange(index, it) },
            placeholder = "e.g. Data Structures, Math, OS",
            label = "Subject Name"
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) {
                Dropdown(
                    label = "Difficulty",
                    items = Difficulty.values().map { it.name },
                    selectedValue = subject.difficulty.name,
                    onItemSelected = { viewModel.onSubjectDifficultyChange(index, Difficulty.valueOf(it)) }
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                Dropdown(
                    label = "Priority",
                    items = Priority.values().map { it.name },
                    selectedValue = subject.priority.name,
                    onItemSelected = { viewModel.onSubjectPriorityChange(index, Priority.valueOf(it)) }
                )
            }
        }
    }
}

@Composable
private fun TasksSection(uiState: FocusModeSetupState, viewModel: FocusModeSetupViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = primaryBlue.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Anything coming up?",
                style = MaterialTheme.typography.titleMedium,
                color = textDarkBlue,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "We'll gently suggest prep sessions — nothing is forced.",
                style = MaterialTheme.typography.bodySmall,
                color = lightBlue
            )
            Spacer(modifier = Modifier.height(16.dp))

            uiState.tasks.forEachIndexed { index, task ->
                FocusTaskCard(
                    task = task,
                    viewModel = viewModel,
                    index = index,
                    onRemove = { viewModel.removeTask(index) },
                    subjects = uiState.subjects
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedButton(
                onClick = { viewModel.addTask() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, primaryBlue)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task", tint = primaryBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Task", color = primaryBlue)
            }
        }
    }
}

@Composable
private fun FocusTaskCard(
    task: FocusTask,
    viewModel: FocusModeSetupViewModel,
    index: Int,
    onRemove: () -> Unit,
    subjects: List<FocusSubject>
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    if (task.dueDate != null) {
        calendar.timeInMillis = task.dueDate!!
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(year, month, dayOfMonth)
            viewModel.onTaskDueDateChange(index, selectedCalendar.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    datePickerDialog.datePicker.minDate = System.currentTimeMillis()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(softBlueTint, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Task",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = textDarkBlue
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove Task",
                    tint = textDarkBlue.copy(alpha = 0.7f)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        CustomTextField(
            value = task.name,
            onValueChange = { viewModel.onTaskNameChange(index, it) },
            placeholder = "e.g. Unit Test 2, PPT on OS",
            label = "Task Name"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) {
                Dropdown(
                    label = "Task Type",
                    items = TaskType.values().map { it.name },
                    selectedValue = task.type.name,
                    onItemSelected = { viewModel.onTaskTypeChange(index, TaskType.valueOf(it)) }
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                Dropdown(
                    label = "Effort",
                    items = Effort.values().map { it.name },
                    selectedValue = task.effort.name,
                    onItemSelected = { viewModel.onTaskEffortChange(index, Effort.valueOf(it)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Date Picker
        val formattedDate = if (task.dueDate != null) {
            SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(Date(task.dueDate!!))
        } else {
            "Due by (approximate)"
        }

        Column {
            Text(
                text = "Due Date",
                style = MaterialTheme.typography.labelMedium,
                color = textDarkBlue,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(inputBackground, RoundedCornerShape(16.dp))
                    .border(1.dp, dividerBlue, RoundedCornerShape(16.dp))
                    .clickable {
                        Log.d("FocusUI-debug", "Date row clicked; showing date picker")
                        datePickerDialog.show()
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formattedDate,
                    color = if (task.dueDate != null) Color.Black else lightBlue
                )
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = "Select Date",
                    tint = primaryBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subject Dropdown
        val subjectNames = subjects.map { it.name }.filter { it.isNotBlank() }
        if (subjectNames.isNotEmpty()) {
            Dropdown(
                label = "Related Subject",
                items = subjectNames,
                selectedValue = task.relatedSubject?.takeIf { subjectNames.contains(it) }
                    ?: subjectNames.first(),
                onItemSelected = { viewModel.onTaskSubjectChange(index, it) }
            )
        }
    }
}

@Composable
private fun BottomActionBar(
    onContinueClick: () -> Unit,
    onBackClick: () -> Unit,
    isContinueEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, primaryBlue)
        ) {
            Text("Back", color = primaryBlue, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Button(
            onClick = onContinueClick,
            enabled = isContinueEnabled,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = lightBlue
            ),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = if (isContinueEnabled) listOf(
                                gradientButtonStart,
                                gradientButtonEnd
                            ) else listOf(lightBlue, lightBlue)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Continue",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    label: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor = if (isFocused) primaryBlue else dividerBlue

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = textDarkBlue) },
        placeholder = { Text(placeholder, color = lightBlue) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = borderColor,
            unfocusedBorderColor = borderColor,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            cursorColor = primaryBlue,
            focusedContainerColor = inputBackground,
            unfocusedContainerColor = inputBackground,
        ),
        interactionSource = interactionSource,
        singleLine = true,
        textStyle = TextStyle(fontSize = 16.sp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dropdown(
    label: String,
    items: List<String>,
    selectedValue: String,
    onItemSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = textDarkBlue) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .clickable {
                    expanded = !expanded
                    Log.d("FocusUI-debug", "Dropdown '$label' toggled to $expanded")
                },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryBlue,
                unfocusedBorderColor = dividerBlue,
                focusedContainerColor = inputBackground,
                unfocusedContainerColor = inputBackground
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        Log.d("FocusUI-debug", "Dropdown '$label' selected: $item")
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}