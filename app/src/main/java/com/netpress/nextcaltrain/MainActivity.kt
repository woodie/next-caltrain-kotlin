package com.netpress.nextcaltrain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
            try {
                val schedule = Schedule.fetchFromNetwork(context)
                LoadState.Ready(schedule)
            } catch (e: Exception) {
                LoadState.Error
            }
        } else {
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
        is LoadState.Ready   -> {
            val vm: TripViewModel = viewModel(
                factory = TripViewModelFactory(state.schedule, context)
            )
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = "home") {
                composable("home") {
                    HomeScreen(
                        vm = vm,
                        onNavigateToTripList = { navController.navigate("tripList") },
                        onNavigateToAbout = { navController.navigate("about") },
                        onNavigateToStationSelection = { navController.navigate("stationSelection") },
                    )
                }
                composable("tripList") {
                    TripListScreen(
                        vm = vm,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToTripDetail = { navController.navigate("tripDetail") },
                        onNavigateToStationSelection = { navController.navigate("stationSelection") },
                    )
                }
                composable("tripDetail") {
                    TripDetailScreen(
                        vm = vm,
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
                composable("stationSelection") {
                    StationSelectionScreen(
                        vm = vm,
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
                composable("about") {
                    AboutScreen(
                        scheduleDate = state.schedule.scheduleDate,
                        isLoading = false,
                        loadFailed = false,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
