package com.example.studypilot.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studypilot.ui.home.HomeBottomNavigationBar
import androidx.compose.ui.text.font.FontWeight


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    modeName: String,
    viewModel: AnalyticsViewModel = viewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToPlanner: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AnalyticsTopBar(
                modeName = modeName,
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            HomeBottomNavigationBar(
                onNavigateToHome = onNavigateToHome,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToPlanner = onNavigateToPlanner,
                onNavigateToAnalytics = {},
                activeIndex = 3
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->

        uiState?.let { state ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Today's Summary
                item { DailySummaryCard(state.dailySummary) }

                // Study Streak
                item {
                    StreakCard(
                        currentStreak = state.currentStreak,
                        longestStreak = state.longestStreak
                    )
                }

                // Weekly Trend
                item {
                    WeeklyTrendCard(
                        thisWeekSeconds = state.thisWeekSeconds,
                        lastWeekSeconds = state.lastWeekSeconds,
                        weeklyChange = state.weeklyChange
                    )
                }

                // Weekly Bar Chart
                item { WeeklyBarChart(state.weeklyStudy) }

                // Subject Pie Chart
                if (state.subjectStats.isNotEmpty()) {
                    item { SubjectPieChart(state.subjectStats) }
                }

                // Subject Details
                item { SubjectTimeBreakdown(state.subjectStats) }

                item { SubjectProgressSection(state.subjectStats) }
            }
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "📊",
                    fontSize = 48.sp
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "No data available yet",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "Start studying to see your analytics!",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}