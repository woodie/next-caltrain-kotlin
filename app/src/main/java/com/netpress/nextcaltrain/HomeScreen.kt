package com.netpress.nextcaltrain

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.netpress.nextcaltrain.ui.theme.AppStyle
import com.netpress.nextcaltrain.ui.theme.LocalAppColors

// TODO: Full HomeScreen implementation
@Composable
fun HomeScreen(schedule: Schedule) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Next Caltrain",
            fontSize = AppStyle.fontBlurb,
            color = colors.appText,
        )
    }
}
