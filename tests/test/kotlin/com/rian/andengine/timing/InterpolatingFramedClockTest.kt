package com.rian.andengine.timing

import com.rian.osu.math.Precision
import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

sealed class BaseInterpolatingFramedClockTest {
    protected lateinit var source: TestClock
    protected lateinit var interpolating: InterpolatingFramedClock

    @Before
    open fun setUp() {
        source = TestClock()

        interpolating = InterpolatingFramedClock()
        interpolating.changeSource(source)
    }
}

class MainInterpolatingFramedClockTest : BaseInterpolatingFramedClockTest() {
    @Test
    fun `Test never interpolates backwards`() {
        assertEquals("Interpolating should match source time.", interpolating.currentTime, source.currentTime)
        source.start()
        assertEquals("Interpolating should match source time.", interpolating.currentTime, source.currentTime)
        interpolating.processFrame()

        // Test with test clock not elapsing
        var lastValue = interpolating.currentTime

        (0..100).forEach { _ ->
            interpolating.processFrame()

            assertTrue("Interpolating should not jump against rate.", interpolating.currentTime >= lastValue)
            assertTrue("Interpolating should not jump before source time.", interpolating.currentTime >= source.currentTime)

            Thread.sleep((interpolating.allowableErrorSeconds / 2 * 1e3).toLong())
            lastValue = interpolating.currentTime
        }

        var interpolatedCount = 0

        // Test with test clock elapsing
        lastValue = interpolating.currentTime

        (0..100).forEach { _ ->
            // We want to interpolate but not fall behind and fail interpolation too much.
            source.currentTime += interpolating.allowableErrorSeconds / 2 + 0.005f
            interpolating.processFrame()

            assertTrue("Interpolating should not jump against rate.", interpolating.currentTime >= lastValue)
            assertTrue("Interpolating should be within allowance.", abs(interpolating.currentTime - source.currentTime) <= interpolating.allowableErrorSeconds)

            if (interpolating.isInterpolating) {
                ++interpolatedCount
            }

            Thread.sleep((interpolating.allowableErrorSeconds / 2 * 1e3).toLong())
            lastValue = interpolating.currentTime
        }

        assertTrue(interpolatedCount > 10)
    }

    @Test
    fun `Test source change transfers value adjustable`() {
        // For interpolating clocks, value transfer is always in the direction of the interpolating clock.
        val firstSourceTime = 256f
        val secondSourceTime = 128f

        source.seek(firstSourceTime)

        val secondSource = TestClock().apply {
            // More importantly, test a value lower than the original source. This is to both test value transfer *and*
            // the case where time is going backwards, as some clocks have special provisions for this.
            currentTime = secondSourceTime
        }

        interpolating.processFrame()
        assertEquals(firstSourceTime, interpolating.currentTime)

        interpolating.changeSource(secondSource)
        interpolating.processFrame()

        assertEquals(secondSourceTime, secondSource.currentTime)
        assertEquals(secondSourceTime, interpolating.currentTime)
    }

    @Test
    fun `Test source change transfers value non-adjustable`() {
        // For interpolating clocks, value transfer is always in the direction of the interpolating clock.
        val firstSourceTime = 256f
        val secondSourceTime = 128f

        source.seek(firstSourceTime)

        val secondSource = TestClock().apply {
            // More importantly, test a value lower than the original source. This is to both test value transfer *and*
            // the case where time is going backwards, as some clocks have special provisions for this.
            currentTime = secondSourceTime
        }

        interpolating.processFrame()
        assertEquals(firstSourceTime, interpolating.currentTime)

        interpolating.changeSource(secondSource)
        interpolating.processFrame()

        assertEquals(secondSourceTime, secondSource.currentTime)
        assertEquals(secondSourceTime, interpolating.currentTime)
    }

    @Test
    fun `Test never interpolates backwards on interpolation fail`() {
        val sleepTime = 20L

        var lastValue = interpolating.currentTime
        source.start()
        var interpolatedCount = 0

        for (i in 0 until 200) {
            source.rate += i * 10

            if (i < 100) {
                // Stop the elapsing at some point in time. should still work as source's elapsedTime is zero.
                source.currentTime += sleepTime * source.rate
            }

            interpolating.processFrame()

            if (interpolating.isInterpolating) {
                ++interpolatedCount
            }

            assertTrue("Interpolating should not jump against rate.", interpolating.currentTime >= lastValue)
            assertTrue("Interpolating should be within allowance.", abs(interpolating.currentTime - source.currentTime) <= interpolating.allowableErrorSeconds * source.rate)

            Thread.sleep(sleepTime)
            lastValue = interpolating.currentTime
        }

        assertTrue(interpolatedCount > 10)
    }

    @Test
    fun `Test can seek forwards on interpolation fail`() {
        val sleepTime = 20L

        var lastValue = interpolating.currentTime
        source.start()
        var interpolatedCount = 0

        for (i in 0 until 200) {
            source.rate += i * 10

            // Seek forward once at a random point.
            if (i == 100) {
                source.currentTime += interpolating.allowableErrorSeconds * 10 * source.rate
                interpolating.processFrame()
                assertFalse(interpolating.isInterpolating)
                assertEquals(source.currentTime, interpolating.currentTime, 0f)
            } else {
                source.currentTime += sleepTime * source.rate / 1000
                interpolating.processFrame()
            }

            if (interpolating.isInterpolating) {
                ++interpolatedCount
            }

            assertTrue("Interpolating should not jump against rate.", interpolating.currentTime >= lastValue)
            assertTrue("Interpolating should be within allowance.", abs(interpolating.currentTime - source.currentTime) <= interpolating.allowableErrorSeconds * source.rate)

            Thread.sleep(sleepTime)
            lastValue = interpolating.currentTime
        }

        assertTrue(interpolatedCount > 10)
    }

    @Test
    fun `Test can seek backwards`() {
        assertEquals("Interpolating should match source time.", interpolating.currentTime, source.currentTime)
        source.start()

        assertEquals("Interpolating should match source time.", interpolating.currentTime, source.currentTime)
        interpolating.processFrame()

        source.seek(10f)
        interpolating.processFrame()
        assertEquals("Interpolating should match source time.", interpolating.currentTime, source.currentTime)

        source.seek(0f)
        interpolating.processFrame()
        assertEquals("Interpolating should match source time.", interpolating.currentTime, source.currentTime)
    }

    @Test
    fun `Test interpolation after source stopped then seeked`() {
        // Just to make sure this works even when still in interpolation allowance.
        interpolating.allowableErrorSeconds = 100f

        source.start()

        while (!interpolating.isInterpolating) {
            source.currentTime += 0.01f
            Thread.sleep(10)
            interpolating.processFrame()
        }

        source.stop()
        source.seek(-10f)

        interpolating.processFrame()
        assertFalse(interpolating.isInterpolating)
        assertEquals(-10f, interpolating.currentTime, 0.1f)
        assertEquals(-10f, interpolating.elapsedFrameTime, 0.1f)

        source.start()
        interpolating.processFrame()
        assertEquals(-10f, interpolating.currentTime, 0.1f)
        assertEquals(0f, interpolating.elapsedFrameTime, 0.1f)
    }

    @Test
    fun `Test interpolation stays within bounds`() {
        source.start()

        val sleepTime = 20L

        (0..100).forEach { _ ->
            source.currentTime += sleepTime / 1000f
            interpolating.processFrame()

            // Should be a no-op.
            interpolating.changeSource(source)

            assertTrue("Interpolating should be within allowable error bounds.", Precision.almostEquals(interpolating.currentTime, source.currentTime, interpolating.allowableErrorSeconds))

            Thread.sleep(sleepTime)
        }

        source.stop()
        interpolating.processFrame()

        assertFalse(interpolating.isRunning)
        assertEquals(interpolating.currentTime, source.currentTime, interpolating.allowableErrorSeconds)
    }
}

@RunWith(Parameterized::class)
class InterpolatingFramedClockTestNoDrift(private val updateRate: Long) : BaseInterpolatingFramedClockTest() {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "updateRate={0}")
        fun data() = arrayOf(0L, 1L, 10L, 50L)
    }

    @Test
    fun `Test no interpolation drift`() {
        val stopwatch = StopwatchClock()

        interpolating.changeSource(stopwatch)

        source.start()
        stopwatch.start()

        while (interpolating.currentTime <= 1f) {
            interpolating.processFrame()
            assertEquals(stopwatch.currentTime, interpolating.currentTime, 1e-3f)

            Thread.sleep(updateRate)
        }
    }
}