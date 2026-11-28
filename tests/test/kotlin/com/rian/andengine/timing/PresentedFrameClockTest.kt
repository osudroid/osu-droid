package com.rian.andengine.timing

import org.junit.Assert.*
import org.junit.Test

class PresentedFrameClockTest {
    private val frame = 8_333_333L

    @Test
    fun `Values only change when a frame is processed`() {
        val clock = PresentedFrameClock()

        clock.onFramePresented(1_000_000_000L)
        clock.onFramePresented(1_000_000_000L + frame)

        assertEquals(0f, clock.elapsedFrameTime, 0f)

        clock.processFrame()

        assertEquals(0.008333333f, clock.elapsedFrameTime, 1e-6f)
        assertEquals(clock.elapsedFrameTime, clock.timeInfo.elapsed, 0f)
    }

    @Test
    fun `First frame after a reset counts no gap`() {
        val clock = PresentedFrameClock()

        clock.onFramePresented(5_000_000_000L)
        clock.processFrame()

        assertEquals(0f, clock.elapsedFrameTime, 0f)
        assertEquals(0f, clock.longestFrameTime, 0f)
    }

    @Test
    fun `Gaps keep their precision at large uptimes`() {
        val clock = PresentedFrameClock()
        val start = 1_000_000_000_000_000L // ~11.6 days of uptime

        clock.onFramePresented(start)
        clock.onFramePresented(start + frame)
        clock.processFrame()

        assertEquals(0.008333333f, clock.elapsedFrameTime, 1e-6f)
    }

    @Test
    fun `Longest frame time covers every frame since the previous processed frame`() {
        val clock = PresentedFrameClock()
        var t = 1_000_000_000L

        clock.onFramePresented(t)

        t += frame
        clock.onFramePresented(t)

        t += 25_000_000L
        clock.onFramePresented(t)

        t += frame
        clock.onFramePresented(t)

        clock.processFrame()

        assertEquals(0.025f, clock.longestFrameTime, 1e-6f)
        assertEquals(0.008333333f, clock.elapsedFrameTime, 1e-6f)

        clock.processFrame()

        assertEquals(0f, clock.longestFrameTime, 0f)
    }

    @Test
    fun `Frames per second follows the presented rate`() {
        val clock = PresentedFrameClock()
        var t = 1_000_000_000L

        repeat(240) {
            clock.onFramePresented(t)
            t += frame
            clock.processFrame()
        }

        assertEquals(120f, clock.framesPerSecond, 1f)
    }

    @Test
    fun `Gaps longer than a second restart without counting`() {
        val clock = PresentedFrameClock()

        clock.onFramePresented(1_000_000_000L)
        clock.onFramePresented(3_000_000_000L)
        clock.processFrame()

        assertEquals(0f, clock.elapsedFrameTime, 0f)
        assertEquals(0f, clock.longestFrameTime, 0f)

        clock.onFramePresented(3_000_000_000L + frame)
        clock.processFrame()

        assertEquals(0.008333333f, clock.elapsedFrameTime, 1e-6f)
    }

    @Test
    fun `Non-increasing present times are ignored`() {
        val clock = PresentedFrameClock()

        clock.onFramePresented(2_000_000_000L)
        clock.onFramePresented(2_000_000_000L)
        clock.onFramePresented(1_990_000_000L)
        clock.processFrame()

        assertEquals(0f, clock.longestFrameTime, 0f)

        clock.onFramePresented(2_000_000_000L + frame)
        clock.processFrame()

        assertEquals(0.008333333f, clock.elapsedFrameTime, 1e-6f)
    }

    @Test
    fun `Reset clears the frame rate and the previous frame`() {
        val clock = PresentedFrameClock()
        var t = 1_000_000_000L

        repeat(120) {
            clock.onFramePresented(t)
            t += frame
            clock.processFrame()
        }

        assertTrue(clock.framesPerSecond > 0f)

        clock.reset()
        clock.processFrame()

        assertEquals(0f, clock.framesPerSecond, 0f)

        clock.onFramePresented(t + frame)
        clock.processFrame()

        assertEquals(0f, clock.elapsedFrameTime, 0f)
    }

    @Test
    fun `Reset leaves availability unchanged`() {
        val clock = PresentedFrameClock()

        clock.isAvailable = true
        clock.reset()
        clock.processFrame()

        assertTrue(clock.isAvailable)
    }

    @Test
    fun `Running follows availability`() {
        val clock = PresentedFrameClock()

        assertFalse(clock.isRunning)

        clock.isAvailable = true

        assertTrue(clock.isRunning)
        assertEquals(1f, clock.rate, 0f)
    }
}
