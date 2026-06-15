package com.netpress.nextcaltrain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TripViewModel(val schedule: Schedule) : ViewModel() {

    companion object {
        const val dayMinutes = 1440
    }

    // MARK: - Published state

    private val _origin = MutableStateFlow("San Francisco")
    val origin: StateFlow<String> = _origin.asStateFlow()

    private val _destination = MutableStateFlow("Palo Alto")
    val destination: StateFlow<String> = _destination.asStateFlow()

    private val _scheduleType = MutableStateFlow(ScheduleType.WEEKDAY)
    val scheduleType: StateFlow<ScheduleType> = _scheduleType.asStateFlow()

    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips: StateFlow<List<Trip>> = _trips.asStateFlow()

    private val _nextIndex = MutableStateFlow(0)
    val nextIndex: StateFlow<Int> = _nextIndex.asStateFlow()

    private val _offset = MutableStateFlow(0)
    val offset: StateFlow<Int> = _offset.asStateFlow()

    private val _goodTimes = MutableStateFlow(GoodTimes())
    val goodTimes: StateFlow<GoodTimes> = _goodTimes.asStateFlow()

    private var userSelected = false
    val hasManualSelection: Boolean get() = userSelected

    private val service = CaltrainService(schedule)
    private var timerJob: Job? = null

    init {
        val gt = GoodTimes()
        _goodTimes.value = gt
        _scheduleType.value = CaltrainSchedule.forToday(gt, schedule.specialDates)
        refresh()
        startTimer()
    }

    // MARK: - Computed

    val swapped: Boolean
        get() {
            val today = CaltrainSchedule.forToday(_goodTimes.value, schedule.specialDates)
            return _scheduleType.value != today
        }

    val tomorrowScheduleType: ScheduleType
        get() = CaltrainSchedule.forTomorrow(_goodTimes.value, schedule.specialDates)

    val isFutureSelected: Boolean
        get() {
            val o = _offset.value
            return if (o < _trips.value.size) _trips.value[o].isFuture else false
        }

    val countdown: String?
        get() {
            val o = _offset.value
            if (o >= _trips.value.size) return null
            val c = _goodTimes.value.countdown(_trips.value[o].depart)
            return c.ifEmpty { null }
        }

    val isDeparting: Boolean
        get() {
            val o = _offset.value
            return if (o < _trips.value.size) _goodTimes.value.departing(_trips.value[o].depart) else false
        }

    val orderedStations: List<String>
        get() {
            val dir = CaltrainService.direction(_origin.value, _destination.value, schedule.southStops)
            return if (dir == "South") schedule.southStops else schedule.southStops.reversed()
        }

    // MARK: - Actions

    fun setOrigin(value: String) {
        _origin.value = value
    }

    fun setDestination(value: String) {
        _destination.value = value
    }

    fun refresh() {
        val todayTrips = service.routes(_origin.value, _destination.value, _scheduleType.value)
        val tomorrowTrips = service.routes(_origin.value, _destination.value, tomorrowScheduleType)
            .map { shiftedToTomorrow(it) }
        _trips.value = todayTrips + tomorrowTrips
        val ni = service.nextIndex(_trips.value, _goodTimes.value.minutes)
        _nextIndex.value = ni
        userSelected = false
        _offset.value = clampedOffset(ni)
    }

    fun setOffset(newOffset: Int) {
        userSelected = true
        _offset.value = newOffset
    }

    fun offsetUp() {
        val o = _offset.value
        if (o > 0) _offset.value = o - 1
    }

    fun offsetDown() {
        val o = _offset.value
        if (o < _trips.value.size - 1) _offset.value = o + 1
    }

    fun resetToNext() {
        userSelected = false
        _offset.value = clampedOffset(_nextIndex.value)
    }

    fun swapStations() {
        val tmp = _origin.value
        _origin.value = _destination.value
        _destination.value = tmp
        refresh()
    }

    fun cycleSchedule() {
        val next = (_scheduleType.value.ordinal + 1) % 3
        _scheduleType.value = ScheduleType.entries[next]
        refresh()
    }

    fun updateNextIndex() {
        val gt = GoodTimes()
        _goodTimes.value = gt
        val ni = service.nextIndex(_trips.value, gt.minutes)
        _nextIndex.value = ni
        if (!userSelected) {
            _offset.value = clampedOffset(ni)
        } else if (_offset.value >= _trips.value.size) {
            _offset.value = clampedOffset(_offset.value)
        }
    }

    // MARK: - Private

    private fun shiftedToTomorrow(trip: Trip): Trip {
        val shiftedLegs = trip.legs.map { leg ->
            Leg(trainId = leg.trainId, station = leg.station, depart = leg.depart + dayMinutes)
        }
        return Trip(
            id = trip.id,
            legs = shiftedLegs,
            arrive = trip.arrive + dayMinutes,
            isFuture = true,
        )
    }

    private fun clampedOffset(desired: Int): Int {
        if (desired < _trips.value.size) return desired
        val hasTomorrow = _trips.value.any { it.isFuture }
        if (!hasTomorrow && _trips.value.isNotEmpty()) return 0
        return maxOf(0, _trips.value.size - 1)
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                updateNextIndex()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
