package com.netpress.nextcaltrain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.netpress.nextcaltrain.ui.theme.NextCaltrainTheme

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

// Ignores taps while a screen change animates, which would stack a second copy of a screen.
private fun NavBackStackEntry.ifResumed(action: () -> Unit): () -> Unit = {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) action()
}

@Composable
fun NextCaltrainApp() {
    val context = LocalContext.current
    val scheduleVm: ScheduleViewModel = viewModel()

    // applicationContext, not context: the fetch coroutine can run up to 10s and must outlive the Activity.
    LaunchedEffect(Unit) { scheduleVm.ensureLoaded(context.applicationContext) }
    val loadState by scheduleVm.loadState.collectAsStateWithLifecycle()

    when (val state = loadState) {
        is LoadState.Loading -> AboutScreen(scheduleDate = null, isLoading = true, loadFailed = false, onBack = null)

        is LoadState.Error -> AboutScreen(scheduleDate = null, isLoading = true, loadFailed = true, onBack = null)

        is LoadState.Ready -> {
            val vm: TripViewModel = viewModel(
                factory = TripViewModelFactory(state.schedule, context),
            )
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = "home") {
                composable("home") { entry ->
                    HomeScreen(
                        vm = vm,
                        onNavigateToTripList = entry.ifResumed { navController.navigate("tripList") },
                        onNavigateToAbout = entry.ifResumed { navController.navigate("about") },
                        onNavigateToStationSelection = entry.ifResumed { navController.navigate("stationSelection") },
                        onNavigateToCalendar = entry.ifResumed { navController.navigate("calendar") },
                    )
                }
                composable("tripList") { entry ->
                    TripListScreen(
                        vm = vm,
                        onNavigateBack = entry.ifResumed { navController.popBackStack() },
                        onNavigateToTripDetail = entry.ifResumed { navController.navigate("tripDetail") },
                        onNavigateToStationSelection = entry.ifResumed { navController.navigate("stationSelection") },
                        onNavigateToCalendar = entry.ifResumed { navController.navigate("calendar") },
                    )
                }
                composable("tripDetail") { entry ->
                    TripDetailScreen(
                        vm = vm,
                        onNavigateBack = entry.ifResumed { navController.popBackStack() },
                    )
                }
                composable("stationSelection") { entry ->
                    StationSelectionScreen(
                        vm = vm,
                        onNavigateBack = entry.ifResumed { navController.popBackStack() },
                    )
                }
                composable("calendar") { entry ->
                    CalendarScreen(
                        vm = vm,
                        onNavigateBack = entry.ifResumed { navController.popBackStack() },
                        onNavigateToStationSelection = entry.ifResumed { navController.navigate("stationSelection") },
                    )
                }
                composable("about") { entry ->
                    AboutScreen(
                        scheduleDate = state.schedule.scheduleDate,
                        isLoading = false,
                        loadFailed = false,
                        onBack = entry.ifResumed { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
