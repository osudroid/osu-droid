package org.andengine.graphics.view

import android.view.Choreographer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChoreographerVsyncSourceTest {
    private val looper = FakeFrameLooper()
    private val source = ChoreographerVsyncSource(looper)
    private val callback = RecordingCallback()

    @Test
    fun `Frame callback answers the request it was posted for`() {
        source.requestNextVsync(callback, 1)
        looper.runPosts()

        looper.runFrame()

        assertEquals(listOf(1L), callback.answered)
    }

    @Test
    fun `Late frame callback answers its own request while the next one is not posted yet`() {
        source.requestNextVsync(callback, 1)
        looper.runPosts()
        // The next request is made, but its post is still queued behind the late frame.
        source.requestNextVsync(callback, 2)

        looper.runFrame()
        looper.runPosts()
        looper.runFrame()

        assertEquals(listOf(1L, 2L), callback.answered)
    }

    @Test
    fun `Lost frame callback does not shift the answers of later ones`() {
        source.requestNextVsync(callback, 1)
        looper.runPosts()
        looper.loseOldestFrameCallback()

        source.requestNextVsync(callback, 2)
        looper.runPosts()
        looper.runFrame()

        source.requestNextVsync(callback, 3)
        looper.runPosts()
        looper.runFrame()

        assertEquals(listOf(2L, 3L), callback.answered)
    }

    @Test
    fun `Frame callbacks of one vsync all answer the latest posted request`() {
        source.requestNextVsync(callback, 1)
        looper.runPosts()
        source.requestNextVsync(callback, 2)
        looper.runPosts()

        looper.runFrame()

        assertEquals(listOf(2L, 2L), callback.answered)
    }

    @Test
    fun `Lost frame callback does not stall later waits`() {
        val waiter = VsyncWaiter(source, timeoutMillis = 300)
        waiter.waitForVsync()
        looper.runPosts()
        looper.loseOldestFrameCallback()

        // Times out, as the frame callback of the first request is lost.
        waiter.waitForVsync()
        looper.runPosts()
        looper.runFrame()

        val start = System.nanoTime()
        waiter.waitForVsync()
        val elapsedMillis = (System.nanoTime() - start) / 1_000_000L

        assertTrue("elapsed $elapsedMillis", elapsedMillis < 150)
    }

    @Test
    fun `Quit stops the looper`() {
        source.quit()

        assertEquals(1, looper.quitCount)
    }

    /**
     * Runs posted actions and frames only when told to, on the calling thread. Like the real looper thread, a frame
     * runs every frame callback posted before it.
     */
    private class FakeFrameLooper : FrameLooper {
        private val posts = ArrayDeque<Runnable>()
        private val frameCallbacks = ArrayDeque<Choreographer.FrameCallback>()
        var quitCount = 0

        override fun post(action: Runnable) {
            posts.addLast(action)
        }

        override fun postFrameCallback(callback: Choreographer.FrameCallback) {
            frameCallbacks.addLast(callback)
        }

        override fun quit() {
            quitCount++
        }

        /**
         * Runs every posted action, like the looper thread catching up on its queue.
         */
        fun runPosts() {
            while (posts.isNotEmpty()) {
                posts.removeFirst().run()
            }
        }

        /**
         * Runs every frame callback posted so far, like a vertical sync.
         */
        fun runFrame(frameTimeNanos: Long = 0L) {
            val due = frameCallbacks.toList()
            frameCallbacks.clear()
            due.forEach { it.doFrame(frameTimeNanos) }
        }

        /**
         * Drops the oldest frame callback without running it, like a callback that never fires.
         */
        fun loseOldestFrameCallback() {
            frameCallbacks.removeFirst()
        }
    }

    private class RecordingCallback : VsyncSource.Callback {
        val answered = mutableListOf<Long>()

        override fun onVsync(request: Long, frameTimeNanos: Long) {
            answered += request
        }
    }
}
