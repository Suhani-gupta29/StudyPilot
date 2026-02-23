package com.example.studypilot.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studypilot.ui.theme.Roboto

private val sheetPrimaryBlue = Color(0xFF1E88E5)
private val sheetTextPrimary = Color(0xFF102A43)
private val sheetTextSecondary = Color(0xFF627D98)
private val sheetCardBorder = Color(0xFFE0E7F1)
private val sheetBackground = Color(0xFFFFFFFF)
private val sheetSectionBackground = Color(0xFFF5F9FF)
private val sheetCompletedGreen = Color(0xFF2E7D32)

/**
 * Modal bottom sheet that lets the user pick a different subject for a specific session.
 *
 * @param sessionNumber     The 1-based number of the session being changed (shown in title)
 * @param currentSubject    The subject currently assigned to this session
 * @param availableSubjects All subjects the user has added for this mode
 * @param onSubjectSelected Called with the new subject name when user taps one
 * @param onDismiss         Called when the sheet is dismissed without a selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectChangeBottomSheet(
    sessionNumber: Int,
    currentSubject: String,
    availableSubjects: List<String>,
    onSubjectSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBackground,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Text(
                text = "Change Subject — Session $sessionNumber",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = sheetTextPrimary,
                fontFamily = Roboto
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pick a subject to replace \"$currentSubject\" for today.",
                fontSize = 13.sp,
                color = sheetTextSecondary,
                fontFamily = Roboto,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Subject options
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(sheetSectionBackground, RoundedCornerShape(12.dp))
                    .border(1.dp, sheetCardBorder, RoundedCornerShape(12.dp))
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                availableSubjects.forEachIndexed { index, subjectName ->
                    val isCurrentlySelected = subjectName == currentSubject

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isCurrentlySelected) {
                                onSubjectSelected(subjectName)
                            }
                            .background(
                                if (isCurrentlySelected) sheetPrimaryBlue.copy(alpha = 0.07f)
                                else Color.Transparent
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = subjectName,
                                fontSize = 14.sp,
                                fontWeight = if (isCurrentlySelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isCurrentlySelected) sheetPrimaryBlue else sheetTextPrimary,
                                fontFamily = Roboto
                            )
                            if (isCurrentlySelected) {
                                Text(
                                    text = "Current subject",
                                    fontSize = 12.sp,
                                    color = sheetPrimaryBlue.copy(alpha = 0.7f),
                                    fontFamily = Roboto
                                )
                            }
                        }

                        if (isCurrentlySelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Selected",
                                tint = sheetPrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (index < availableSubjects.lastIndex) {
                        Divider(
                            color = sheetCardBorder,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cancel button
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, sheetCardBorder)
            ) {
                Text(
                    "Cancel",
                    fontSize = 14.sp,
                    color = sheetTextSecondary,
                    fontFamily = Roboto
                )
            }
        }
    }
}