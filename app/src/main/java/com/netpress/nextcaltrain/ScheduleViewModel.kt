package com.netpress.nextcaltrain

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

// Schedule loading state machine; see docs/COMMENTS.md for the three cases this walks through.
sealed class LoadState {
    object Loading : LoadState()
    object Error : LoadState()
    data class Ready(val schedule: Schedule) : LoadState()
}

// Hosted in a ViewModel (not Compose `remember`) so the load sequence survives Activity recreation on rotation.
class ScheduleViewModel : ViewModel() {
    private val _loadState = MutableStateFlow<LoadState>(LoadState.Loading)
    val loadState: StateFlow<LoadState> = _loadState.asStateFlow()

    private var started = false

    // Safe to call on every recomposition — the load sequence only actually runs once per instance.
    fun ensureLoaded(context: Context) {
        if (started) return
        started = true
        viewModelScope.launch {
            val cached = Schedule.loadCached(context)
            _loadState.value = when {
                cached != null && Schedule.fetchedToday(context) -> {
                    LoadState.Ready(cached)
                }

                cached == null -> {
                    try {
                        LoadState.Ready(Schedule.fetchFromNetwork(context))
                    } catch (e: Exception) {
                        LoadState.Error
                    }
                }

                else -> {
                    try {
                        withTimeout(10_000L) {
                            LoadState.Ready(Schedule.fetchFromNetwork(context))
                        }
                    } catch (e: Exception) {
                        LoadState.Ready(cached)
                    }
                }
            }
        }
    }
}
