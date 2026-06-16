package com.netpress.nextcaltrain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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

@Composable
fun NextCaltrainApp() {
    val context = LocalContext.current
    val scheduleVm: ScheduleViewModel = viewModel()

    // ensureLoaded() no-ops after its first call, so this is safe to re-run after
    // rotation recreates the Activity/composition — it won't re-trigger the fetch
    // or flash the loading screen again. Pass applicationContext so the coroutine
    // (which can run for up to 10s) never holds a reference to a destroyed Activity.
    LaunchedEffect(Unit) { scheduleVm.ensureLoaded(context.applicationContext) }
    val loadState by scheduleVm.loadState.collectAsStateWithLifecycle()

    when (val state = loadState) {
        is LoadState.Loading -> AboutScreen(scheduleDate = null, isLoading = true, loadFailed = false, onBack = null)
        is LoadState.Error   -> AboutScreen(scheduleDate = null, isLoading = true, loadFailed = true, onBack = null)
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
