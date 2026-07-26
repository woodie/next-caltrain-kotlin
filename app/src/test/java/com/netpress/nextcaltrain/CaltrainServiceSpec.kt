package com.netpress.nextcaltrain

import com.netpress.kwick.justBeforeEach
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
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
            lateinit var origin: String
            lateinit var destin: String
            lateinit var dir: String
            justBeforeEach { dir = CaltrainService.direction(origin, destin, SpecFixtures.stops) }

            context("when traveling from San Francisco to Gilroy") {
                beforeEach { origin = SpecFixtures.sanFrancisco; destin = SpecFixtures.gilroy }
                it("is South") { dir shouldBe "South" }
            }

            context("when traveling from Gilroy to San Francisco") {
                beforeEach { origin = SpecFixtures.gilroy; destin = SpecFixtures.sanFrancisco }
                it("is North") { dir shouldBe "North" }
            }
        }

        describe("#routes()") {
            val schedule = SpecFixtures.weekdayOnlySchedule()
            val service = CaltrainService(schedule)

            context("for a direct electric trip (San Francisco to San Jose Diridon)") {
                val routes = service.routes(
                    SpecFixtures.sanFrancisco,
                    SpecFixtures.sanJoseDiridon,
                    ScheduleType.WEEKDAY,
                )

                it("returns one direct trip") {
                    routes shouldHaveSize 1
                }
                it("is not a transfer") {
                    routes.first().isTransfer.shouldBeFalse()
                }
                it("uses the electric southbound train") {
                    routes.first().id shouldBe SpecFixtures.electricSouthTrainId
                }
                it("departs and arrives at the scheduled times") {
                    routes.first().depart shouldBe 480
                    routes.first().arrive shouldBe 510
                }
            }

            context("for a direct diesel trip (Morgan Hill to Gilroy)") {
                val routes = service.routes(
                    SpecFixtures.morganHill,
                    SpecFixtures.gilroy,
                    ScheduleType.WEEKDAY,
                )

                it("returns one direct trip") {
                    routes shouldHaveSize 1
                }
                it("is not a transfer, since both endpoints are South County") {
                    routes.first().isTransfer.shouldBeFalse()
                }
                it("uses the diesel southbound train") {
                    routes.first().id shouldBe SpecFixtures.dieselSouthTrainId
                }
            }

            context("for an unknown station") {
                val routes = service.routes("Unknown", SpecFixtures.gilroy, ScheduleType.WEEKDAY)

                it("returns no trips") {
                    routes.shouldBeEmpty()
                }
            }

            context("for a transfer trip (San Francisco to Gilroy)") {
                val routes = service.routes(
                    SpecFixtures.sanFrancisco,
                    SpecFixtures.gilroy,
                    ScheduleType.WEEKDAY,
                )

                it("returns one trip") {
                    routes shouldHaveSize 1
                }
                it("is a transfer") {
                    routes.first().isTransfer.shouldBeTrue()
                }
                it("has two legs") {
                    routes.first().legs shouldHaveSize 2
                }
                it("starts with the electric train from San Francisco") {
                    val leg1 = routes.first().legs[0]
                    leg1.trainId shouldBe SpecFixtures.electricSouthTrainId
                    leg1.station shouldBe SpecFixtures.sanFrancisco
                    leg1.depart shouldBe 480
                }
                it("connects to the diesel train at San Jose Diridon") {
                    val leg2 = routes.first().legs[1]
                    leg2.trainId shouldBe SpecFixtures.dieselSouthTrainId
                    leg2.station shouldBe SpecFixtures.sanJoseDiridon
                    leg2.depart shouldBe 515
                }
                it("arrives in Gilroy at the diesel train's scheduled time") {
                    routes.first().arrive shouldBe 545
                }
            }

            context("for a transfer trip (Gilroy to San Francisco)") {
                val routes = service.routes(
                    SpecFixtures.gilroy,
                    SpecFixtures.sanFrancisco,
                    ScheduleType.WEEKDAY,
                )

                it("returns one trip") {
                    routes shouldHaveSize 1
                }
                it("is a transfer") {
                    routes.first().isTransfer.shouldBeTrue()
                }
                it("starts with the diesel train from Gilroy") {
                    val leg1 = routes.first().legs[0]
                    leg1.trainId shouldBe SpecFixtures.dieselNorthTrainId
                    leg1.station shouldBe SpecFixtures.gilroy
                    leg1.depart shouldBe 420
                }
                it("connects to the electric train at San Jose Diridon") {
                    val leg2 = routes.first().legs[1]
                    leg2.trainId shouldBe SpecFixtures.electricNorthTrainId
                    leg2.station shouldBe SpecFixtures.sanJoseDiridon
                    leg2.depart shouldBe 520
                }
                it("arrives in San Francisco at the electric train's scheduled time") {
                    routes.first().arrive shouldBe 550
                }
            }

            context("for a route with no service (weekend, empty fixture tables)") {
                val routes = service.routes(
                    SpecFixtures.sanFrancisco,
                    SpecFixtures.gilroy,
                    ScheduleType.WEEKEND,
                )

                it("returns no trips") {
                    routes.shouldBeEmpty()
                }
            }
        }

        describe("#nextIndex()") {
            val schedule = SpecFixtures.weekdayOnlySchedule()
            val service = CaltrainService(schedule)

            context("when no trips have departed yet") {
                val trips = service.routes(
                    SpecFixtures.sanFrancisco,
                    SpecFixtures.sanJoseDiridon,
                    ScheduleType.WEEKDAY,
                )
                val index = service.nextIndex(trips, 0)

                it("returns 0") {
                    index shouldBe 0
                }
            }

            context("when all trips have already departed") {
                val trips = service.routes(
                    SpecFixtures.sanFrancisco,
                    SpecFixtures.sanJoseDiridon,
                    ScheduleType.WEEKDAY,
                )
                val index = service.nextIndex(trips, 1000)

                it("returns the trip count") {
                    index shouldBe trips.size
                }
            }

            context("when given an empty trip list") {
                val index = service.nextIndex(emptyList(), 500)

                it("returns 0") {
                    index shouldBe 0
                }
            }

            context("when the current time is exactly at the first trip's departure") {
                val trips = service.routes(
                    SpecFixtures.sanFrancisco,
                    SpecFixtures.sanJoseDiridon,
                    ScheduleType.WEEKDAY,
                )
                val firstDepart = trips.first().depart

                it("returns the index of that trip") {
                    service.nextIndex(trips, firstDepart) shouldBe 0
                }
                it("returns the next index one minute later") {
                    service.nextIndex(trips, firstDepart + 1) shouldBe 1
                }
            }
        }
    }
})
