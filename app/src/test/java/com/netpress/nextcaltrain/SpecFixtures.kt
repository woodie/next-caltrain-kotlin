package com.netpress.nextcaltrain

/**
 * Factory for building Schedule fixtures for specs.
 * Mirrors iOS SpecFixtures.swift exactly.
 *
 * 16 stations: SF(0), SJD(7), Morgan Hill(14), Gilroy(15)
 * Electric trains run SF <-> SJD only.
 * Diesel/SC trains run SJD <-> Gilroy only.
 */
object SpecFixtures {
    const val sanFrancisco = "San Francisco"
    const val sanJoseDiridon = "San Jose Diridon"
    const val morganHill = "Morgan Hill"
    const val gilroy = "Gilroy"

    const val sanFranciscoIndex = 0
    const val sanJoseDiridonIndex = 7
    const val morganHillIndex = 14
    const val gilroyIndex = 15
    const val stopCount = 16

    const val electricSouthTrainId = 101
    const val electricNorthTrainId = 102
    const val dieselSouthTrainId = 801
    const val dieselNorthTrainId = 802

    val stops: List<String> = buildList {
        val result = Array<String>(stopCount) { "" }
        result[sanFranciscoIndex] = sanFrancisco
        result[sanJoseDiridonIndex] = sanJoseDiridon
        result[morganHillIndex] = morganHill
        result[gilroyIndex] = gilroy
        for (i in result.indices) {
            if (result[i].isEmpty()) result[i] = "Stop $i"
        }
        addAll(result)
    }

    val northStops: List<String> = stops.reversed()

    enum class Service { NORMAL, NONE }

    fun weekdayOnlySchedule(): Schedule = schedule {
        weekday(electric = Service.NORMAL, diesel = Service.NORMAL)
    }

    fun schedule(configure: Builder.() -> Unit): Schedule {
        val builder = Builder()
        builder.configure()
        return builder.build()
    }

    class Builder {
        private val south = mutableMapOf<ScheduleType, MutableMap<String, List<Int?>>>()
        private val north = mutableMapOf<ScheduleType, MutableMap<String, List<Int?>>>()

        fun weekday(electric: Service = Service.NONE, diesel: Service = Service.NONE) =
            set(ScheduleType.WEEKDAY, electric, diesel)

        fun weekend(electric: Service = Service.NONE, diesel: Service = Service.NONE) =
            set(ScheduleType.WEEKEND, electric, diesel)

        fun holiday(electric: Service = Service.NONE, diesel: Service = Service.NONE) =
            set(ScheduleType.HOLIDAY, electric, diesel)

        private fun set(type: ScheduleType, electric: Service, diesel: Service) {
            val southTable = south.getOrPut(type) { mutableMapOf() }
            val northTable = north.getOrPut(type) { mutableMapOf() }

            if (electric == Service.NORMAL) {
                // Electric southbound: SF(480) -> SJD(510)
                southTable[electricSouthTrainId.toString()] = southRow(
                    sanFranciscoIndex to 480,
                    sanJoseDiridonIndex to 510,
                )
                // Electric northbound: SJD(520) -> SF(550)
                northTable[electricNorthTrainId.toString()] = northRow(
                    sanJoseDiridonIndex to 520,
                    sanFranciscoIndex to 550,
                )
            }

            if (diesel == Service.NORMAL) {
                // Diesel southbound: SJD(515) -> Morgan Hill(535) -> Gilroy(545)
                southTable[dieselSouthTrainId.toString()] = southRow(
                    sanJoseDiridonIndex to 515,
                    morganHillIndex to 535,
                    gilroyIndex to 545,
                )
                // Diesel northbound: Gilroy(420) -> Morgan Hill(430) -> SJD(450)
                northTable[dieselNorthTrainId.toString()] = northRow(
                    gilroyIndex to 420,
                    morganHillIndex to 430,
                    sanJoseDiridonIndex to 450,
                )
            }
        }

        private fun southRow(vararg values: Pair<Int, Int>): List<Int?> {
            val row = arrayOfNulls<Int>(stopCount)
            for ((index, time) in values) row[index] = time
            return row.toList()
        }

        private fun northRow(vararg values: Pair<Int, Int>): List<Int?> {
            val row = arrayOfNulls<Int>(stopCount)
            for ((southIndex, time) in values) row[stopCount - 1 - southIndex] = time
            return row.toList()
        }

        fun build(): Schedule = Schedule(
            specialDates = emptyMap(),
            northStops = northStops,
            southStops = stops,
            northWeekday = north[ScheduleType.WEEKDAY] ?: emptyMap(),
            northWeekend = north[ScheduleType.WEEKEND] ?: emptyMap(),
            northHoliday = north[ScheduleType.HOLIDAY] ?: emptyMap(),
            southWeekday = south[ScheduleType.WEEKDAY] ?: emptyMap(),
            southWeekend = south[ScheduleType.WEEKEND] ?: emptyMap(),
            southHoliday = south[ScheduleType.HOLIDAY] ?: emptyMap(),
            scheduleDate = null,
        )
    }
}
