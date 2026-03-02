package com.example.studypilot.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.studypilot.ui.theme.Roboto

@Composable
fun HomeBottomNavigationBar(
    onNavigateToSettings: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToSubjects: () -> Unit,
    onNavigateToHome: () -> Unit = {},

    activeIndex: Int
) {
    val items = listOf("Home", "Planner", "Subjects", "Analytics", "Settings")
    val icons = listOf(Icons.Default.Home, Icons.Default.DateRange, Icons.Outlined.LibraryBooks, Icons.Outlined.Analytics, Icons.Default.Settings)

    val activeColor = Color(0xFF1E88E5)
    val inactiveColor = Color(0xFF9AA4B2)
    val pillBackgroundColor = Color(0xFFEAF2FF)
    val dividerColor = Color(0xFFE6ECF5)

    Column(modifier = Modifier.background(Color.White)) {
        Divider(color = dividerColor, thickness = 1.dp)
        NavigationBar(
            containerColor = Color.White,
            tonalElevation = 0.dp
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = activeIndex == index
                NavigationBarItem(
                    icon = {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) pillBackgroundColor else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icons[index], contentDescription = item)
                        }
                    },
                    label = { Text(item, fontFamily = Roboto, fontWeight = FontWeight.Medium) },
                    selected = isSelected,
                    onClick = {
                        when (index) {
                            0 -> onNavigateToHome()
                            1 -> onNavigateToPlanner()
                            2 -> onNavigateToSubjects()
                            3 -> onNavigateToAnalytics()
                            4 -> onNavigateToSettings()
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = activeColor,
                        unselectedIconColor = inactiveColor,
                        selectedTextColor = activeColor,
                        unselectedTextColor = inactiveColor,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}