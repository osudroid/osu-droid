package org.andengine.graphics.view

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VsyncWaiterTest {

    private class FakeSource : VsyncSource {
        var requests = 0
        var callback: VsyncSource.Callback? = null

        override fun requestNextVsync(callback: VsyncSource.Callback) {
            requests++
            this.callback = callback
        }
    }

    @Test
    fun waitForVsync_firstCall_returnsImmediatelyAndRequestsCallback() {
        val source = FakeSource()
        val waiter = VsyncWaiter(source, timeoutMillis = 1000)

        val start = System.nanoTime()
        waiter.waitForVsync()

        assertTrue(System.nanoTime() - start < 500_000_000L)
        assertEquals(1, source.requests)
    }

    @Test
    fun waitForVsync_returnsExpectedPresentationOfSignal() {
        val source = FakeSource()
        val waiter = VsyncWaiter(source, timeoutMillis = 1000)
        waiter.waitForVsync()

        source.callback!!.onVsync(10, 25)

        assertEquals(25L, waiter.waitForVsync())
        assertEquals(2, source.requests)
    }

    @Test
    fun waitForVsync_withoutSignal_timesOutAfterLimit() {
        val source = FakeSource()
        val waiter = VsyncWaiter(source, timeoutMillis = 20)
        waiter.waitForVsync()

        val start = System.nanoTime()
        waiter.waitForVsync()
        val elapsedMillis = (System.nanoTime() - start) / 1_000_000L

        assertTrue("elapsed $elapsedMillis", elapsedMillis in 20..199)
    }
}
