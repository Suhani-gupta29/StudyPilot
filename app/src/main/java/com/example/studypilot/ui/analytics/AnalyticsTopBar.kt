package com.example.studypilot.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.studypilot.ui.theme.Roboto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsTopBar(
    modeName: String,
    scrollBehavior: TopAppBarScrollBehavior
) {
    Column {
        TopAppBar(
            title = {
                Text(
                    text = "StudyPilot",
                    fontFamily = Roboto,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = AnalyticsColors.primaryBlue
                )
            },
            actions = {
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .border(
                            width = 1.dp,
                            color = AnalyticsColors.primaryBlue,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = modeName.uppercase(),
                        color = AnalyticsColors.primaryBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = Roboto
                    )
                }
            },
            scrollBehavior = scrollBehavior,
            modifier = Modifier.background(Color.White),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White,
                scrolledContainerColor = Color.White,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                titleContentColor = AnalyticsColors.primaryBlue,
                actionIconContentColor = AnalyticsColors.primaryBlue
            )
        )
        Divider(color = AnalyticsColors.softDivider)
    }
}
