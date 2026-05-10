package com.rian.andengine.timing

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

sealed class BaseDecouplingFramedClockTest {
    protected lateinit var source: IAdjustableClock
    protected lateinit var realTimeClock: TestClock
    protected lateinit var decouplingClock: DecouplingFramedClock

    @Before
    open fun setUp() {
        source = TestClockWithRange()
        realTimeClock = TestClock().apply { start() }
        decouplingClock = DecouplingFramedClock(realtimeReferenceClockSource = realTimeClock)
        decouplingClock.changeSource(source)
    }
}

//region Basic assumptions (which hold for both decoupled and not)

@RunWith(Parameterized::class)
class DecouplingFramedClockDecouplingIndependentTest(private val allowDecoupling: Boolean) : BaseDecouplingFramedClockTest() {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "allowDecoupling = {0}")
        fun data() = arrayOf(true, false)
    }

    @Test
    fun `Test start from decoupling`() {
        decouplingClock.allowDecoupling = allowDecoupling

        assertFalse(source.isRunning)
        assertFalse(decouplingClock.isRunning)

        decouplingClock.start()
        decouplingClock.processFrame()

        assertTrue(source.isRunning)
        assertTrue(decouplingClock.isRunning)
    }

    @Test
    fun `Test start from source`() {
        decouplingClock.allowDecoupling = allowDecoupling

        assertFalse(source.isRunning)
        assertFalse(decouplingClock.isRunning)

        source.start()
        decouplingClock.processFrame()

        assertTrue(source.isRunning)
        assertTrue(decouplingClock.isRunning)
    }

    @Test
    fun `Test seeking from decoupling without processFrame`() {
        decouplingClock.allowDecoupling = true

        assertEquals(0f, source.currentTime, 0f)
        assertEquals(0f, decouplingClock.currentTime, 0f)

        decouplingClock.start()

        decouplingClock.seek(1f)
        decouplingClock.processFrame()
        assertTrue(source.isRunning)

        decouplingClock.seek(-1f)
        // Intentionally do not call processFrame.
        assertFalse(source.isRunning)

        decouplingClock.seek(-0.001f)

        // Intentionally make sure that reference time has increased to push time into positive before a process frame.
        realTimeClock.currentTime += 0.5f
        decouplingClock.processFrame()

        while (decouplingClock.currentTime < 0) {
            realTimeClock.currentTime += 0.01f
            decouplingClock.processFrame()
        }

        assertTrue(source.isRunning)
    }

    @Test
    fun `Test seek from decoupling`() {
        decouplingClock.allowDecoupling = allowDecoupling

        assertEquals(0f, source.currentTime, 0f)
        assertEquals(0f, decouplingClock.currentTime, 0f)

        decouplingClock.seek(1f)

        assertEquals(1f, source.currentTime, 0f)
        assertEquals(0f, decouplingClock.currentTime, 0f)

        decouplingClock.processFrame()

        assertEquals(1f, source.currentTime, 0f)
        assertEquals(1f, decouplingClock.currentTime, 0f)
    }

    @Test
    fun `Test seek from source`() {
        decouplingClock.allowDecoupling = allowDecoupling

        // Seeking the source when in decoupled mode is not really supported,
        // but it will work if the source is running.
        source.start()

        assertEquals(0f, source.currentTime, 0f)
        assertEquals(0f, decouplingClock.currentTime, 0f)

        source.seek(1f)
        decouplingClock.processFrame()

        assertEquals(1f, source.currentTime, 0f)
        assertEquals(1f, decouplingClock.currentTime, 0f)
    }

    @Test
    fun `Test change source updates to new source time`() {
        decouplingClock.allowDecoupling = allowDecoupling

        val firstSourceTime = 256f
        val secondSourceTime = 128f

        source.seek(firstSourceTime)
        source.start()

        decouplingClock.processFrame()

        val secondSource = TestClock().apply { currentTime = secondSourceTime }

        assertEquals(firstSourceTime, decouplingClock.currentTime, 0f)

        decouplingClock.changeSource(secondSource)
        decouplingClock.processFrame()

        assertEquals(secondSourceTime, secondSource.currentTime, 0f)
        assertEquals(secondSourceTime, decouplingClock.currentTime, 0f)
    }

    @Test
    fun `Test change source updates to correct source state`() {
        decouplingClock.allowDecoupling = allowDecoupling

        source.start()
        decouplingClock.processFrame()
        assertTrue(source.isRunning)

        val secondSource = TestClock()

        decouplingClock.changeSource(secondSource)
        decouplingClock.processFrame()
        assertFalse(decouplingClock.isRunning)

        decouplingClock.changeSource(source)
        decouplingClock.processFrame()
        assertTrue(decouplingClock.isRunning)
    }

    @Test
    fun `Test reset`() {
        decouplingClock.allowDecoupling = allowDecoupling

        source.seek(2f)
        source.start()

        decouplingClock.processFrame()

        assertTrue(decouplingClock.isRunning)
        assertEquals(2f, decouplingClock.currentTime, 0f)

        decouplingClock.reset()
        decouplingClock.processFrame()

        assertFalse(decouplingClock.isRunning)
        assertFalse(source.isRunning)
        assertEquals(0f, decouplingClock.currentTime, 0f)
        assertEquals(0f, source.currentTime, 0f)
    }
}

//endregion

//region Operation in non-decoupling mode

class DecouplingFramedClockNonDecouplingTest : BaseDecouplingFramedClockTest() {
    @Test
    fun `Test source stopped while not decoupling`() {
        decouplingClock.allowDecoupling = false
        decouplingClock.start()
        decouplingClock.processFrame()

        assertTrue(source.isRunning)
        assertTrue(decouplingClock.isRunning)

        source.stop()
        decouplingClock.processFrame()

        assertFalse(source.isRunning)
        assertFalse(decouplingClock.isRunning)
    }

    @Test
    fun `Test seek negative while not decoupling`() {
        decouplingClock.allowDecoupling = false

        assertFalse(decouplingClock.seek(-1f))

        assertEquals(0f, source.currentTime, 0f)
        assertEquals(0f, decouplingClock.currentTime, 0f)
    }

    @Test
    fun `Test seek positive while not decoupling`() {
        decouplingClock.allowDecoupling = false
        assertTrue(decouplingClock.seek(1f))
        decouplingClock.processFrame()

        assertEquals(1f, source.currentTime, 0f)
        assertEquals(1f, decouplingClock.currentTime, 0f)
    }
}

// endregion

//region Operation in decoupling mode

class DecouplingFramedClockDecouplingTest : BaseDecouplingFramedClockTest() {
    @Test
    fun `Test source stopped while decoupling`() {
        decouplingClock.allowDecoupling = true
        decouplingClock.start()
        decouplingClock.processFrame()

        assertTrue(source.isRunning)
        assertTrue(decouplingClock.isRunning)

        source.stop()

        assertFalse(source.isRunning)
        // We are decoupling, so we should still be running.
        assertTrue(decouplingClock.isRunning)
    }

    @Test
    fun `Test seek negative while decoupling`() {
        decouplingClock.allowDecoupling = true
        assertTrue(decouplingClock.seek(-1f))

        decouplingClock.processFrame()

        assertEquals(0f, source.currentTime, 0f)
        // We are decoupling, so we should be able to go beyond zero.
        assertEquals(-1f, decouplingClock.currentTime, 0f)
    }

    @Test
    fun `Test seek positive while decoupling`() {
        decouplingClock.allowDecoupling = true
        assertTrue(decouplingClock.seek(1f))
        decouplingClock.processFrame()

        assertEquals(1f, source.currentTime, 0f)
        assertEquals(1f, decouplingClock.currentTime, 0f)
    }

    @Test
    fun `Test seek beyond length while decoupling`() {
        source = TestStopwatchClockWithRangeLimit().apply { maxTime = 0.5f }

        decouplingClock.changeSource(source)
        decouplingClock.allowDecoupling = true

        assertTrue(decouplingClock.seek(1f))
        decouplingClock.processFrame()

        assertEquals(0.5f, source.currentTime, 0f)
        assertEquals(1f, decouplingClock.currentTime, 0f)
    }

    @Test
    fun `Test seek from negative to beyond length while decoupling`() {
        source = TestStopwatchClockWithRangeLimit().apply { maxTime = 0.5f }

        decouplingClock.changeSource(source)
        decouplingClock.allowDecoupling = true

        decouplingClock.start()

        assertTrue(decouplingClock.seek(-1f))
        decouplingClock.processFrame()

        assertEquals(0f, source.currentTime, 0.03f)
        assertFalse(source.isRunning)
        assertEquals(-1f, decouplingClock.currentTime, 0f)
        assertTrue(decouplingClock.isRunning)

        assertTrue(decouplingClock.seek(1f))
        decouplingClock.processFrame()

        assertEquals(0.5f, source.currentTime, 0f)
        assertFalse(source.isRunning)
        assertEquals(1f, decouplingClock.currentTime, 0.03f)
        assertTrue(decouplingClock.isRunning)
    }

    // In decoupled operation, seeking the source while it is not playing is undefined behavior.
    @Test
    fun `Test seek from source while decoupling`() {
        decouplingClock.allowDecoupling = true

        assertEquals(0f, source.currentTime, 0f)
        assertEquals(0f, decouplingClock.currentTime, 0f)

        source.seek(1f)

        assertEquals(1f, source.currentTime, 0f)
        // One might expect this to match the source, but with the current implementation, it does not.
        assertNotEquals(1f, decouplingClock.currentTime, 0f)

        // One should seek the decoupling clock directly.
        decouplingClock.seek(1f)
        decouplingClock.processFrame()

        assertEquals(1f, source.currentTime, 0f)
        assertEquals(1f, decouplingClock.currentTime, 0f)
    }

    @Test
    fun `Test backward playback over zero boundary`() {
        source = TestStopwatchClockWithRangeLimit()
        decouplingClock.changeSource(source)
        decouplingClock.allowDecoupling = true

        decouplingClock.seek(-0.3f)
        decouplingClock.rate = -1f
        decouplingClock.start()

        decouplingClock.processFrame()

        while (source.isRunning) {
            decouplingClock.processFrame()
            assertEquals(source.currentTime, decouplingClock.currentTime, 0.03f)
        }

        assertFalse(source.isRunning)

        var time = decouplingClock.currentTime

        while (decouplingClock.currentTime > -300) {
            assertFalse(source.isRunning)
            assertTrue(decouplingClock.currentTime <= time)
            time = decouplingClock.currentTime

            decouplingClock.processFrame()
        }
    }

    @Test
    fun `Test forward playback over zero boundary`() {
        source = TestStopwatchClockWithRangeLimit()
        decouplingClock.changeSource(source)
        decouplingClock.allowDecoupling = true

        decouplingClock.seek(-0.3f)
        decouplingClock.start()

        decouplingClock.processFrame()

        var time = decouplingClock.currentTime

        while (decouplingClock.currentTime < 0) {
            assertFalse(source.isRunning)
            assertTrue(decouplingClock.currentTime >= time)
            time = decouplingClock.currentTime

            decouplingClock.processFrame()
        }

        assertEquals(decouplingClock.currentTime, source.currentTime, 0.03f)
        assertTrue(source.isRunning)

        // Subsequently test stop/start works correctly.
        decouplingClock.stop()
        decouplingClock.processFrame()
        assertFalse(decouplingClock.isRunning)
        assertFalse(source.isRunning)

        decouplingClock.start()
        decouplingClock.processFrame()
        assertTrue(decouplingClock.isRunning)
        assertTrue(source.isRunning)
    }

    @Test
    fun `Test forward playback over length boundary`() {
        source = TestStopwatchClockWithRangeLimit().apply { maxTime = 10f }

        decouplingClock.changeSource(source)
        decouplingClock.allowDecoupling = true

        decouplingClock.seek(9.8f)
        decouplingClock.start()

        decouplingClock.processFrame()

        var time = decouplingClock.currentTime
        val tolerance = 0.03f

        // The decoupling clock generally lags behind the source clock, so we don't want the threshold here to go up to
        // the full tolerance, to avoid situations like so:
        //
        // x: decouplingClock
        // o: sourceClock
        //
        // ------x-----------o------>
        //    9980ms      10000ms
        //
        // The source clock has reached its playback limit and cannot seek further, so it will stop.
        // The decoupling clock hasn't caught up to the source clock yet, but it is close enough to pass the tolerance
        // check.
        //
        // Subtracting the tolerance ensures that both the decoupling and source clocks stay in the same 30ms band, but
        // neither stops yet.
        // We will assert that the source should eventually stop further down anyway.
        while (decouplingClock.currentTime < 10f - tolerance) {
            assertTrue(source.isRunning)
            assertEquals(decouplingClock.currentTime, source.currentTime, tolerance)
            assertTrue(decouplingClock.currentTime >= time)
            time = decouplingClock.currentTime

            decouplingClock.processFrame()
        }

        while (source.isRunning) {
            decouplingClock.processFrame()
        }

        assertFalse(source.isRunning)
        assertTrue(decouplingClock.currentTime <= 10.1f)

        while (decouplingClock.currentTime < 10.2f) {
            assertTrue(decouplingClock.isRunning)
            assertTrue(decouplingClock.currentTime >= time)
            time = decouplingClock.currentTime

            decouplingClock.processFrame()
        }

        assertFalse(source.isRunning)
    }

    @Test
    fun `Test play different source after seek failure`() {
        decouplingClock.allowDecoupling = true

        val firstSource = source as TestClockWithRange
        firstSource.maxTime = 0.1f

        decouplingClock.seek(1f)

        assertFalse(firstSource.isRunning)

        val secondSource = TestClockWithRange()

        decouplingClock.changeSource(secondSource)
        decouplingClock.start()

        assertTrue(secondSource.isRunning)
    }

    @Test
    fun `Test source change resets pending source restart`() {
        // Enter negative time to trigger negative seek source restart.
        decouplingClock.seek(-10f)
        decouplingClock.processFrame()

        val secondSource = TestClock()
        secondSource.currentTime = 0f
        decouplingClock.changeSource(secondSource)

        // If source restart is still true, the next processFrame will call secondSource.start().
        decouplingClock.processFrame()

        assertFalse("Second source should not have been started automatically", secondSource.isRunning)
    }
}

@RunWith(Parameterized::class)
class DecouplingFramedClockDecouplingNegativeTimeSeek(private val seekBeforeStart: Boolean) : BaseDecouplingFramedClockTest() {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "seekBeforeStart = {0}")
        fun data() = arrayOf(true, false)
    }

    @Test
    fun `Test start from negative time increments correctly`() {
        // Intentionally wait some time to allow the reference clock to build up some elapsed difference.
        // We want to make sure that this is not all applied at once causing a large jump.
        realTimeClock.currentTime += 0.5f

        decouplingClock.allowDecoupling = true

        if (seekBeforeStart) {
            decouplingClock.seek(-0.3f)
            decouplingClock.start()
        } else {
            decouplingClock.start()
            decouplingClock.seek(-0.3f)
        }

        decouplingClock.processFrame()

        assertFalse(source.isRunning)
        assertEquals(0f, source.currentTime, 0f)

        val time = decouplingClock.currentTime

        assertTrue(decouplingClock.isRunning)
        assertTrue(decouplingClock.currentTime < 0)

        realTimeClock.currentTime += 0.1f

        decouplingClock.processFrame()
        assertTrue(decouplingClock.currentTime < 0)
        assertTrue(decouplingClock.currentTime > time)
    }
}

@RunWith(Parameterized::class)
class DecouplingFramedClockDecouplingNoDriftTest(private val simulatedUpdateRate: Float) : BaseDecouplingFramedClockTest() {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "simulatedUpdateRate = {0} s")
        fun data() = arrayOf(0.0001f, 0.001f, 0.01f, 0.05f)
    }

    @Test
    fun `Test no decoupled drift`() {
        val stopwatch = StopwatchClock(source = realTimeClock::currentTimeLong)

        decouplingClock.start()
        stopwatch.start()

        decouplingClock.seek(-0.1f)
        stopwatch.seek(-0.1f)

        // Initialize lastReferenceTime
        decouplingClock.processFrame()

        while (decouplingClock.currentTime < 0) {
            realTimeClock.currentTime += simulatedUpdateRate

            decouplingClock.processFrame()
            assertEquals(stopwatch.currentTime, decouplingClock.currentTime, 1e-3f)
        }
    }
}

//endregion

private class TestClockWithRange : TestClock() {
    val minTime = 0f
    var maxTime = Float.POSITIVE_INFINITY

    override fun seek(position: Float): Boolean {
        if (position.coerceIn(minTime, maxTime) != position) {
            return false
        }

        return super.seek(position)
    }
}