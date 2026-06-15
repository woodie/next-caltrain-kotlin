package com.netpress.nextcaltrain

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith

class GoodTimesSpec : DescribeSpec({

    afterEach {
        GoodTimes.debugOverrideMinutes = null
        GoodTimes.debugOverrideDotw = null
    }

    describe("GoodTimes") {

        describe(".partTime()") {
            context("when given a morning time") {
                it("returns the time and 'am'") {
                    val (t, mer) = GoodTimes.partTime(330) // 5:30am
                    t shouldBe "5:30"
                    mer shouldBe "am"
                }
            }
            context("when given noon") {
                it("returns 12:00 and 'pm'") {
                    val (t, mer) = GoodTimes.partTime(720)
                    t shouldBe "12:00"
                    mer shouldBe "pm"
                }
            }
            context("when given midnight") {
                it("returns 12:00 and 'am'") {
                    val (t, mer) = GoodTimes.partTime(0)
                    t shouldBe "12:00"
                    mer shouldBe "am"
                }
            }
            context("when given a tomorrow-shifted time at 1740 (29:00)") {
                it("wraps to 5:00am") {
                    val (t, mer) = GoodTimes.partTime(1740)
                    t shouldBe "5:00"
                    mer shouldBe "am"
                }
            }
        }

        describe(".fullTime()") {
            context("when given noon") {
                it("returns '12:00pm'") {
                    GoodTimes.fullTime(720) shouldBe "12:00pm"
                }
            }
        }

        context("when 'now' is fixed via debugOverrideMinutes") {
            beforeEach { GoodTimes.debugOverrideMinutes = 720 }

            describe("#inThePast()") {
                it("returns true when the target is before now") {
                    val gt = GoodTimes()
                    gt.inThePast(gt.minutes - 2).shouldBeTrue()
                }
                it("returns false when the target is after now") {
                    val gt = GoodTimes()
                    gt.inThePast(gt.minutes + 2).shouldBeFalse()
                }
            }

            describe("#departing()") {
                it("returns true when the target equals now") {
                    val gt = GoodTimes()
                    gt.departing(gt.minutes).shouldBeTrue()
                }
                it("returns false when the target does not equal now") {
                    val gt = GoodTimes()
                    gt.departing(gt.minutes + 1).shouldBeFalse()
                }
            }

            describe("#countdown()") {
                it("returns empty string for a past target") {
                    val gt = GoodTimes()
                    gt.countdown(gt.minutes - 1) shouldBe ""
                }
                it("formats as 'in N hr M min' when over an hour away") {
                    val gt = GoodTimes()
                    gt.countdown(gt.minutes + 66) shouldBe "in 1 hr 5 min"
                }
                it("formats as 'in N min M sec' when under an hour away") {
                    val gt = GoodTimes()
                    gt.countdown(gt.minutes + 5).shouldStartWith("in 4 min")
                }
            }
        }

        context("when 'today' is fixed via debugOverrideDotw") {
            context("and today is Friday (5)") {
                it("computes tomorrow as Saturday (6)") {
                    GoodTimes.debugOverrideDotw = 5
                    val gt = GoodTimes()
                    gt.dotw shouldBe 5
                    gt.tomorrowDotw shouldBe 6
                }
            }
            context("and today is Saturday (6)") {
                it("computes tomorrow as Sunday (0), wrapping the week") {
                    GoodTimes.debugOverrideDotw = 6
                    val gt = GoodTimes()
                    gt.tomorrowDotw shouldBe 0
                }
            }
            context("and today is Sunday (0)") {
                it("computes tomorrow as Monday (1)") {
                    GoodTimes.debugOverrideDotw = 0
                    val gt = GoodTimes()
                    gt.tomorrowDotw shouldBe 1
                }
            }
        }
    }
})
