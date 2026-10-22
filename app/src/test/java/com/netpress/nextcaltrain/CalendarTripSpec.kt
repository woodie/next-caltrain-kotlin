package com.netpress.nextcaltrain

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class CalendarTripSpec : DescribeSpec({

    // Palo Alto to San Mateo, weekday: #523 5:43pm, #151 5:55pm, #425 6:10pm, #153 6:25pm
    fun trip(trainId: Int, depart: Int, arrive: Int) =
        Trip(id = trainId, legs = listOf(Leg(trainId, "Palo Alto", depart)), arrive = arrive)
    val routes = listOf(
        trip(523, 17 * 60 + 43, 17 * 60 + 59),
        trip(151, 17 * 60 + 55, 18 * 60 + 17),
        trip(425, 18 * 60 + 10, 18 * 60 + 28),
        trip(153, 18 * 60 + 25, 18 * 60 + 47),
    )
    fun labels(index: Int?) = CalendarTrip.neighbors(routes, index!!).map { it.label }

    // Shows a time as Pacific wall-clock time.
    fun pacific(time: ZonedDateTime) = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

    describe("CalendarTrip") {
        describe(".find()") {
            context("leaving at 5:50pm") {
                it("picks the first train departing at or after then") {
                    labels(CalendarTrip.find(routes, PlanMode.LEAVE, 17 * 60 + 50)) shouldBe
                        listOf("#523", "#151", "#425")
                }
            }

            context("arriving by 6:20pm") {
                it("picks the last train arriving at or before then") {
                    labels(CalendarTrip.find(routes, PlanMode.ARRIVE, 18 * 60 + 20)) shouldBe
                        listOf("#523", "#151", "#425")
                }
            }

            context("leaving after the last train") {
                it("finds nothing") {
                    CalendarTrip.find(routes, PlanMode.LEAVE, 1500).shouldBeNull()
                }
            }
        }

        describe(".neighbors()") {
            it("marks the planned train") {
                val index = CalendarTrip.find(routes, PlanMode.LEAVE, 17 * 60 + 55)!!
                CalendarTrip.neighbors(routes, index).map { it.selected } shouldBe listOf(false, true, false)
            }

            it("has no train before the first one of the day") {
                CalendarTrip.neighbors(routes, 0) shouldHaveSize 2
            }
        }

        describe(".line() and .notes()") {
            val trains = CalendarTrip.neighbors(routes, CalendarTrip.find(routes, PlanMode.LEAVE, 17 * 60 + 55)!!)

            it("formats a train") {
                trains.map(CalendarTrip::line) shouldBe
                    listOf("#523  5:43pm – 5:59pm", "#151  5:55pm – 6:17pm", "#425  6:10pm – 6:28pm")
            }

            it("lists the route and the trains") {
                CalendarTrip.notes("Palo Alto", "San Mateo", trains) shouldBe
                    "Palo Alto to San Mateo\n#523  5:43pm – 5:59pm\n#151  5:55pm – 6:17pm\n#425  6:10pm – 6:28pm"
            }
        }

        describe(".location()") {
            it("names the station") {
                CalendarTrip.location("Palo Alto") shouldBe "Palo Alto Caltrain Station"
            }
        }

        describe(".date()") {
            it("is that time on Pacific time") {
                pacific(CalendarTrip.date("2026-05-21", 17 * 60 + 55)) shouldBe "2026-05-21 17:55"
            }

            it("rolls times past midnight over to the next day") {
                pacific(CalendarTrip.date("2026-12-31", 1445)) shouldBe "2027-01-01 00:05"
            }
        }

        describe(".dotw()") {
            it("is Thursday for 2026-05-21") {
                CalendarTrip.dotw("2026-05-21") shouldBe 4
            }
        }
    }
})
