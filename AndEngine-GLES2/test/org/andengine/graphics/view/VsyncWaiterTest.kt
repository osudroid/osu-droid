package org.andengine.graphics.view

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VsyncWaiterTest {

    private class FakeSource : VsyncSource {
        var requests = 0
        var callback: VsyncSource.Callback? = null
        var request = 0L

        override fun requestNextVsync(callback: VsyncSource.Callback, request: Long) {
            requests++
            this.callback = callback
            this.request = request
        }

        fun signal(frameTimeNanos: Long) {
            callback!!.onVsync(request, frameTimeNanos)
        }
    }

    private fun millisToWait(waiter: VsyncWaiter): Long {
        val start = System.nanoTime()
        waiter.waitForVsync()
        return (System.nanoTime() - start) / 1_000_000L
    }

    @Test
    fun waitForVsync_firstCall_returnsImmediatelyAndRequestsCallback() {
        val source = FakeSource()
        val waiter = VsyncWaiter(source, timeoutMillis = 1000)

        assertTrue(millisToWait(waiter) < 500)
        assertEquals(1, source.requests)
    }

    @Test
    fun waitForVsync_afterSignal_returnsBeforeTimeout() {
        val source = FakeSource()
        val waiter = VsyncWaiter(source, timeoutMillis = 1000)
        waiter.waitForVsync()

        source.signal(10)

        val elapsedMillis = millisToWait(waiter)
        assertTrue("elapsed $elapsedMillis", elapsedMillis < 500)
        assertEquals(2, source.requests)
    }

    @Test
    fun waitForVsync_withoutSignal_timesOutAfterLimit() {
        val source = FakeSource()
        val waiter = VsyncWaiter(source, timeoutMillis = 20)
        waiter.waitForVsync()

        val elapsedMillis = millisToWait(waiter)

        assertTrue("elapsed $elapsedMillis", elapsedMillis in 20..199)
    }

    @Test
    fun lateSignalAfterTimeout_isIgnored() {
        val source = FakeSource()
        val waiter = VsyncWaiter(source, timeoutMillis = 300)
        waiter.waitForVsync()
        val timedOutRequest = source.request
        waiter.waitForVsync()

        // The signal of the request that timed out arrives while the waiter already waits for the next one.
        source.callback!!.onVsync(timedOutRequest, 10)

        val elapsedMillis = millisToWait(waiter)
        assertTrue("elapsed $elapsedMillis", elapsedMillis >= 300)

        // The signal of the current request still ends the wait early.
        source.signal(30)
        val signaledMillis = millisToWait(waiter)
        assertTrue("elapsed $signaledMillis", signaledMillis < 150)
    }
}
