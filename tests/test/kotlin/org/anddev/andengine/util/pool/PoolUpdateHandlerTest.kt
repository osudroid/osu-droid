package org.anddev.andengine.util.pool

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PoolUpdateHandlerTest {
    private val handler = object : RunnablePoolUpdateHandler<ActionItem>() {
        override fun onAllocatePoolItem() = ActionItem()
    }

    @Test
    fun `Items are handled in the order they were posted`() {
        val handled = mutableListOf<Int>()

        repeat(5) { i -> post { handled.add(i) } }
        handler.onUpdate(0f)

        assertEquals(listOf(0, 1, 2, 3, 4), handled)
    }

    @Test
    fun `Items are handled once`() {
        var handledCount = 0

        post { handledCount++ }

        handler.onUpdate(0f)
        handler.onUpdate(0f)

        assertEquals(1, handledCount)
    }

    @Test
    fun `Item posted while items are being handled is handled on the next update`() {
        var lateItemHandledCount = 0

        post { post { lateItemHandledCount++ } }

        handler.onUpdate(0f)
        assertEquals(0, lateItemHandledCount)

        handler.onUpdate(0f)
        assertEquals(1, lateItemHandledCount)
    }

    @Test
    fun `Posting from another thread does not wait for items being handled`() {
        val handlingStarted = CountDownLatch(1)
        val releaseHandling = CountDownLatch(1)
        val posted = CountDownLatch(1)

        post {
            handlingStarted.countDown()
            releaseHandling.await(5, TimeUnit.SECONDS)
        }

        val updateThread = Thread { handler.onUpdate(0f) }
        updateThread.start()

        try {
            assertTrue("Handling did not start", handlingStarted.await(5, TimeUnit.SECONDS))

            Thread {
                post {}
                posted.countDown()
            }.start()

            assertTrue("Posting waited for the item being handled", posted.await(1, TimeUnit.SECONDS))
        } finally {
            releaseHandling.countDown()
            updateThread.join(5000)
        }
    }

    @Test
    fun `Items are not handled again after handling one of them throws`() {
        var handledCount = 0

        post { handledCount++ }
        post { throw IllegalStateException() }
        post { handledCount++ }

        assertThrows(IllegalStateException::class.java) { handler.onUpdate(0f) }
        assertEquals(1, handledCount)

        handler.onUpdate(0f)
        assertEquals(1, handledCount)
    }

    @Test
    fun `Reset discards posted items`() {
        var handledCount = 0

        post { handledCount++ }
        handler.reset()
        handler.onUpdate(0f)

        assertEquals(0, handledCount)
    }

    private fun post(action: () -> Unit) {
        val item = handler.obtainPoolItem()
        item.action = action
        handler.postPoolItem(item)
    }

    private class ActionItem : RunnablePoolItem() {
        var action: () -> Unit = {}

        override fun run() = action()
    }
}
