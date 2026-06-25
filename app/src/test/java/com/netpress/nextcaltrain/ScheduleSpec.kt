package com.netpress.nextcaltrain

import android.content.Context
import android.content.SharedPreferences
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.every
import io.mockk.mockk
import java.util.Calendar

/**
 * Covers Schedule.fetchedToday() -- the once-per-day fetch cap that lets
 * MainActivity/ScheduleViewModel skip a redundant network call once we
 * already have today's schedule. fetchedToday() itself only reads from
 * SharedPreferences and delegates to GoodTimes.scheduleDateFor(); the 2am
 * boundary math for scheduleDateFor() is covered separately in GoodTimesSpec.
 */
class ScheduleSpec : DescribeSpec({

    fun contextWithLastFetch(epochMillis: Long?): Context {
        val prefs = mockk<SharedPreferences>()
        every {
            prefs.getLong(Schedule.KEY_LAST_FETCH_MS, -1L)
        } returns (epochMillis ?: -1L)

        val context = mockk<Context>()
        every {
            context.getSharedPreferences(Schedule.PREFS_NAME, Context.MODE_PRIVATE)
        } returns prefs
        return context
    }

    // Fixed at noon on an arbitrary date, well clear of the 2am schedule-day
    // boundary in either direction. Tests pin "now" explicitly via fetchedToday's
    // nowMillis param instead of System.currentTimeMillis(), so results don't
    // depend on what time the suite happens to run.
    fun fixedNoon(): Long {
        val cal = Calendar.getInstance()
        cal.set(2024, Calendar.JANUARY, 15, 12, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    describe("Schedule.fetchedToday") {
        context("when nothing has ever been fetched") {
            val result = Schedule.fetchedToday(contextWithLastFetch(null))

            it("returns false") { result.shouldBeFalse() }
        }

        context("when the last fetch was a few minutes ago") {
            val now = fixedNoon()
            val recent = now - 5 * 60 * 1000L
            val result = Schedule.fetchedToday(contextWithLastFetch(recent), now)

            it("returns true") { result.shouldBeTrue() }
        }

        context("when the last fetch was more than a day ago") {
            val now = fixedNoon()
            val stale = now - 26 * 60 * 60 * 1000L
            val result = Schedule.fetchedToday(contextWithLastFetch(stale), now)

            it("returns false") { result.shouldBeFalse() }
        }
    }
})
