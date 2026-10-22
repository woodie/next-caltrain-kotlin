package com.netpress.nextcaltrain

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

// Like Google Maps' "Leave at" / "Arrive by".
enum class PlanMode(val label: String) {
    LEAVE("Leave at"),
    ARRIVE("Arrive by"),
}

data class PlannedTrain(
    val label: String,
    val depart: Int, // minutes since midnight
    val arrive: Int,
    val selected: Boolean,
)

// The calendar planner's logic. Mirrors iOS CalendarTrip.swift.
object CalendarTrip {
    // Caltrain runs on Pacific time, whatever the phone's own time zone is.
    val zone: ZoneId = ZoneId.of("America/Los_Angeles")

    // Index of the planned train in trips, or null if none fits.
    fun find(trips: List<Trip>, mode: PlanMode, minutes: Int): Int? {
        val index = when (mode) {
            PlanMode.LEAVE -> trips.indexOfFirst { it.depart >= minutes }
            PlanMode.ARRIVE -> trips.indexOfLast { it.arrive <= minutes }
        }
        return index.takeIf { it >= 0 }
    }

    // The planned train with the trains before and after it.
    fun neighbors(trips: List<Trip>, index: Int): List<PlannedTrain> =
        (index - 1..index + 1).filter { it in trips.indices }.map { i ->
            val trip = trips[i]
            PlannedTrain(
                label = trip.legs.joinToString("+") { "#${it.trainId}" },
                depart = trip.depart,
                arrive = trip.arrive,
                selected = i == index,
            )
        }

    fun line(train: PlannedTrain): String =
        "${train.label}  ${GoodTimes.fullTime(train.depart)} – ${GoodTimes.fullTime(train.arrive)}"

    fun title(destination: String): String = "Caltrain to $destination"

    // The boarding station, as Maps names it ("Palo Alto Caltrain Station").
    fun location(station: String): String {
        val name = if (station == "So San Francisco") "South San Francisco" else station
        return "$name Caltrain Station"
    }

    // The route, then the trains.
    fun notes(origin: String, destination: String, trains: List<PlannedTrain>): String =
        (listOf("$origin to $destination") + trains.map(::line)).joinToString("\n")

    // Minutes past midnight on an ISO date (YYYY-MM-DD), on Pacific time.
    fun date(isoDate: String, minutes: Int): ZonedDateTime = LocalDate.parse(isoDate)
        .plusDays((minutes / 1440).toLong())
        .atTime(minutes % 1440 / 60, minutes % 60)
        .atZone(zone)

    // Day of the week, 0 for Sunday, the way the schedule counts it.
    fun dotw(isoDate: String): Int = LocalDate.parse(isoDate).dayOfWeek.value % 7
}
