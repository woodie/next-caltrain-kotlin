package com.netpress.nextcaltrain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Central time utility: subtracts 2 hours so trains past midnight stay on "today's" schedule.
data class GoodTimes(
    val date: String,
    val minutes: Int,
    val seconds: Int,
    val dotw: Int,
    val tomorrowDate: String,
    val tomorrowDotw: Int,
) {
    companion object {
        // Ambient fallback for invoke() (GoodTimes()) -- exists only for callers that
        // construct GoodTimes() internally and can't take a seed as a parameter (e.g.
        // TripViewModel). Anything that constructs GoodTimes itself should call
        // seeded(dotw, mins) directly instead and leave these alone.
        var dotwSeed: Int? = null
        var minutesSeed: Int? = null

        private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        operator fun invoke(): GoodTimes = seeded(dotwSeed, minutesSeed)

        // dotw (0=Sunday...6=Saturday) and/or mins (minutes since midnight) pin the fields
        // that would otherwise come from the real clock -- resolved directly from the
        // arguments, so there's nothing global to reset afterward.
        fun seeded(dotw: Int? = null, mins: Int? = null): GoodTimes {
            val now = Date()
            val run = Date(now.time - 2 * 60 * 60 * 1000L)

            val cal = Calendar.getInstance()
            cal.time = run
            val tomorrow = Calendar.getInstance()
            tomorrow.time = run
            tomorrow.add(Calendar.DAY_OF_YEAR, 1)

            val realDotw = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sunday
            val resolvedDotw = dotw ?: realDotw
            val tomorrowDotw = (resolvedDotw + 1) % 7

            val date = dateFmt.format(run)
            val tomorrowDate = dateFmt.format(tomorrow.time)

            val resolvedMinutes = mins
                ?: ((cal.get(Calendar.HOUR_OF_DAY) + 2) * 60 + cal.get(Calendar.MINUTE))
            val seconds = if (mins != null) 0 else cal.get(Calendar.SECOND)

            return GoodTimes(
                date = date,
                minutes = resolvedMinutes,
                seconds = seconds,
                dotw = resolvedDotw,
                tomorrowDate = tomorrowDate,
                tomorrowDotw = tomorrowDotw,
            )
        }

        // Schedule-day string for an instant, using the same 2am-boundary rule as invoke().
        fun scheduleDateFor(epochMillis: Long): String = dateFmt.format(Date(epochMillis - 2 * 60 * 60 * 1000L))

        fun partTime(minutes: Int): Pair<String, String> {
            var hrs = (minutes / 60) % 24
            val min = minutes % 60
            val mer = if (hrs > 11 && hrs < 24) "pm" else "am"
            if (hrs > 12) hrs -= 12
            if (hrs > 12) hrs -= 12
            if (hrs < 1) hrs = 12
            return Pair(String.format("%d:%02d", hrs, min), mer)
        }

        fun fullTime(minutes: Int): String {
            val (t, mer) = partTime(minutes)
            return "$t$mer"
        }
    }

    fun partTime(): Pair<String, String> = Companion.partTime(minutes)
    fun fullTime(): String = Companion.fullTime(minutes)

    fun inThePast(target: Int): Boolean = target - minutes < 0

    fun departing(target: Int): Boolean = target == minutes

    fun countdown(target: Int): String {
        val diff = target - minutes - 1
        return when {
            diff < 0 -> ""
            diff > 59 -> "in ${diff / 60} hr ${diff % 60} min"
            else -> "in $diff min ${60 - seconds} sec"
        }
    }
}
