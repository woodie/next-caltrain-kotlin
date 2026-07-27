package com.netpress.nextcaltrain

import android.content.Context
import com.netpress.kwick.justBeforeEach
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class TripViewModelSpec : DescribeSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeSpec { Dispatchers.setMain(testDispatcher) }
    afterSpec { Dispatchers.resetMain() }

    afterEach {
        GoodTimes.debugOverrideMinutes = null
        GoodTimes.debugOverrideDotw = null
    }

    // Relaxed mock: TripViewModel reads stop prefs in init{}, but every test overwrites origin/destination anyway.
    fun makeViewModel(schedule: Schedule, origin: String, destination: String): TripViewModel =
        TripViewModel(schedule, mockk<Context>(relaxed = true)).apply {
            setOrigin(origin)
            setDestination(destination)
            refresh()
        }

    describe("TripViewModel") {
        var dotw = 0
        var mins = 0
        justBeforeEach {
            GoodTimes.debugOverrideDotw = dotw
            GoodTimes.debugOverrideMinutes = mins
        }

        context("for a route with no service tomorrow") {
            // Weekday-only schedule. Friday -> Saturday = weekend with no SC service.
            lateinit var viewModel: TripViewModel
            beforeEach { dotw = 5 } // Friday
            justBeforeEach {
                viewModel = makeViewModel(
                    SpecFixtures.weekdayOnlySchedule(),
                    SpecFixtures.sanFrancisco,
                    SpecFixtures.gilroy,
                )
            }

            context("and all of today's trips have already departed") {
                beforeEach { mins = 1000 }

                it("still has today's trips available") { viewModel.trips.value.shouldNotBeEmpty() }
                it("has no future (tomorrow) trips appended") {
                    viewModel.trips.value.any { it.isFuture }.shouldBeFalse()
                }
                it("selects the first trip of the day, not the last") {
                    viewModel.offset.value shouldBe 0
                }
                it("keeps the selection at 0 on subsequent ticks") {
                    viewModel.updateNextIndex()
                    viewModel.offset.value shouldBe 0
                }
            }

            context("and some of today's trips are still upcoming") {
                beforeEach { mins = 100 }

                it("selects the next upcoming trip") {
                    viewModel.offset.value shouldBe viewModel.nextIndex.value
                    viewModel.offset.value shouldBe 0
                }
            }
        }

        context("for a route with service every day") {
            // Monday -> Tuesday, both weekday.

            lateinit var viewModel: TripViewModel
            beforeEach { dotw = 1; mins = 0 } // Monday
            justBeforeEach {
                val schedule = SpecFixtures.schedule {
                    weekday(electric = SpecFixtures.Service.NORMAL, diesel = SpecFixtures.Service.NORMAL)
                    weekend(electric = SpecFixtures.Service.NORMAL, diesel = SpecFixtures.Service.NORMAL)
                }
                viewModel = makeViewModel(schedule, SpecFixtures.sanFrancisco, SpecFixtures.sanJoseDiridon)
            }

            context("and all of today's trips have already departed") {
                beforeEach { mins = 1000 }

                it("appends tomorrow's trips, marked as future") {
                    viewModel.trips.value.any { it.isFuture }.shouldBeTrue()
                }
                it("rolls the selection into tomorrow's first trip") {
                    viewModel.trips.value[viewModel.offset.value].isFuture.shouldBeTrue()
                }
                it("shifts tomorrow's depart time by a full day") {
                    val futureTrip = viewModel.trips.value[viewModel.offset.value]
                    futureTrip.depart shouldBe 480 + TripViewModel.dayMinutes
                }
            }

            context("and some of today's trips are still upcoming") {
                beforeEach { mins = 100 }

                it("selects today's trip, not a future one") {
                    viewModel.trips.value[viewModel.offset.value].isFuture.shouldBeFalse()
                }
            }
        }

        context("for a route with no service on any day") {
            lateinit var viewModel: TripViewModel
            beforeEach {
                GoodTimes.debugOverrideDotw = 1
                GoodTimes.debugOverrideMinutes = 100
                viewModel = makeViewModel(
                    SpecFixtures.schedule { },
                    SpecFixtures.sanFrancisco,
                    SpecFixtures.gilroy,
                )
            }
            it("has no trips") { viewModel.trips.value.shouldBeEmpty() }
            it("selects offset 0 without crashing") { viewModel.offset.value shouldBe 0 }
            it("remains at offset 0 after a timer tick") {
                viewModel.updateNextIndex()
                viewModel.offset.value shouldBe 0
            }
        }

        context("for a future trip's schedule type (Friday -> Saturday)") {
            lateinit var viewModel: TripViewModel
            beforeEach {
                GoodTimes.debugOverrideDotw = 5 // Friday
                GoodTimes.debugOverrideMinutes = 1000
                val schedule = SpecFixtures.schedule {
                    weekday(electric = SpecFixtures.Service.NORMAL, diesel = SpecFixtures.Service.NORMAL)
                    weekend(electric = SpecFixtures.Service.NORMAL, diesel = SpecFixtures.Service.NORMAL)
                }
                viewModel = makeViewModel(schedule, SpecFixtures.sanFrancisco, SpecFixtures.sanJoseDiridon)
            }
            it("has a future trip selected") {
                viewModel.trips.value[viewModel.offset.value].isFuture.shouldBeTrue()
            }
            it("isFutureSelected is true") { viewModel.isFutureSelected.shouldBeTrue() }
            it("today is weekday, tomorrow is weekend") {
                viewModel.scheduleType.value shouldBe ScheduleType.WEEKDAY
                viewModel.tomorrowScheduleType shouldBe ScheduleType.WEEKEND
            }
            it("correct schedule type for detail view is tomorrowScheduleType") {
                val trip = viewModel.trips.value[viewModel.offset.value]
                val detailType = if (trip.isFuture) viewModel.tomorrowScheduleType else viewModel.scheduleType.value
                detailType shouldBe ScheduleType.WEEKEND
            }
        }

        context("manual selection via setOffset") {
            // Regression coverage for the reset-button-stuck-on bug; see docs/COMMENTS.md.
            lateinit var viewModel: TripViewModel
            beforeEach {
                GoodTimes.debugOverrideDotw = 1 // Monday
                GoodTimes.debugOverrideMinutes = 100
                val schedule = SpecFixtures.schedule {
                    weekday(electric = SpecFixtures.Service.NORMAL, diesel = SpecFixtures.Service.NORMAL)
                    weekend(electric = SpecFixtures.Service.NORMAL, diesel = SpecFixtures.Service.NORMAL)
                }
                viewModel = makeViewModel(schedule, SpecFixtures.sanFrancisco, SpecFixtures.sanJoseDiridon)
            }

            it("flags manual selection when dragging to an offset other than nextIndex") {
                viewModel.setOffset(viewModel.nextIndex.value + 1)
                viewModel.hasManualSelection.shouldBeTrue()
            }

            it("clears manual selection when dragging back to the next train") {
                viewModel.setOffset(viewModel.nextIndex.value + 1)
                viewModel.hasManualSelection.shouldBeTrue()

                viewModel.setOffset(viewModel.nextIndex.value)
                viewModel.hasManualSelection.shouldBeFalse()
            }

            it("resetToNext() also clears manual selection") {
                viewModel.setOffset(viewModel.nextIndex.value + 1)
                viewModel.resetToNext()
                viewModel.hasManualSelection.shouldBeFalse()
            }
        }
    }
})
