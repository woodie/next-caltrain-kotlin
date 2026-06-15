package com.netpress.nextcaltrain

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netpress.nextcaltrain.ui.theme.AppStyle
import com.netpress.nextcaltrain.ui.theme.LocalAppColors

@Composable
fun LoadingScreen(message: String) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            fontSize = AppStyle.fontBlurb,
            color = colors.calPast,
        )
    }
}
