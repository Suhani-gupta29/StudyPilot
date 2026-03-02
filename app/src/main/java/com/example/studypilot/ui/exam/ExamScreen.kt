package com.example.studypilot.ui.exam

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studypilot.R
import com.example.studypilot.ui.shared.Difficulty
import com.example.studypilot.ui.shared.Priority
import com.example.studypilot.ui.shared.Subject
import java.text.SimpleDateFormat
import java.util.*

// --- New Color System ---
private val primaryBlue = Color(0xFF1E88E5)
private val lightBlue = Color(0xFF0861C4)
private val softBlueTint = Color(0xFFEAF3FB)
private val textDarkBlue = Color(0xFF0861C4)
private val dividerBlue = Color(0xFFCBD5E1)
private val gradientTop = Color(0xFFDBE9FF)
private val gradientBottom = Color(0xFF9ECFFA)
private val gradientButtonStart = Color(0xFF1E88E5)
private val gradientButtonEnd = Color(0xFF1565C0)
private val inputBackground = Color(0xFFF5FAFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    viewModel: ExamViewModel = viewModel(),
    onContinueClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val calendar = Calendar.getInstance()
    val examDateVal = uiState.examDate
    if (examDateVal != null) {
        calendar.timeInMillis = examDateVal
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(year, month, dayOfMonth)
            viewModel.onExamDateChange(selectedCalendar.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    datePickerDialog.datePicker.minDate = System.currentTimeMillis()


    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            BottomButtons(
                onContinueClick = onContinueClick,
                onCancelClick = onCancelClick,
                isContinueEnabled = uiState.examName.isNotBlank() && uiState.examDate != null && uiState.subjects.all { it.name.isNotBlank() }
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
            InputSection(uiState, viewModel) { datePickerDialog.show() }
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
            painter = painterResource(id = R.drawable.image_4),
            contentDescription = "Exam Illustration",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
fun InputSection(uiState: ExamInputState, viewModel: ExamViewModel, onDateClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(24.dp), spotColor = primaryBlue.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            CustomTextField(
                value = uiState.examName,
                onValueChange = { viewModel.onExamNameChange(it) },
                placeholder = "Enter exam name",
                label = "Exam Name"
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Date Picker
            val formattedDate = uiState.examDate?.let { SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(Date(it)) } ?: "Select exam date"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(inputBackground, RoundedCornerShape(16.dp))
                    .border(1.dp, dividerBlue, RoundedCornerShape(16.dp))
                    .clickable {
                        Log.d("ExamUI-debug", "Date row clicked; showing date picker")
                        onDateClick()
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formattedDate, color = if (uiState.examDate != null) Color.Black else lightBlue)
                Icon(Icons.Default.CalendarToday, contentDescription = "Select Date", tint = primaryBlue)
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Subjects
            uiState.subjects.forEachIndexed { index, subject ->
                SubjectInput(
                    subject = subject,
                    onNameChange = { name -> viewModel.onSubjectNameChange(index, name) },
                    onPriorityChange = { priority -> viewModel.onPriorityChange(index, priority) },
                    onDifficultyChange = { difficulty -> viewModel.onDifficultyChange(index, difficulty) },
                    onRemove = { viewModel.removeSubject(index) },
                    isRemoveEnabled = uiState.subjects.size > 1
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedButton(
                onClick = { viewModel.addSubject() },
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
fun CustomTextField(value: String, onValueChange: (String) -> Unit, placeholder: String, label: String) {
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

@Composable
fun SubjectInput(
    subject: Subject,
    onNameChange: (String) -> Unit,
    onPriorityChange: (Priority) -> Unit,
    onDifficultyChange: (Difficulty) -> Unit,
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
            Text("Subject", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textDarkBlue)
            if (isRemoveEnabled) {
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove Subject", tint = textDarkBlue.copy(alpha = 0.7f))
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        CustomTextField(
            value = subject.name,
            onValueChange = onNameChange,
            placeholder = "Subject Name",
            label = "Subject"
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) {
                Dropdown(
                    label = "Priority",
                    items = Priority.entries.map { it.name },
                    selectedValue = subject.priority.name,
                    onItemSelected = { onPriorityChange(Priority.valueOf(it)) }
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                Dropdown(
                    label = "Difficulty",
                    items = Difficulty.entries.map { it.name },
                    selectedValue = subject.difficulty.name,
                    onItemSelected = { onDifficultyChange(Difficulty.valueOf(it)) }
                )
            }
        }
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

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        // Make the text field clickable and log toggles; some Compose fields don't automatically
        // toggle ExposedDropdownMenuBox, so we attach an explicit clickable to open/close the menu.
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
                    Log.d("ExamUI-debug", "Dropdown '$label' toggled to $expanded")
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
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item, color = Color(0xFF0A2540), fontSize = 15.sp) },
                    onClick = {
                        Log.d("ExamUI-debug", "Dropdown '$label' selected: $item")
                        onItemSelected(item)
                        expanded = false
                    },
                    colors = MenuItemColors(
                        textColor = Color(0xFF0A2540),
                        leadingIconColor = Color(0xFF0A2540),
                        trailingIconColor = Color(0xFF0A2540),
                        disabledTextColor = Color(0xFF0A2540).copy(alpha = 0.4f),
                        disabledLeadingIconColor = Color.Transparent,
                        disabledTrailingIconColor = Color.Transparent
                    )
                )
            }
        }
    }
}


@Composable
private fun BottomButtons(
    onContinueClick: () -> Unit,
    onCancelClick: () -> Unit,
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
            onClick = onCancelClick,
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
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color(0xFFB0CDE8)),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(colors = if(isContinueEnabled) listOf(gradientButtonStart, gradientButtonEnd) else listOf(Color(0xFFB0CDE8), Color(0xFFB0CDE8)))),
                contentAlignment = Alignment.Center
            ) {
                Text("Continue", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
