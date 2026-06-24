package com.netpress.nextcaltrain

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe

class CaltrainServiceSpec : DescribeSpec({

    describe("CaltrainService") {

        describe(".isSouthCounty()") {
            it("returns true for South County trains (801-900)") {
                CaltrainService.isSouthCounty(801).shouldBeTrue()
                CaltrainService.isSouthCounty(814).shouldBeTrue()
                CaltrainService.isSouthCounty(900).shouldBeTrue()
            }
            it("returns false for electric trains") {
                CaltrainService.isSouthCounty(101).shouldBeFalse()
                CaltrainService.isSouthCounty(514).shouldBeFalse()
            }
        }

        describe(".direction()") {
            it("returns South when origin index < destination index") {
                val dir = CaltrainService.direction(
                    SpecFixtures.sanFrancisco,
                    SpecFixtures.gilroy,
                    SpecFixtures.stops,
                )
                dir shouldBe "South"
            }
            it("returns North when origin index > destination index") {
                val dir = CaltrainService.direction(
                    SpecFixtures.gilroy,
                    SpecFixtures.sanFrancisco,
                    SpecFixtures.stops,
                )
                dir shouldBe "North"
            }
        }

        describe("#routes() direct routes") {
            val schedule = SpecFixtures.weekdayOnlySchedule()
            val service = CaltrainService(schedule)

            context("SF -> SJD on Weekday") {
                val routes = service.routes(
                    SpecFixtures.sanFrancisco,
                    SpecFixtures.sanJoseDiridon,
                    ScheduleType.WEEKDAY,
                )
                it("returns trips") { routes.shouldNotBeEmpty() }
                it("each trip is a direct route (one leg)") {
                    routes.forEach { it.isTransfer.shouldBeFalse() }
                }
                it("trips are sorted by departure time") {
                    for (i in 1 until routes.size) {
                        routes[i].depart shouldBeGreaterThanOrEqual routes[i - 1].depart
                    }
                }
            }

            context("Morgan Hill -> Gilroy on Weekday") {
                val routes = service.routes(
                    SpecFixtures.morganHill,
                    SpecFixtures.gilroy,
                    ScheduleType.WEEKDAY,
                )
                it("returns direct trips (both SC, no transfer needed)") {
                    routes.shouldNotBeEmpty()
                    routes.forEach { it.isTransfer.shouldBeFalse() }
                }
            }

            context("unknown station") {
                it("returns empty") {
                    val routes = service.routes("Unknown", SpecFixtures.gilroy, ScheduleType.WEEKDAY)
                    routes.shouldBeEmpty()
                }
            }
        }

        describe("#routes() transfer routes") {
            val schedule = SpecFixtures.weekdayOnlySchedule()
            val service = CaltrainService(schedule)

            context("southbound SF -> Gilroy on Weekday") {
                val routes = service.routes(
                    SpecFixtures.sanFrancisco,
                    SpecFixtures.gilroy,
                    ScheduleType.WEEKDAY,
                )
                it("returns trips") { routes.shouldNotBeEmpty() }
                it("each trip is a transfer (two legs)") {
                    routes.forEach { it.isTransfer.shouldBeTrue() }
                }
                it("lead train is electric (non-South County)") {
                    routes.forEach { CaltrainService.isSouthCounty(it.id).shouldBeFalse() }
                }
                it("second leg train is South County") {
                    routes.forEach { CaltrainService.isSouthCounty(it.legs[1].trainId).shouldBeTrue() }
                }
                it("trips are sorted by departure time") {
                    for (i in 1 until routes.size) {
                        routes[i].depart shouldBeGreaterThanOrEqual routes[i - 1].depart
                    }
                }
            }

            context("northbound Gilroy -> SF on Weekday") {
                val routes = service.routes(
                    SpecFixtures.gilroy,
                    SpecFixtures.sanFrancisco,
                    ScheduleType.WEEKDAY,
                )
                it("returns trips") { routes.shouldNotBeEmpty() }
                it("each trip is a transfer (two legs)") {
                    routes.forEach { it.isTransfer.shouldBeTrue() }
                }
                it("lead train is South County") {
                    routes.forEach { CaltrainService.isSouthCounty(it.id).shouldBeTrue() }
                }
                it("second leg train is electric") {
                    routes.forEach { CaltrainService.isSouthCounty(it.legs[1].trainId).shouldBeFalse() }
                }
            }

            context("SF -> Gilroy on Weekend") {
                it("returns no trips (no South County weekend service)") {
                    val schedule = SpecFixtures.schedule {
                        weekday(electric = SpecFixtures.Service.NORMAL, diesel = SpecFixtures.Service.NORMAL)
                        weekend(electric = SpecFixtures.Service.NORMAL, diesel = SpecFixtures.Service.NONE)
                    }
                    val routes = CaltrainService(schedule).routes(
                        SpecFixtures.sanFrancisco,
                        SpecFixtures.gilroy,
                        ScheduleType.WEEKEND,
                    )
                    routes.shouldBeEmpty()
                }
            }
        }

        describe("#nextIndex()") {
            val schedule = SpecFixtures.weekdayOnlySchedule()
            val service = CaltrainService(schedule)
            val trips = service.routes(
                SpecFixtures.sanFrancisco,
                SpecFixtures.sanJoseDiridon,
                ScheduleType.WEEKDAY,
            )

            it("returns 0 when current time is before all trips") {
                service.nextIndex(trips, 0) shouldBe 0
            }
            it("returns trips.size when current time is after all trips") {
                service.nextIndex(trips, 9999) shouldBe trips.size
            }
            it("returns the index of the first trip at or after current time") {
                val firstDepart = trips.first().depart
                service.nextIndex(trips, firstDepart) shouldBe 0
                service.nextIndex(trips, firstDepart + 1) shouldBe 1
            }
        }
    }
})
