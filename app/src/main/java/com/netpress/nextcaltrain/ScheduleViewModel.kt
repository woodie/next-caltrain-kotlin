package com.netpress.nextcaltrain

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Schedule loading state machine:
 * - Already fetched today (2am boundary) and cache exists: use the cache, no
 *   network call at all.
 * - No cache: block on a network fetch. Success -> Ready. Failure -> Error
 *   (retried the next time the app/process is opened).
 * - Cache exists but not fetched today: race a fetch against a 10s timeout.
 *   Winner -> Ready. Loser/failure -> fall back to the stale cache (retried
 *   the next time the app/process is opened, since fetchedToday() stays false
 *   until a fetch actually succeeds).
 */
sealed class LoadState {
    object Loading : LoadState()
    object Error : LoadState()
    data class Ready(val schedule: Schedule) : LoadState()
}

/**
 * Hosted in a ViewModel (rather than Compose `remember`) so this survives
 * Activity recreation on rotation. Without this, rotating the screen would
 * tear down the whole load sequence and re-flash "Loading schedule data"
 * every time, even though we already had a perfectly good schedule loaded.
 */
class ScheduleViewModel : ViewModel() {
    private val _loadState = MutableStateFlow<LoadState>(LoadState.Loading)
    val loadState: StateFlow<LoadState> = _loadState.asStateFlow()

    private var started = false

    /** Safe to call on every recomposition (e.g. after rotation re-creates the
     * Activity) — the load sequence only actually runs once per ViewModel instance. */
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
