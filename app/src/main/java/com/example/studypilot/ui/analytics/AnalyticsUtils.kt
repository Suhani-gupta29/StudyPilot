package com.example.studypilot.ui.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object AnalyticsColors {
    val primaryBlue = Color(0xFF1E88E5)
    val mainBackground = Color(0xFFFFFFFF)
    val cardBackground = Color(0xFFFFFFFF)
    val softDivider = Color(0xFFE6ECF5)
    val textSecondary = Color(0xFF627D98)
    val cardBorderStroke = BorderStroke(1.dp, Color(0xFFE0E7F1))
}

fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
