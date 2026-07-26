package com.netpress.nextcaltrain

import com.netpress.kwick.justBeforeEach
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CaltrainScheduleSpec : DescribeSpec({

    afterEach {
        GoodTimes.debugOverrideMinutes = null
        GoodTimes.debugOverrideDotw = null
    }

    describe("CaltrainSchedule") {

        describe(".optionIndexFor()") {
            lateinit var result: ScheduleType
            lateinit var date: String
            var dotw = 0
            lateinit var specialDates: Map<String, Int>
            justBeforeEach { result = CaltrainSchedule.optionIndexFor(date, dotw, specialDates) }

            context("with no special dates") {
                beforeEach { specialDates = emptyMap() }

                context("on a weekday (Wednesday, dotw=3)") {
                    beforeEach { date = "2026-06-17"; dotw = 3 }
                    it("returns WEEKDAY") { result shouldBe ScheduleType.WEEKDAY }
                }
                context("on Sunday (dotw=0)") {
                    beforeEach { date = "2026-06-14"; dotw = 0 }
                    it("returns WEEKEND") { result shouldBe ScheduleType.WEEKEND }
                }
                context("on Saturday (dotw=6)") {
                    beforeEach { date = "2026-06-13"; dotw = 6 }
                    it("returns WEEKEND") { result shouldBe ScheduleType.WEEKEND }
                }
            }

            context("with a special date matching today") {
                beforeEach {
                    specialDates = mapOf(
                        "2026-07-04" to 1, // Weekend override
                        "2026-12-25" to 2, // Holiday
                    )
                }

                context("when the special date maps to WEEKEND") {
                    beforeEach { date = "2026-07-04"; dotw = 3 }
                    it("overrides a weekday dotw") { result shouldBe ScheduleType.WEEKEND }
                }
                context("when the special date maps to HOLIDAY") {
                    beforeEach { date = "2026-12-25"; dotw = 5 }
                    it("returns HOLIDAY regardless of dotw") { result shouldBe ScheduleType.HOLIDAY }
                }
                context("on a date not in specialDates") {
                    beforeEach { date = "2026-06-17"; dotw = 3 }
                    it("falls back to dotw-based logic") { result shouldBe ScheduleType.WEEKDAY }
                }
            }

            context("with an invalid special date value") {
                beforeEach {
                    date = "2026-06-17"
                    dotw = 3
                    specialDates = mapOf("2026-06-17" to 99)
                }
                it("falls back to WEEKDAY") { result shouldBe ScheduleType.WEEKDAY }
            }
        }

        describe(".forTomorrow()") {
            lateinit var result: ScheduleType
            justBeforeEach { result = CaltrainSchedule.forTomorrow(GoodTimes(), emptyMap()) }

            context("when today is Friday (5)") {
                beforeEach { GoodTimes.debugOverrideDotw = 5 }
                it("returns WEEKEND for tomorrow (Saturday)") { result shouldBe ScheduleType.WEEKEND }
            }
            context("when today is Sunday (0)") {
                beforeEach { GoodTimes.debugOverrideDotw = 0 }
                it("returns WEEKDAY for tomorrow (Monday)") { result shouldBe ScheduleType.WEEKDAY }
            }
            context("when today is Thursday (4)") {
                beforeEach { GoodTimes.debugOverrideDotw = 4 }
                it("returns WEEKDAY for tomorrow (Friday)") { result shouldBe ScheduleType.WEEKDAY }
            }
        }
    }
})
