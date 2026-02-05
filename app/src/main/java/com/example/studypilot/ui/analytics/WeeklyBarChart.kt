package com.example.studypilot.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WeeklyBarChart(
    weeklyStudy: List<DayStudyUi>,
    modifier: Modifier = Modifier
) {
    if (weeklyStudy.isEmpty()) return

    // Set minimum scale to 1 hour (3600 seconds) for better visualization
    val maxSeconds = weeklyStudy.maxOf { it.studySeconds }.coerceAtLeast(3600)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Weekly Study Time",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            Text(
                text = "Last 7 days • Total: ${formatTotalTime(weeklyStudy.sumOf { it.studySeconds })}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(24.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val barSpacing = 16.dp.toPx()
                val totalSpacing = barSpacing * (weeklyStudy.size - 1)
                val barWidth = (size.width - totalSpacing) / weeklyStudy.size
                val chartHeight = size.height - 40.dp.toPx()

                weeklyStudy.forEachIndexed { index, day ->
                    // Calculate bar height proportionally
                    val barHeightRatio = if (maxSeconds > 0) {
                        day.studySeconds.toFloat() / maxSeconds
                    } else 0f

                    val barHeight = (chartHeight * barHeightRatio).coerceAtLeast(
                        if (day.studySeconds > 0) 4.dp.toPx() else 0f
                    )

                    val xPosition = index * (barWidth + barSpacing)

                    // Different colors for future/past/no data
                    val barColor = when {
                        day.isFuture -> Color(0xFFE2E8F0) // Gray for future
                        day.studySeconds == 0 -> Color(0xFFF1F5F9) // Light gray for no data
                        else -> Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF3B82F6),
                                Color(0xFF1E40AF)
                            )
                        )
                    }

                    // Draw bar
                    if (barColor is Brush) {
                        drawRoundRect(
                            brush = barColor,
                            topLeft = Offset(
                                x = xPosition,
                                y = size.height - barHeight - 30.dp.toPx()
                            ),
                            size = Size(
                                width = barWidth,
                                height = barHeight
                            ),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )
                    } else {
                        drawRoundRect(
                            color = barColor as Color,
                            topLeft = Offset(
                                x = xPosition,
                                y = size.height - barHeight - 30.dp.toPx()
                            ),
                            size = Size(
                                width = barWidth,
                                height = barHeight
                            ),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weeklyStudy.forEach { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = day.dayLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (day.isFuture) Color(0xFFCBD5E1) else Color(0xFF64748B)
                        )
                        Text(
                            text = formatShortTime(day.studySeconds),
                            fontSize = 10.sp,
                            color = if (day.studySeconds == 0) Color(0xFFCBD5E1) else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

private fun formatShortTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        seconds > 0 -> "${seconds}s"
        else -> "0m"
    }
}

private fun formatTotalTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}