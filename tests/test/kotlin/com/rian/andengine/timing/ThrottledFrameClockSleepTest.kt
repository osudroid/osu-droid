package com.rian.andengine.timing

import org.junit.Assert.assertEquals
import org.junit.Test

class ThrottledFrameClockSleepTest {

    private class RecordingSleeper : ThrottledFrameClock.Sleeper {
        val calls = ArrayList<Pair<Long, Int>>()

        override fun sleep(milliseconds: Long, nanoseconds: Int) {
            calls += milliseconds to nanoseconds
        }
    }

    @Test
    fun subMillisecondRemainder_isSlept() {
        val sleeper = RecordingSleeper()
        // The source never advances, so the whole frame budget of 0.4 ms is left to sleep.
        val clock = ThrottledFrameClock(TestClock(), sleeper)
        clock.maximumUpdateHz = 2500f

        clock.processFrame()

        assertEquals(listOf(0L to 400_000), sleeper.calls)
    }

    @Test
    fun wholeMilliseconds_andRemainder_areSplit() {
        val sleeper = RecordingSleeper()
        val clock = ThrottledFrameClock(TestClock(), sleeper)
        clock.maximumUpdateHz = 400f

        clock.processFrame()

        assertEquals(listOf(2L to 500_000), sleeper.calls)
    }
}
