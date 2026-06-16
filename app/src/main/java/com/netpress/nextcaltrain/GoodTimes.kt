package com.netpress.nextcaltrain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Central time utility. Subtracts 2 hours from the real clock so that
 * trains running past midnight (e.g. 24:05 = 1445 minutes) still appear
 * on "today's" schedule rather than rolling to tomorrow at midnight.
 */
data class GoodTimes(
    val date: String,
    val minutes: Int,
    val seconds: Int,
    val dotw: Int,
    val tomorrowDate: String,
    val tomorrowDotw: Int,
) {
    companion object {
        // DEBUG OVERRIDES — set to non-null to pin time/day for testing.
        // Format: minutes since midnight, e.g. 330 = 5:30am.
        var debugOverrideMinutes: Int? = null

        // 0 = Sunday ... 6 = Saturday. Set to null for normal behavior.
        var debugOverrideDotw: Int? = null

        private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        operator fun invoke(): GoodTimes {
            val now = Date()
            val run = Date(now.time - 2 * 60 * 60 * 1000L)

            val cal = Calendar.getInstance()
            cal.time = run
            val tomorrow = Calendar.getInstance()
            tomorrow.time = run
            tomorrow.add(Calendar.DAY_OF_YEAR, 1)

            val realDotw = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sunday
            val dotw = debugOverrideDotw ?: realDotw
            val tomorrowDotw = (dotw + 1) % 7

            val date = dateFmt.format(run)
            val tomorrowDate = dateFmt.format(tomorrow.time)

            val minutes = debugOverrideMinutes
                ?: ((cal.get(Calendar.HOUR_OF_DAY) + 2) * 60 + cal.get(Calendar.MINUTE))
            val seconds = if (debugOverrideMinutes != null) 0 else cal.get(Calendar.SECOND)

            return GoodTimes(
                date = date,
                minutes = minutes,
                seconds = seconds,
                dotw = dotw,
                tomorrowDate = tomorrowDate,
                tomorrowDotw = tomorrowDotw,
            )
        }

        /** Returns the yyyy-MM-dd schedule-day string for an arbitrary instant, using
         * the same "day starts at 2am" rule as invoke(): subtract 2 hours before formatting.
         * Lets us compare "today" against a stored last-fetch timestamp for the
         * once-per-day schedule fetch policy. */
        fun scheduleDateFor(epochMillis: Long): String =
            dateFmt.format(Date(epochMillis - 2 * 60 * 60 * 1000L))

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
            diff < 0  -> ""
            diff > 59 -> "in ${diff / 60} hr ${diff % 60} min"
            else      -> "in $diff min ${60 - seconds} sec"
        }
    }
}
