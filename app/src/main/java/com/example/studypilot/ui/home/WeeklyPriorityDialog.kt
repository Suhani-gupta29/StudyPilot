package com.example.studypilot.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DragHandle
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

private val dialogPrimaryBlue = Color(0xFF1E88E5)
private val dialogTextPrimary = Color(0xFF102A43)
private val dialogTextSecondary = Color(0xFF627D98)
private val dialogCardBorder = Color(0xFFE0E7F1)
private val dialogBackground = Color(0xFFFFFFFF)
private val dialogSectionBackground = Color(0xFFF5F9FF)

/**
 * Dialog shown at the start of each week asking the user which subjects
 * they want to prioritize. The selected subjects will appear more frequently
 * in the generated daily plan for that week.
 *
 * @param subjectNames  Full list of subjects available in this mode
 * @param onSave        Called with the ordered list of SELECTED subject names when user taps Save
 * @param onDismiss     Called when user taps "Skip This Week"
 */
@Composable
fun WeeklyPriorityDialog(
    subjectNames: List<String>,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    // Keep track of which subjects the user has checked/ticked
    // Default: all selected so user can deselect what they don't want
    val checkedSubjects = remember { mutableStateListOf<String>().also { it.addAll(subjectNames) } }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                // Title
                Text(
                    text = "This Week's Focus",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = dialogTextPrimary,
                    fontFamily = Roboto
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Subtitle
                Text(
                    text = "Select subjects you want sessions scheduled for this week. Uncheck any you want to skip.",
                    fontSize = 13.sp,
                    color = dialogTextSecondary,
                    fontFamily = Roboto,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Subject list with checkboxes
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(dialogSectionBackground, RoundedCornerShape(12.dp))
                        .border(1.dp, dialogCardBorder, RoundedCornerShape(12.dp))
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
                                    checkedColor = dialogPrimaryBlue,
                                    uncheckedColor = dialogTextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = subjectName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = dialogTextPrimary,
                                fontFamily = Roboto,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Divider between items (not after last)
                        if (index < subjectNames.lastIndex) {
                            Divider(
                                color = dialogCardBorder,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }

                // Show warning if nothing selected
                val noneSelected = checkedSubjects.isEmpty()
                if (noneSelected) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select at least one subject to generate sessions.",
                        fontSize = 12.sp,
                        color = Color(0xFFD32F2F),
                        fontFamily = Roboto
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Skip button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, dialogCardBorder)
                    ) {
                        Text(
                            "Skip Week",
                            fontSize = 14.sp,
                            color = dialogTextSecondary,
                            fontFamily = Roboto
                        )
                    }

                    // Save button
                    Button(
                        onClick = {
                            if (checkedSubjects.isNotEmpty()) {
                                // Preserve the original order from subjectNames, filtered to checked ones
                                val ordered = subjectNames.filter { it in checkedSubjects }
                                onSave(ordered)
                            }
                        },
                        enabled = !noneSelected,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = dialogPrimaryBlue,
                            disabledContainerColor = dialogPrimaryBlue.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            "Save",
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