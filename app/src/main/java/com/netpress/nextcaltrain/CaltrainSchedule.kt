package com.netpress.nextcaltrain

enum class ScheduleType(val label: String) {
    WEEKDAY("Weekday"),
    WEEKEND("Weekend"),
    HOLIDAY("Holiday"),
    ;

    companion object {
        fun fromInt(value: Int): ScheduleType = when (value) {
            1 -> WEEKEND
            2 -> HOLIDAY
            else -> WEEKDAY
        }
    }
}

object CaltrainSchedule {
    // Factored out (rather than inlined into forToday) so tomorrow's schedule type can be computed independently.
    fun optionIndexFor(date: String, dotw: Int, specialDates: Map<String, Int>): ScheduleType {
        specialDates[date]?.let { return ScheduleType.fromInt(it) }
        return if (dotw == 0 || dotw == 6) ScheduleType.WEEKEND else ScheduleType.WEEKDAY
    }

    fun forToday(goodTimes: GoodTimes, specialDates: Map<String, Int>): ScheduleType =
        optionIndexFor(goodTimes.date, goodTimes.dotw, specialDates)

    fun forTomorrow(goodTimes: GoodTimes, specialDates: Map<String, Int>): ScheduleType =
        optionIndexFor(goodTimes.tomorrowDate, goodTimes.tomorrowDotw, specialDates)
}
