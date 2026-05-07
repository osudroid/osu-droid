package com.rian.andengine.timing

import org.junit.Assert.*
import org.junit.Test

class StopwatchClockTest {
    @Test
    fun `Test reset time`() {
        val clock = StopwatchClock()
        clock.start()

        Thread.sleep(1000)

        assertTrue(clock.currentTime > 0)

        clock.stop()
        clock.reset()

        assertEquals(0f, clock.currentTime, 0f)
    }

    @Test
    fun `Test rate up reset time`() {
        val clock = StopwatchClock()
        clock.start()

        Thread.sleep(1000)

        clock.stop()
        val stoppedTime = clock.currentTime
        assertTrue(stoppedTime > 0)

        clock.rate = 2f
        assertEquals(stoppedTime, clock.currentTime)

        clock.reset()

        assertEquals(0f, clock.currentTime, 0f)
    }

    @Test
    fun `Test seek while stopped`() {
        val clock = StopwatchClock()
        clock.seek(5f)
        assertEquals(5f, clock.currentTime, 0f)
    }

    @Test
    fun `Test seek when non-zero`() {
        val clock = StopwatchClock()
        clock.start()

        Thread.sleep(1000)

        clock.stop()
        val stoppedTime = clock.currentTime
        assertTrue(stoppedTime > 0)

        clock.seek(stoppedTime)

        assertEquals(stoppedTime, clock.currentTime, 0f)
    }

    @Test
    fun `Test seek negative adjust rate`() {
        val clock = StopwatchClock()
        clock.seek(-5f)
        assertEquals(-5f, clock.currentTime, 0f)

        clock.rate = 2f
        clock.start()

        Thread.sleep(1000)

        clock.stop()
        val stoppedTime = clock.currentTime
        assertTrue(stoppedTime < 0)

        clock.seek(stoppedTime)

        assertEquals(stoppedTime, clock.currentTime, 0f)
    }

    @Test
    fun `Test negative rate`() {
        val clock = StopwatchClock()
        clock.rate = -2f
        clock.start()

        Thread.sleep(1000)

        clock.stop()

        assertTrue(clock.currentTime < 0)
    }
}