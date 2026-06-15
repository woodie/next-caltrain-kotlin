package com.netpress.nextcaltrain

import androidx.compose.runtime.Composable

@Composable
fun LoadingScreen(message: String) {
    val loadFailed = message.contains("Unable")
    AboutScreen(
        scheduleDate = null,
        isLoading = true,
        loadFailed = loadFailed,
        onBack = null,
    )
}
