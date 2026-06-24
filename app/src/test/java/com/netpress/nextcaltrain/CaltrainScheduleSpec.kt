package com.netpress.nextcaltrain

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CaltrainScheduleSpec : DescribeSpec({

    afterEach {
        GoodTimes.debugOverrideMinutes = null
        GoodTimes.debugOverrideDotw = null
    }

    describe("CaltrainSchedule") {

        describe(".optionIndexFor()") {
            context("with no special dates") {
                val specialDates = emptyMap<String, Int>()

                context("on a weekday (Wednesday, dotw=3)") {
                    it("returns WEEKDAY") {
                        val result = CaltrainSchedule.optionIndexFor("2026-06-17", 3, specialDates)
                        result shouldBe ScheduleType.WEEKDAY
                    }
                }
                context("on Sunday (dotw=0)") {
                    it("returns WEEKEND") {
                        val result = CaltrainSchedule.optionIndexFor("2026-06-14", 0, specialDates)
                        result shouldBe ScheduleType.WEEKEND
                    }
                }
                context("on Saturday (dotw=6)") {
                    it("returns WEEKEND") {
                        val result = CaltrainSchedule.optionIndexFor("2026-06-13", 6, specialDates)
                        result shouldBe ScheduleType.WEEKEND
                    }
                }
            }

            context("with a special date matching today") {
                val specialDates = mapOf(
                    "2026-07-04" to 1, // Weekend override
                    "2026-12-25" to 2, // Holiday
                )

                context("when the special date maps to WEEKEND") {
                    it("overrides a weekday dotw") {
                        val result = CaltrainSchedule.optionIndexFor("2026-07-04", 3, specialDates)
                        result shouldBe ScheduleType.WEEKEND
                    }
                }
                context("when the special date maps to HOLIDAY") {
                    it("returns HOLIDAY regardless of dotw") {
                        val result = CaltrainSchedule.optionIndexFor("2026-12-25", 5, specialDates)
                        result shouldBe ScheduleType.HOLIDAY
                    }
                }
                context("on a date not in specialDates") {
                    it("falls back to dotw-based logic") {
                        val result = CaltrainSchedule.optionIndexFor("2026-06-17", 3, specialDates)
                        result shouldBe ScheduleType.WEEKDAY
                    }
                }
            }

            context("with an invalid special date value") {
                it("falls back to WEEKDAY") {
                    val specialDates = mapOf("2026-06-17" to 99)
                    val result = CaltrainSchedule.optionIndexFor("2026-06-17", 3, specialDates)
                    result shouldBe ScheduleType.WEEKDAY
                }
            }
        }

        describe(".forTomorrow()") {
            context("when today is Friday (5)") {
                it("returns WEEKEND for tomorrow (Saturday)") {
                    GoodTimes.debugOverrideDotw = 5
                    val gt = GoodTimes()
                    val result = CaltrainSchedule.forTomorrow(gt, emptyMap())
                    result shouldBe ScheduleType.WEEKEND
                }
            }
            context("when today is Sunday (0)") {
                it("returns WEEKDAY for tomorrow (Monday)") {
                    GoodTimes.debugOverrideDotw = 0
                    val gt = GoodTimes()
                    val result = CaltrainSchedule.forTomorrow(gt, emptyMap())
                    result shouldBe ScheduleType.WEEKDAY
                }
            }
            context("when today is Thursday (4)") {
                it("returns WEEKDAY for tomorrow (Friday)") {
                    GoodTimes.debugOverrideDotw = 4
                    val gt = GoodTimes()
                    val result = CaltrainSchedule.forTomorrow(gt, emptyMap())
                    result shouldBe ScheduleType.WEEKDAY
                }
            }
        }
    }
})
