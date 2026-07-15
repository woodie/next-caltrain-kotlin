package com.netpress.nextcaltrain

// One leg of a trip: one train, one boarding station, one depart time.
data class Leg(
    val trainId: Int,
    val station: String,
    val depart: Int, // minutes since midnight
)

// A trip with one or two legs. Two legs = transfer at San Jose Diridon.
data class Trip(
    val id: Int, // lead train ID
    val legs: List<Leg>,
    val arrive: Int, // arrival at final destination (minutes since midnight)
    val isFuture: Boolean = false,
) {
    val depart: Int get() = legs.first().depart
    val isTransfer: Boolean get() = legs.size > 1
}

// Computes train routes, including transfers for South County service. Mirrors iOS CaltrainService.swift.
class CaltrainService(private val schedule: Schedule) {

    companion object {
        val southCountyStations: Set<String> = setOf(
            "Gilroy",
            "San Martin",
            "Morgan Hill",
            "Blossom Hill",
            "Capitol",
        )
        const val transferStation = "San Jose Diridon"

        fun isSouthCounty(trainId: Int): Boolean = trainId in 801..900

        fun trainType(trainId: Int): String = when {
            trainId > 900 -> "Unknown"
            trainId > 800 -> "South County"
            trainId > 500 -> "Express"
            trainId > 400 -> "Limited"
            trainId > 100 -> "Local"
            else -> "Unknown"
        }

        fun direction(from: String, to: String, stops: List<String>): String {
            val fromIdx = stops.indexOf(from)
            val toIdx = stops.indexOf(to)
            return if (fromIdx < toIdx) "South" else "North"
        }
    }

    fun routes(from: String, to: String, scheduleType: ScheduleType): List<Trip> {
        val fromIsSC = southCountyStations.contains(from)
        val toIsSC = southCountyStations.contains(to)
        val needsTransfer = scheduleType == ScheduleType.WEEKDAY && (fromIsSC != toIsSC)
        return if (needsTransfer) transferRoutes(from, to, scheduleType) else directRoutes(from, to, scheduleType)
    }

    fun nextIndex(trips: List<Trip>, minutes: Int): Int =
        trips.indexOfFirst { it.depart >= minutes }.takeIf { it >= 0 } ?: trips.size

    // MARK: - Private

    private fun directRoutes(from: String, to: String, scheduleType: ScheduleType): List<Trip> {
        val dir = direction(from, to, schedule.southStops)
        val source = select(dir, scheduleType)
        val stops = if (dir == "North") schedule.northStops else schedule.southStops

        val fromIdx = stops.indexOf(from).takeIf { it >= 0 } ?: return emptyList()
        val toIdx = stops.indexOf(to).takeIf { it >= 0 } ?: return emptyList()

        val trips = mutableListOf<Trip>()
        for ((trainKey, times) in source) {
            val trainId = trainKey.toIntOrNull() ?: continue
            if (fromIdx >= times.size || toIdx >= times.size) continue
            val departTime = times[fromIdx] ?: continue
            val arriveTime = times[toIdx] ?: continue
            val leg = Leg(trainId, from, departTime)
            trips.add(Trip(id = trainId, legs = listOf(leg), arrive = arriveTime))
        }
        return trips.sortedBy { it.depart }
    }

    private fun transferRoutes(origin: String, destination: String, scheduleType: ScheduleType): List<Trip> {
        val dir = direction(origin, destination, schedule.southStops)
        val source = select(dir, scheduleType)
        val stops = if (dir == "North") schedule.northStops else schedule.southStops

        val originIdx = stops.indexOf(origin).takeIf { it >= 0 } ?: return emptyList()
        val transferIdx = stops.indexOf(transferStation).takeIf { it >= 0 } ?: return emptyList()
        val destIdx = stops.indexOf(destination).takeIf { it >= 0 } ?: return emptyList()

        return if (dir == "North") {
            northboundTransfers(source, stops, origin, originIdx, transferIdx, destIdx)
        } else {
            southboundTransfers(source, stops, origin, originIdx, transferIdx, destIdx, destination)
        }
    }

    private fun northboundTransfers(
        source: Map<String, List<Int?>>,
        stops: List<String>,
        origin: String,
        originIdx: Int,
        transferIdx: Int,
        destIdx: Int,
    ): List<Trip> {
        // SC trains: origin -> SJD
        val scTrains = mutableListOf<Triple<Int, Int, Int>>() // trainId, departOrigin, arriveTransfer
        for ((trainKey, times) in source) {
            val trainId = trainKey.toIntOrNull() ?: continue
            if (!isSouthCounty(trainId)) continue
            if (originIdx >= times.size || transferIdx >= times.size) continue
            val depart = times[originIdx] ?: continue
            val arrive = times[transferIdx] ?: continue
            scTrains.add(Triple(trainId, depart, arrive))
        }
        scTrains.sortBy { it.second }

        // Electric trains: SJD -> destination
        val elTrains = mutableListOf<Triple<Int, Int, Int>>() // trainId, departTransfer, arriveDestination
        for ((trainKey, times) in source) {
            val trainId = trainKey.toIntOrNull() ?: continue
            if (isSouthCounty(trainId)) continue
            if (transferIdx >= times.size || destIdx >= times.size) continue
            val depart = times[transferIdx] ?: continue
            val arrive = times[destIdx] ?: continue
            elTrains.add(Triple(trainId, depart, arrive))
        }
        elTrains.sortBy { it.second }

        // Pair each SC train with the first electric departing SJD >= SC arrival
        val trips = mutableListOf<Trip>()
        for ((scId, scDepart, scArrive) in scTrains) {
            val el = elTrains.firstOrNull { it.second >= scArrive } ?: continue
            val leg1 = Leg(scId, origin, scDepart)
            val leg2 = Leg(el.first, transferStation, el.second)
            trips.add(Trip(id = scId, legs = listOf(leg1, leg2), arrive = el.third))
        }
        return trips
    }

    private fun southboundTransfers(
        source: Map<String, List<Int?>>,
        stops: List<String>,
        origin: String,
        originIdx: Int,
        transferIdx: Int,
        destIdx: Int,
        destination: String,
    ): List<Trip> {
        // Electric trains: origin -> SJD
        val elTrains = mutableListOf<Triple<Int, Int, Int>>() // trainId, departOrigin, arriveTransfer
        for ((trainKey, times) in source) {
            val trainId = trainKey.toIntOrNull() ?: continue
            if (isSouthCounty(trainId)) continue
            if (originIdx >= times.size || transferIdx >= times.size) continue
            val depart = times[originIdx] ?: continue
            val arrive = times[transferIdx] ?: continue
            elTrains.add(Triple(trainId, depart, arrive))
        }
        elTrains.sortBy { it.second }

        // SC trains: SJD -> destination
        val scTrains = mutableListOf<Triple<Int, Int, Int>>() // trainId, departTransfer, arriveDestination
        for ((trainKey, times) in source) {
            val trainId = trainKey.toIntOrNull() ?: continue
            if (!isSouthCounty(trainId)) continue
            if (transferIdx >= times.size || destIdx >= times.size) continue
            val depart = times[transferIdx] ?: continue
            val arrive = times[destIdx] ?: continue
            scTrains.add(Triple(trainId, depart, arrive))
        }
        scTrains.sortBy { it.second }

        // Pair each SC train with the last electric arriving SJD <= SC departure
        val trips = mutableListOf<Trip>()
        for ((scId, scDepart, scArrive) in scTrains) {
            val el = elTrains.lastOrNull { it.third <= scDepart } ?: continue
            val leg1 = Leg(el.first, origin, el.second)
            val leg2 = Leg(scId, transferStation, scDepart)
            trips.add(Trip(id = el.first, legs = listOf(leg1, leg2), arrive = scArrive))
        }
        return trips.sortedBy { it.depart }
    }

    private fun select(direction: String, scheduleType: ScheduleType): Map<String, List<Int?>> = when (scheduleType) {
        ScheduleType.WEEKDAY -> if (direction == "North") schedule.northWeekday else schedule.southWeekday
        ScheduleType.WEEKEND -> if (direction == "North") schedule.northWeekend else schedule.southWeekend
        ScheduleType.HOLIDAY -> if (direction == "North") schedule.northHoliday else schedule.southHoliday
    }
}
