package com.netpress.nextcaltrain

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import java.util.Calendar

class GoodTimesSpec : DescribeSpec({

    describe("GoodTimes") {
        lateinit var gt: GoodTimes

        describe(".partTime()") {
            context("when given a morning time") {
                val (t, mer) = GoodTimes.partTime(330) // 5:30am

                it("returns the time and 'am'") {
                    t shouldBe "5:30"
                    mer shouldBe "am"
                }
            }

            context("when given noon") {
                val (t, mer) = GoodTimes.partTime(720) // noon

                it("returns 12:00 and 'pm'") {
                    t shouldBe "12:00"
                    mer shouldBe "pm"
                }
            }

            context("when given midnight") {
                val (t, mer) = GoodTimes.partTime(0) // 12:00am

                it("returns 12:00 and 'am'") {
                    t shouldBe "12:00"
                    mer shouldBe "am"
                }
            }

            context("when given a today's-schedule post-midnight time (24:00-25:59 range)") {
                val (t, mer) = GoodTimes.partTime(1445) // 24:05

                it("formats 24:05 as 12:05am") {
                    t shouldBe "12:05"
                    mer shouldBe "am"
                }
            }

            context("when given a tomorrow-shifted time (>= 1440)") {
                context("at 1740 (29:00)") {
                    val (t, mer) = GoodTimes.partTime(1740)

                    it("wraps to 5:00am") {
                        t shouldBe "5:00"
                        mer shouldBe "am"
                    }
                }

                context("at 1620 (27:00)") {
                    val (t, mer) = GoodTimes.partTime(1620)

                    it("wraps to 3:00am") {
                        t shouldBe "3:00"
                        mer shouldBe "am"
                    }
                }
            }
        }

        describe(".fullTime()") {
            context("when given noon") {
                val result = GoodTimes.fullTime(720) // noon

                it("returns '12:00pm'") {
                    result shouldBe "12:00pm"
                }
            }
        }

        context("when 'now' is fixed via debugOverrideMinutes") {
            var minutes = 0
            beforeEach { GoodTimes.debugOverrideMinutes = 720; gt = GoodTimes() }

            describe("#inThePast()") {
                context("when the target is before now") {
                    beforeEach { minutes = gt.minutes - 2 }

                    it("returns true") {
                        gt.inThePast(minutes).shouldBeTrue()
                    }
                }

                context("when the target is after now") {
                    beforeEach { minutes = gt.minutes + 2 }

                    it("returns false") {
                        gt.inThePast(gt.minutes + 2).shouldBeFalse()
                    }
                }
            }

            describe("#departing()") {
                context("when the target equals now") {
                    beforeEach { minutes = gt.minutes }

                    it("returns true") {
                        gt.departing(minutes).shouldBeTrue()
                    }
                }

                context("when the target does not equal now") {
                    beforeEach { minutes = gt.minutes + 1 }

                    it("returns false") {
                        gt.departing(gt.minutes + 1).shouldBeFalse()
                    }
                }
            }

            describe("#countdown()") {
                context("when the target is in the past") {
                    beforeEach { minutes = gt.minutes - 1 }

                    it("returns an empty string") {
                        gt.countdown(minutes) shouldBe ""
                    }
                }

                context("when the target is more than an hour away") {
                    beforeEach { minutes = gt.minutes + 66 }

                    it("formats as 'in N hr M min'") {
                        gt.countdown(gt.minutes + 66) shouldBe "in 1 hr 5 min"
                    }
                }

                context("when the target is less than an hour away") {
                    beforeEach { minutes = gt.minutes + 5 }

                    it("formats as 'in N min M sec'") {
                        gt.countdown(gt.minutes + 5).shouldStartWith("in 4 min")
                    }
                }
            }
        }

        context("when 'today' is fixed via debugOverrideDotw") {
            fun setDotw(dotw: Int) {
                GoodTimes.debugOverrideDotw = dotw
                gt = GoodTimes()
            }
            afterEach { GoodTimes.debugOverrideDotw = null }

            context("and today is Friday (5)") {
                beforeEach { setDotw(5) }

                it("computes tomorrow as Saturday (6)") {
                    gt.dotw shouldBe 5
                    gt.tomorrowDotw shouldBe 6
                }
            }
            context("and today is Saturday (6)") {
                beforeEach { setDotw(6) }

                it("computes tomorrow as Sunday (0), wrapping the week") {
                    gt.dotw shouldBe 6
                    gt.tomorrowDotw shouldBe 0
                }
            }
            context("and today is Sunday (0)") {
                beforeEach { setDotw(0) }

                it("computes tomorrow as Monday (1)") {
                    gt.dotw shouldBe 0
                    gt.tomorrowDotw shouldBe 1
                }
            }
        }

        describe(".scheduleDateFor()") {
            // Used by the once-per-day schedule fetch cap to decide whether a
            // stored "last fetched at" timestamp still counts as "today" under
            // the same "day starts at 2am" rule GoodTimes() itself uses. These
            // tests build both timestamps from the same Calendar so the
            // comparison holds regardless of the machine's default timezone.
            val cal = Calendar.getInstance()

            it("returns the same schedule-day for two instants on the same calendar day, both after 2am") {
                cal.set(2026, Calendar.JUNE, 15, 10, 0, 0)
                val morning = cal.timeInMillis
                cal.set(2026, Calendar.JUNE, 15, 23, 0, 0)
                val night = cal.timeInMillis

                GoodTimes.scheduleDateFor(morning) shouldBe GoodTimes.scheduleDateFor(night)
            }

            it("treats 1am as still belonging to the previous schedule-day") {
                cal.set(2026, Calendar.JUNE, 15, 23, 0, 0)
                val lateNight = GoodTimes.scheduleDateFor(cal.timeInMillis)
                cal.set(2026, Calendar.JUNE, 16, 1, 0, 0)
                val earlyMorning = GoodTimes.scheduleDateFor(cal.timeInMillis)

                earlyMorning shouldBe lateNight
            }

            it("rolls over to the next schedule-day right at the 2am boundary") {
                cal.set(2026, Calendar.JUNE, 16, 1, 59, 0)
                val before = GoodTimes.scheduleDateFor(cal.timeInMillis)
                cal.set(2026, Calendar.JUNE, 16, 2, 1, 0)
                val after = GoodTimes.scheduleDateFor(cal.timeInMillis)

                after shouldNotBe before
            }
        }
    }
})
