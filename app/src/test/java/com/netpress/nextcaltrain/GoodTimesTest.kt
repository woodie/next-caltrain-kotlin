package com.netpress.nextcaltrain

import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class GoodTimesTest {

    @After
    fun tearDown() {
        GoodTimes.debugOverrideMinutes = null
        GoodTimes.debugOverrideDotw = null
    }

    // MARK: - Static formatting

    @Test
    fun `partTime returns correct time and meridiem for morning`() {
        val (t, mer) = GoodTimes.partTime(330) // 5:30am
        assertEquals("5:30", t)
        assertEquals("am", mer)
    }

    @Test
    fun `partTime returns correct time and meridiem for noon`() {
        val (t, mer) = GoodTimes.partTime(720) // 12:00pm
        assertEquals("12:00", t)
        assertEquals("pm", mer)
    }

    @Test
    fun `partTime returns correct time and meridiem for midnight`() {
        val (t, mer) = GoodTimes.partTime(0)
        assertEquals("12:00", t)
        assertEquals("am", mer)
    }

    @Test
    fun `partTime wraps tomorrow-shifted time correctly`() {
        // 1740 = 29:00 -> wraps to 5:00am
        val (t, mer) = GoodTimes.partTime(1740)
        assertEquals("5:00", t)
        assertEquals("am", mer)
    }

    @Test
    fun `fullTime returns combined string`() {
        assertEquals("12:00pm", GoodTimes.fullTime(720))
    }

    // MARK: - Instance methods with debug overrides

    @Test
    fun `inThePast returns true for target before now`() {
        GoodTimes.debugOverrideMinutes = 720
        val gt = GoodTimes()
        assertTrue(gt.inThePast(gt.minutes - 2))
    }

    @Test
    fun `inThePast returns false for target after now`() {
        GoodTimes.debugOverrideMinutes = 720
        val gt = GoodTimes()
        assertFalse(gt.inThePast(gt.minutes + 2))
    }

    @Test
    fun `departing returns true when target equals now`() {
        GoodTimes.debugOverrideMinutes = 720
        val gt = GoodTimes()
        assertTrue(gt.departing(gt.minutes))
    }

    @Test
    fun `departing returns false when target does not equal now`() {
        GoodTimes.debugOverrideMinutes = 720
        val gt = GoodTimes()
        assertFalse(gt.departing(gt.minutes + 1))
    }

    @Test
    fun `countdown returns empty string for past target`() {
        GoodTimes.debugOverrideMinutes = 720
        val gt = GoodTimes()
        assertEquals("", gt.countdown(gt.minutes - 1))
    }

    @Test
    fun `countdown formats over one hour correctly`() {
        GoodTimes.debugOverrideMinutes = 720
        val gt = GoodTimes()
        assertEquals("in 1 hr 5 min", gt.countdown(gt.minutes + 66))
    }

    @Test
    fun `countdown formats under one hour correctly`() {
        GoodTimes.debugOverrideMinutes = 720
        val gt = GoodTimes()
        val result = gt.countdown(gt.minutes + 5)
        assertTrue(result.startsWith("in 4 min"))
    }

    // MARK: - tomorrowDotw with debugOverrideDotw

    @Test
    fun `tomorrowDotw is Saturday when today is Friday`() {
        GoodTimes.debugOverrideDotw = 5 // Friday
        val gt = GoodTimes()
        assertEquals(5, gt.dotw)
        assertEquals(6, gt.tomorrowDotw)
    }

    @Test
    fun `tomorrowDotw wraps to Sunday when today is Saturday`() {
        GoodTimes.debugOverrideDotw = 6 // Saturday
        val gt = GoodTimes()
        assertEquals(0, gt.tomorrowDotw)
    }

    @Test
    fun `tomorrowDotw is Monday when today is Sunday`() {
        GoodTimes.debugOverrideDotw = 0 // Sunday
        val gt = GoodTimes()
        assertEquals(1, gt.tomorrowDotw)
    }
}
