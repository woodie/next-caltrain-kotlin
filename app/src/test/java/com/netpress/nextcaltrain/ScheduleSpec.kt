package com.netpress.nextcaltrain

import android.content.Context
import android.content.SharedPreferences
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import java.util.Calendar

// Covers Schedule.fetchedToday() and .fetchFromNetwork(); the 2am-boundary math the former
// delegates to is covered in GoodTimesSpec. fetchFromNetwork's error-path coverage matches
// huck's ScanClientSpec.kt: a FakeScheduleHttpClient stands in for a real network call, and each
// it calls the suspend function directly inside its own runTest -- not hoisted into
// justBeforeEach, since a throwing act belongs inside the shouldThrow assertion itself, not
// setup that runs before it.
class ScheduleSpec : DescribeSpec({

    val validScheduleJson =
        """{"northStops":["A","B"],"southStops":["A","B"],""" +
            """"northWeekday":{},"northWeekend":{},"northHoliday":{},""" +
            """"southWeekday":{},"southWeekend":{},"southHoliday":{}}"""

    // northStops is empty -- fails Schedule.isValid without needing a malformed-JSON case too.
    val invalidScheduleJson =
        """{"northStops":[],"southStops":["A","B"],""" +
            """"northWeekday":{},"northWeekend":{},"northHoliday":{},""" +
            """"southWeekday":{},"southWeekend":{},"southHoliday":{}}"""

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

    // Fixed noon, well clear of the 2am schedule-day boundary, so results don't depend on real run time.
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

    describe("Schedule.fetchFromNetwork()") {
        lateinit var cacheDir: File
        lateinit var context: Context

        beforeEach {
            cacheDir = Files.createTempDirectory("schedule-tests-").toFile()
            context = mockk<Context>(relaxed = true)
            every { context.filesDir } returns cacheDir
        }
        afterEach { cacheDir.deleteRecursively() }

        context("when the server responds with 200 and valid schedule data") {
            it("parses and returns the schedule, caching it for next launch") {
                runTest {
                    val fakeHttp = FakeScheduleHttpClient(getHandler = { ScheduleHttpResult(200, validScheduleJson) })

                    val schedule = Schedule.fetchFromNetwork(context, fakeHttp)

                    schedule.northStops shouldBe listOf("A", "B")
                    File(cacheDir, Schedule.CACHE_FILE).readText() shouldBe validScheduleJson
                }
            }
        }

        context("when the server responds with a non-2xx status") {
            it("throws ScheduleError with that status code, without caching anything") {
                runTest {
                    val fakeHttp = FakeScheduleHttpClient(getHandler = { ScheduleHttpResult(500, "") })

                    val error = shouldThrow<ScheduleError> { Schedule.fetchFromNetwork(context, fakeHttp) }

                    error.message shouldBe "The server responded with status 500."
                    File(cacheDir, Schedule.CACHE_FILE).exists().shouldBeFalse()
                }
            }
        }

        context("when the server responds with 200 but invalid schedule data") {
            it("throws ScheduleError instead of returning or caching it") {
                runTest {
                    val fakeHttp = FakeScheduleHttpClient(getHandler = { ScheduleHttpResult(200, invalidScheduleJson) })

                    val error = shouldThrow<ScheduleError> { Schedule.fetchFromNetwork(context, fakeHttp) }

                    error.message shouldBe "The server sent back schedule data that didn't validate."
                    File(cacheDir, Schedule.CACHE_FILE).exists().shouldBeFalse()
                }
            }
        }
    }
})
