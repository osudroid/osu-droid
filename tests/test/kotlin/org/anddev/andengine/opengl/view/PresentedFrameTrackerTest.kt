package org.anddev.andengine.opengl.view

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresentedFrameTrackerTest {
    private val source = FakeFrameTimestampSource()
    private var now = 0L
    private val tracker = PresentedFrameTracker(source) { now }
    private val batch = PresentedFrameBatch()
    private val stats = PresentedFrameStats()

    @Test
    fun `No native calls while disabled`() {
        tracker.onSurfaceCreated()
        repeat(3) { tracker.onBeforeSwap() }

        assertEquals(0, source.startCount)
        assertEquals(0, source.queryCount)

        tracker.drainInto(batch)

        assertFalse(batch.available)
    }

    @Test
    fun `Enabling starts the source on the next frame with a warm-up`() {
        tracker.onSurfaceCreated()
        tracker.setEnabled(true)
        tracker.onBeforeSwap()

        assertEquals(1, source.startCount)
        assertEquals(1, source.queryCount)

        tracker.drainInto(batch)

        assertTrue(batch.wasReset)
        assertFalse(batch.available)
    }

    @Test
    fun `Presented frames are reported in order and make data available`() {
        enableAndSwap(3)
        source.results[1] = 100L
        source.results[2] = 200L

        tracker.onBeforeSwap()
        tracker.drainInto(batch)

        assertEquals(2, batch.count)
        assertEquals(100L, batch.presentTimes[0])
        assertEquals(200L, batch.presentTimes[1])
        assertTrue(batch.available)
    }

    @Test
    fun `Availability is reported once a frame is presented`() {
        enableAndSwap(2)

        assertFalse(tracker.isAvailable)

        source.results[1] = 100L
        tracker.onBeforeSwap()

        assertTrue(tracker.isAvailable)
    }

    @Test
    fun `Only an invalid present time counts as dropped`() {
        enableAndSwap(2)
        source.results[1] = FrameTimestampSource.RESULT_DROPPED
        source.results[2] = FrameTimestampSource.RESULT_UNKNOWN

        tracker.onBeforeSwap()
        tracker.drainStatsInto(stats)

        assertEquals(1, stats.dropped)
        assertEquals(1, stats.unknown)
    }

    @Test
    fun `Pending frames are bounded and overflow counts as unknown`() {
        enableAndSwap(PresentedFrameTracker.MAX_PENDING_FRAMES + 4)

        assertTrue(source.queryCounts.all { it <= PresentedFrameTracker.MAX_PENDING_FRAMES })

        tracker.drainStatsInto(stats)

        assertEquals(4, stats.unknown)
        assertEquals(0, stats.dropped)
    }

    @Test
    fun `No valid present time for a second gives up until the next trigger`() {
        enableAndSwap(1)

        now += 1_100_000_000L
        tracker.onBeforeSwap()

        assertEquals(1, source.stopCount)
        assertFalse(tracker.isActive)

        tracker.drainInto(batch)

        assertFalse(batch.available)

        val queries = source.queryCount
        repeat(5) { tracker.onBeforeSwap() }

        assertEquals(queries, source.queryCount)

        tracker.reset()
        tracker.onBeforeSwap()

        assertEquals(2, source.startCount)
        assertTrue(tracker.isActive)
    }

    @Test
    fun `Restart while active stops and starts the source again`() {
        enableAndSwap(3)
        tracker.drainInto(batch)

        tracker.reset()
        tracker.onBeforeSwap()

        assertEquals(1, source.stopCount)
        assertEquals(2, source.startCount)
        assertTrue(tracker.isActive)
        // The old pending frames are dropped.
        assertEquals(0, source.queryCounts.last())

        tracker.drainInto(batch)

        assertTrue(batch.wasReset)
    }

    @Test
    fun `A failed start is retried on the next trigger`() {
        source.startResult = false
        tracker.onSurfaceCreated()
        tracker.setEnabled(true)
        tracker.onBeforeSwap()

        assertEquals(1, source.startCount)

        source.startResult = true
        tracker.reset()
        tracker.onBeforeSwap()

        assertEquals(2, source.startCount)
        assertTrue(tracker.isActive)
    }

    @Test
    fun `A destroyed surface makes data unavailable`() {
        enableAndSwap(2)
        source.results[1] = 100L
        tracker.onBeforeSwap()
        tracker.drainInto(batch)

        assertTrue(batch.available)

        tracker.onSurfaceDestroyed()

        tracker.drainInto(batch)
        assertFalse(batch.available)
    }

    @Test
    fun `Unsupported source does not retry every frame`() {
        source.startResult = false

        tracker.onSurfaceCreated()
        tracker.setEnabled(true)
        repeat(5) { tracker.onBeforeSwap() }

        assertEquals(1, source.startCount)
        assertEquals(0, source.queryCount)

        tracker.drainInto(batch)

        assertFalse(batch.available)
    }

    @Test
    fun `Disabling stops the source and drops pending frames`() {
        enableAndSwap(3)

        tracker.setEnabled(false)
        tracker.onBeforeSwap()

        assertEquals(1, source.stopCount)
        assertFalse(tracker.isActive)
        tracker.drainInto(batch)
        assertFalse(batch.available)
    }

    @Test
    fun `The last request wins when toggled several times between frames`() {
        tracker.onSurfaceCreated()

        tracker.setEnabled(true)
        tracker.setEnabled(false)
        tracker.onBeforeSwap()
        assertEquals(0, source.startCount)

        tracker.setEnabled(false)
        tracker.setEnabled(true)
        tracker.onBeforeSwap()
        assertTrue(tracker.isActive)
        assertEquals(1, source.startCount)
    }

    @Test
    fun `A new surface restarts tracking`() {
        enableAndSwap(3)
        tracker.drainInto(batch)

        tracker.onSurfaceDestroyed()
        tracker.onSurfaceCreated()
        tracker.onBeforeSwap()

        // The old surface's frame IDs are not queried.
        assertEquals(0, source.queryCounts.last())
        // There is no stop() on a destroyed surface.
        assertEquals(0, source.stopCount)
        tracker.drainInto(batch)
        assertTrue(batch.wasReset)
    }

    @Test
    fun `Swapped frames are counted while enabled`() {
        enableAndSwap(5)

        tracker.drainStatsInto(stats)
        assertEquals(5, stats.swapped)
    }

    @Test
    fun `Presented frames are counted in the stats and drained stats are cleared`() {
        enableAndSwap(3)
        source.results[1] = 100L
        source.results[2] = 200L
        tracker.onBeforeSwap()

        tracker.drainStatsInto(stats)
        assertEquals(2, stats.presented)

        tracker.drainStatsInto(stats)
        assertEquals(0, stats.presented)
        assertEquals(0, stats.swapped)
    }

    @Test
    fun `Excess presented times discard the oldest and flag a restart`() {
        // The two oldest frames are evicted, leaving frames 3 to 18 pending.
        enableAndSwap(PresentedFrameTracker.MAX_PENDING_FRAMES + 2)
        tracker.drainInto(batch)

        // Frames 3 to 19 are presented over two frames without a drain in between, which is one more than a batch holds.
        for (id in 3L..12L) source.results[id] = id * 10
        tracker.onBeforeSwap()
        for (id in 13L..19L) source.results[id] = id * 10
        tracker.onBeforeSwap()
        tracker.drainInto(batch)

        assertEquals(PresentedFrameBatch.CAPACITY, batch.count)
        assertTrue(batch.wasReset)
        assertEquals(40L, batch.presentTimes[0])
        assertEquals(190L, batch.presentTimes[PresentedFrameBatch.CAPACITY - 1])
    }

    @Test
    fun `Drained batch is cleared`() {
        enableAndSwap(2)
        source.results[1] = 100L
        tracker.onBeforeSwap()
        tracker.drainInto(batch)

        tracker.drainInto(batch)

        assertEquals(0, batch.count)
        assertFalse(batch.wasReset)
        assertTrue(batch.available)
    }

    private fun enableAndSwap(frames: Int) {
        tracker.onSurfaceCreated()
        tracker.setEnabled(true)
        repeat(frames) { tracker.onBeforeSwap() }
    }

    private class FakeFrameTimestampSource : FrameTimestampSource {
        var startResult = true
        var startCount = 0
        var stopCount = 0
        var queryCount = 0
        val queryCounts = mutableListOf<Int>()

        /**
         * The result of each frame ID, which is pending unless set.
         */
        val results = mutableMapOf<Long, Long>()

        private var nextFrameId = 1L

        override fun start(): Boolean {
            startCount++
            return startResult
        }

        override fun stop() {
            stopCount++
        }

        override fun queryAndGetNextFrameId(frameIds: LongArray, count: Int, results: LongArray): Long {
            queryCount++
            queryCounts.add(count)

            for (i in 0 until count) {
                results[i] = this.results[frameIds[i]] ?: FrameTimestampSource.RESULT_PENDING
            }

            return nextFrameId++
        }
    }
}
