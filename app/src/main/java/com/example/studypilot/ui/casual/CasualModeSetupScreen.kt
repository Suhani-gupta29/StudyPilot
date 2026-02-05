
package com.example.studypilot.ui.casual

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studypilot.R
import com.example.studypilot.ui.shared.Subject
import java.text.SimpleDateFormat
import java.util.*

private val backgroundWhite = Color(0xFFFFFFFF)
private val softBlueTint = Color(0xFFF0F8FF)
private val lightBlue = Color(0xFF8B9BCA)
private val primaryBlue = Color(0xFF4A65A0)
private val textDarkBlue = Color(0xFF0D47A1)
private val inputBackground = Color(0xFFF5FAFF)
private val gradientButtonStart = Color(0xFF1E88E5)
private val gradientButtonEnd = Color(0xFF1565C0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasualModeSetupScreen(
    viewModel: CasualModeSetupViewModel = viewModel(),
    onContinueClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = backgroundWhite,
        bottomBar = {
            BottomActionBar(
                onContinueClick = {
                    viewModel.savePreferences()
                    onContinueClick()
                },
                onBackClick = onBackClick,
                isContinueEnabled = true
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            TopVisualSection()
            Header()
            SubjectsSection(uiState, viewModel)
            TasksSection(uiState, viewModel)
        }
    }
}

@Composable
private fun TopVisualSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.image_6),
            contentDescription = "Casual Mode Illustration",
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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Casual Study Mode",
            color = textDarkBlue,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Set gentle preferences — or skip everything.",
            color = lightBlue,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SubjectsSection(uiState: CasualModeSetupState, viewModel: CasualModeSetupViewModel) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("What do you feel like studying?", style = MaterialTheme.typography.titleMedium, color = textDarkBlue)
        Text("Choose anything", style = MaterialTheme.typography.bodySmall, color = lightBlue)
        Spacer(modifier = Modifier.height(16.dp))

        uiState.subjects.forEachIndexed { index, subject ->
            CasualSubjectCard(
                subject = subject,
                viewModel = viewModel,
                index = index,
                onRemove = { viewModel.removeSubject(index) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedButton(
            onClick = { viewModel.addSubject() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, primaryBlue)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Subject", tint = primaryBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add a Subject", color = primaryBlue)
        }
    }
}

@Composable
private fun CasualSubjectCard(subject: Subject, viewModel: CasualModeSetupViewModel, index: Int, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = softBlueTint)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                CustomTextField(value = subject.name, onValueChange = { viewModel.onSubjectNameChange(index, it) }, placeholder = "e.g. History, Art, Programming", label = "Subject")
            }
            IconButton(onClick = onRemove, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Default.Delete, contentDescription = "Remove Subject")
            }
        }
    }
}

@Composable
private fun TasksSection(uiState: CasualModeSetupState, viewModel: CasualModeSetupViewModel) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Anything coming up?", style = MaterialTheme.typography.titleMedium, color = textDarkBlue)
        Text("We’ll gently remind you — nothing is forced.", style = MaterialTheme.typography.bodySmall, color = lightBlue)
        Spacer(modifier = Modifier.height(16.dp))

        uiState.tasks.forEachIndexed { index, task ->
            CasualTaskCard(
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
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, primaryBlue)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Task", tint = primaryBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Task", color = primaryBlue)
        }
    }
}

@Composable
private fun CasualTaskCard(
    task: CasualTask,
    viewModel: CasualModeSetupViewModel,
    index: Int,
    onRemove: () -> Unit,
    subjects: List<Subject>
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = softBlueTint.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                CustomTextField(value = task.name, onValueChange = { viewModel.onTaskNameChange(index, it) }, placeholder = "e.g. Project Idea, Reading Chapter 5", label = "Task")
                Spacer(modifier = Modifier.height(16.dp))
                val formattedDate = if (task.dueDate != null) {
                    SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(Date(task.dueDate!!))
                } else {
                    "Due by (optional)"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(inputBackground, RoundedCornerShape(16.dp))
                        .clickable { datePickerDialog.show() }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formattedDate, color = if (task.dueDate != null) Color.Black else lightBlue)
                    Icon(Icons.Default.CalendarToday, contentDescription = "Select Date", tint = primaryBlue)
                }
                Spacer(modifier = Modifier.height(16.dp))
                val subjectNames = subjects.map { it.name }.filter { it.isNotBlank() }
                if (subjectNames.isNotEmpty()) {
                    Dropdown(
                        label = "Related Subject (Optional)",
                        items = listOf("None") + subjectNames,
                        selectedValue = task.relatedSubject?.takeIf { subjectNames.contains(it) } ?: "None",
                        onItemSelected = { selected -> viewModel.onTaskSubjectChange(index, selected.takeIf { it != "None" }) }
                    )
                }
            }
            IconButton(onClick = onRemove, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Default.Delete, contentDescription = "Remove Task")
            }
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
            .background(backgroundWhite)
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
            Text("Back")
        }
        Spacer(modifier = Modifier.width(16.dp))
        Button(
            onClick = onContinueClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            enabled = isContinueEnabled,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = lightBlue),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(colors = if (isContinueEnabled) listOf(gradientButtonStart, gradientButtonEnd) else listOf(lightBlue, lightBlue))),
                contentAlignment = Alignment.Center
            ) {
                Text("Continue", color = Color.White, fontSize = 16.sp, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTextField(value: String, onValueChange: (String) -> Unit, placeholder: String, label: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = textDarkBlue, modifier = Modifier.padding(bottom = 8.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = lightBlue) },
            modifier = Modifier
                .fillMaxWidth()
                .background(color = inputBackground, shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = primaryBlue
            )
        )
    }
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

    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = textDarkBlue, modifier = Modifier.padding(bottom = 8.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            TextField(
                value = selectedValue,
                onValueChange = { },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .background(color = inputBackground, shape = RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = primaryBlue
                )
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(text = item) },
                        onClick = {
                            onItemSelected(item)
                            expanded = false
                        })
                }
            }
        }
    }
}
