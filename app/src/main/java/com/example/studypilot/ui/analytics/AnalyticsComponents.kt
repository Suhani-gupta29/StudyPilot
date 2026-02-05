package com.example.studypilot.ui.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.studypilot.ui.theme.Roboto

@Composable
fun DailySummaryCard(
    summary: DailySummaryUi,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Today's Summary",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryStatCard(
                    icon = Icons.Default.Schedule,
                    title = "Study Time",
                    value = formatTime(summary.totalStudySeconds),
                    iconColor = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(12.dp))

                SummaryStatCard(
                    icon = Icons.Default.TrendingUp,
                    title = "Sessions",
                    value = summary.sessionCount.toString(),
                    iconColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(12.dp))

                SummaryStatCard(
                    icon = Icons.Default.CheckCircle,
                    title = "Completion",
                    value = "${(summary.completionRate * 100).toInt()}%",
                    iconColor = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryStatCard(
    icon: ImageVector,
    title: String,
    value: String,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            Text(
                text = title,
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

private fun formatTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontFamily = Roboto,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            fontFamily = Roboto,
            color = AnalyticsColors.textSecondary
        )
    }
}

@Composable
fun SubjectTimeBreakdown(subjects: List<SubjectStatUi>) {
    AnalyticsCard(title = "Time by Subject") {
        subjects.forEach {
            AnalyticsRow(
                left = it.subjectName,
                right = formatDuration(it.totalStudySeconds)
            )
        }
    }
}

@Composable
fun SubjectProgressSection(subjects: List<SubjectStatUi>) {
    AnalyticsCard(title = "Subject Progress") {
        subjects.forEach {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                Text(
                    it.subjectName,
                    fontFamily = Roboto,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Sessions: ${it.sessionCount} • Avg: ${formatDuration(it.averageSessionSeconds)}",
                    color = AnalyticsColors.textSecondary,
                    fontFamily = Roboto
                )
            }
        }
    }
}

@Composable
private fun AnalyticsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AnalyticsColors.cardBackground),
        border = AnalyticsColors.cardBorderStroke
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                fontFamily = Roboto,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun AnalyticsRow(left: String, right: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(left, fontFamily = Roboto)
        Text(right, fontFamily = Roboto)
    }
}
