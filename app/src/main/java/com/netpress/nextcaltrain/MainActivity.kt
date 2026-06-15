package com.netpress.nextcaltrain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.netpress.nextcaltrain.ui.theme.NextCaltrainTheme
import kotlinx.coroutines.*

/**
 * Loading state machine mirroring iOS ContentView.loadSchedule():
 * - No cache: block on network fetch. Success -> HomeScreen. Failure -> error screen.
 * - Cache exists: race fetch against 10s timeout. Winner -> HomeScreen.
 */
sealed class LoadState {
    object Loading : LoadState()
    object Error : LoadState()
    data class Ready(val schedule: Schedule) : LoadState()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NextCaltrainTheme {
                NextCaltrainApp()
            }
        }
    }
}

@Composable
fun NextCaltrainApp() {
    val context = LocalContext.current
    var loadState by remember { mutableStateOf<LoadState>(LoadState.Loading) }

    LaunchedEffect(Unit) {
        val cached = Schedule.loadCached(context)
        loadState = if (cached == null) {
            // No cache — block on network
            try {
                val schedule = Schedule.fetchFromNetwork(context)
                LoadState.Ready(schedule)
            } catch (e: Exception) {
                LoadState.Error
            }
        } else {
            // Cache exists — race network against 10s timeout
            try {
                withTimeout(10_000L) {
                    val schedule = Schedule.fetchFromNetwork(context)
                    LoadState.Ready(schedule)
                }
            } catch (e: Exception) {
                LoadState.Ready(cached)
            }
        }
    }

    when (val state = loadState) {
        is LoadState.Loading -> LoadingScreen(message = "Loading schedule data")
        is LoadState.Error   -> LoadingScreen(message = "Unable to load schedule")
        is LoadState.Ready   -> HomeScreen(schedule = state.schedule)
    }
}
