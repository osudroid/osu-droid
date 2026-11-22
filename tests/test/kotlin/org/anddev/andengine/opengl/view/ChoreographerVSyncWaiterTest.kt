package org.anddev.andengine.opengl.view

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChoreographerVSyncWaiterTest {
    @Test
    fun `First wait returns immediately and requests a callback`() {
        val source = FakeVSyncSource()
        val waiter = source.waiter(1000)

        val elapsedMillis = waiter.millisToWait()

        assertTrue("elapsed $elapsedMillis", elapsedMillis < 500)
        assertEquals(1, source.requestCount)
    }

    @Test
    fun `Wait returns before the timeout once the requested callback fires`() {
        val source = FakeVSyncSource()
        val waiter = source.waiter(1000)
        waiter.waitForVSync()

        source.signalOldest()

        val elapsedMillis = waiter.millisToWait()
        assertTrue("elapsed $elapsedMillis", elapsedMillis < 500)
        assertEquals(2, source.requestCount)
    }

    @Test
    fun `Wait gives up after the timeout if the callback never fires`() {
        val source = FakeVSyncSource()
        val waiter = source.waiter(20)
        waiter.waitForVSync()

        val elapsedMillis = waiter.millisToWait()

        assertTrue("elapsed $elapsedMillis", elapsedMillis in 20..199)
        assertEquals(2, source.requestCount)
    }

    @Test
    fun `Callback of a timed-out wait does not end the next wait early`() {
        val source = FakeVSyncSource()
        val waiter = source.waiter(300)
        waiter.waitForVSync()
        // Times out, as the first request is not answered in time.
        waiter.waitForVSync()

        // The first request is answered late, while the second one is still pending.
        source.signalOldest()

        val elapsedMillis = waiter.millisToWait()
        assertTrue("elapsed $elapsedMillis", elapsedMillis >= 300)
    }

    @Test
    fun `Wait after a timeout returns early once every pending callback has fired`() {
        val source = FakeVSyncSource()
        val waiter = source.waiter(300)
        waiter.waitForVSync()
        waiter.waitForVSync()

        // Callbacks requested before a vertical sync all fire on it.
        source.signalAll()

        val elapsedMillis = waiter.millisToWait()
        assertTrue("elapsed $elapsedMillis", elapsedMillis < 150)
    }

    @Test
    fun `Lost callback does not stall later waits`() {
        val source = FakeVSyncSource()
        val waiter = source.waiter(300)
        waiter.waitForVSync()
        waiter.waitForVSync()

        // The first request is never answered, but the second one is.
        source.loseOldest()
        source.signalAll()

        val elapsedMillis = waiter.millisToWait()
        assertTrue("elapsed $elapsedMillis", elapsedMillis < 150)

        // Later frames are paced normally again.
        source.signalAll()
        val nextElapsedMillis = waiter.millisToWait()
        assertTrue("elapsed $nextElapsedMillis", nextElapsedMillis < 150)
    }

    @Test
    fun `Quit releases the source`() {
        val source = FakeVSyncSource()

        source.waiter(1000).quit()

        assertEquals(1, source.quitCount)
    }

    private class FakeVSyncSource : ChoreographerVSyncWaiter.VSyncSource {
        private var callback: ChoreographerVSyncWaiter.VSyncSource.Callback? = null
        private val pending = ArrayDeque<Long>()
        var requestCount = 0
        var quitCount = 0

        override fun requestVSync(pCallback: ChoreographerVSyncWaiter.VSyncSource.Callback, pRequest: Long) {
            requestCount++
            callback = pCallback
            pending.addLast(pRequest)
        }

        override fun quit() {
            quitCount++
        }

        /**
         * Answers only the oldest pending request, like a late callback of a request that timed out.
         */
        fun signalOldest() = callback!!.onVSync(pending.removeFirst())

        /**
         * Drops the oldest pending request without answering it, like a callback that never fires.
         */
        fun loseOldest() {
            pending.removeFirst()
        }

        /**
         * Answers every pending request, like a vertical sync that runs every callback posted before it.
         */
        fun signalAll() {
            val latest = pending.lastOrNull() ?: return
            pending.clear()
            callback!!.onVSync(latest)
        }
    }

    private fun FakeVSyncSource.waiter(timeoutMillis: Long) =
        ChoreographerVSyncWaiter(this, TimeUnit.MILLISECONDS.toNanos(timeoutMillis))

    private fun ChoreographerVSyncWaiter.millisToWait(): Long {
        val start = System.nanoTime()
        waitForVSync()
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
    }
}
