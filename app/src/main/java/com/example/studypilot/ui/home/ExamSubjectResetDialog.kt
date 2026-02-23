package com.example.studypilot.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.studypilot.ui.theme.Roboto

private val examDialogPrimaryBlue = Color(0xFF1E88E5)
private val examDialogTextPrimary = Color(0xFF102A43)
private val examDialogTextSecondary = Color(0xFF627D98)
private val examDialogCardBorder = Color(0xFFE0E7F1)
private val examDialogBackground = Color(0xFFFFFFFF)
private val examDialogSectionBackground = Color(0xFFF5F9FF)

/**
 * Dialog shown when the exam date has passed.
 * Asks the user which subjects they still want to include in their daily plan
 * (e.g. pending subjects, or they can start a new revision round).
 *
 * @param examName          Name of the exam that just passed
 * @param subjectNames      All subjects from the exam plan
 * @param onSave            Called with the selected subject names to continue studying
 * @param onDismiss         Called when user dismisses (keeps all subjects)
 */
@Composable
fun ExamSubjectResetDialog(
    examName: String,
    subjectNames: List<String>,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val checkedSubjects = remember { mutableStateListOf<String>().also { it.addAll(subjectNames) } }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = examDialogBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                // Badge
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFFF4E5), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Exam Period Over",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE65100),
                        fontFamily = Roboto
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title
                Text(
                    text = "What's next for $examName?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = examDialogTextPrimary,
                    fontFamily = Roboto
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Subtitle
                Text(
                    text = "Your exam date has passed. Select the subjects you'd like to keep studying — great for revision or upcoming re-exams.",
                    fontSize = 13.sp,
                    color = examDialogTextSecondary,
                    fontFamily = Roboto,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Subject checkboxes
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(examDialogSectionBackground, RoundedCornerShape(12.dp))
                        .border(1.dp, examDialogCardBorder, RoundedCornerShape(12.dp))
                        .padding(vertical = 8.dp)
                ) {
                    subjectNames.forEachIndexed { index, subjectName ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (checkedSubjects.contains(subjectName)) {
                                        checkedSubjects.remove(subjectName)
                                    } else {
                                        checkedSubjects.add(subjectName)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checkedSubjects.contains(subjectName),
                                onCheckedChange = { isChecked ->
                                    if (isChecked) checkedSubjects.add(subjectName)
                                    else checkedSubjects.remove(subjectName)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = examDialogPrimaryBlue,
                                    uncheckedColor = examDialogTextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = subjectName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = examDialogTextPrimary,
                                fontFamily = Roboto,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (index < subjectNames.lastIndex) {
                            Divider(
                                color = examDialogCardBorder,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }

                if (checkedSubjects.isEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select at least one subject to continue.",
                        fontSize = 12.sp,
                        color = Color(0xFFD32F2F),
                        fontFamily = Roboto
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Keep all / dismiss
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, examDialogCardBorder)
                    ) {
                        Text(
                            "Keep All",
                            fontSize = 14.sp,
                            color = examDialogTextSecondary,
                            fontFamily = Roboto
                        )
                    }

                    // Save selection
                    Button(
                        onClick = {
                            if (checkedSubjects.isNotEmpty()) {
                                val ordered = subjectNames.filter { it in checkedSubjects }
                                onSave(ordered)
                            }
                        },
                        enabled = checkedSubjects.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = examDialogPrimaryBlue,
                            disabledContainerColor = examDialogPrimaryBlue.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            "Continue",
                            fontSize = 14.sp,
                            color = Color.White,
                            fontFamily = Roboto
                        )
                    }
                }
            }
        }
    }
}