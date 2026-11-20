package org.anddev.andengine.extension.input.touch.controller

import android.view.MotionEvent
import org.anddev.andengine.engine.options.TouchOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MultiTouchControllerTest {
    private data class ReceivedEvent(val x: Float, val y: Float, val eventTime: Long, val hasMotionEvent: Boolean)

    private val received = mutableListOf<ReceivedEvent>()

    private val controller = MultiTouchController().apply {
        setTouchEventCallback { event ->
            received.add(ReceivedEvent(event.x, event.y, event.eventTime, event.motionEvent != null))
            true
        }
    }

    @Test
    fun `Queued events carry their event time without a motion event when motion events are not retained`() {
        applyOptions(retainMotionEvent = false, processHistoricalEvents = false)

        dispatch(MotionEvent.obtain(100, 100, MotionEvent.ACTION_DOWN, 1f, 2f, 0))
        controller.onUpdate(0f)

        assertEquals(listOf(ReceivedEvent(1f, 2f, 100, false)), received)
    }

    @Test
    fun `Queued events carry their event time and a motion event when motion events are retained`() {
        applyOptions(retainMotionEvent = true, processHistoricalEvents = false)

        dispatch(MotionEvent.obtain(100, 100, MotionEvent.ACTION_DOWN, 1f, 2f, 0))
        controller.onUpdate(0f)

        assertEquals(listOf(ReceivedEvent(1f, 2f, 100, true)), received)
    }

    @Test
    fun `Historical events carry their own event time when motion events are not retained`() {
        applyOptions(retainMotionEvent = false, processHistoricalEvents = true)

        dispatch(moveWithHistory())
        controller.onUpdate(0f)

        assertEquals(
            listOf(
                ReceivedEvent(1f, 2f, 110, false),
                ReceivedEvent(3f, 4f, 120, false),
            ),
            received,
        )
    }

    @Test
    fun `Historical events carry their own event time when motion events are retained`() {
        applyOptions(retainMotionEvent = true, processHistoricalEvents = true)

        dispatch(moveWithHistory())
        controller.onUpdate(0f)

        assertEquals(
            listOf(
                ReceivedEvent(1f, 2f, 110, true),
                ReceivedEvent(3f, 4f, 120, true),
            ),
            received,
        )
    }

    @Test
    fun `Events handled during dispatch carry the dispatched motion event`() {
        val options = TouchOptions().setRunOnUpdateThread(false).setRetainMotionEvent(false)
        controller.applyTouchOptions(options)

        var motionEventDuringDispatch: MotionEvent? = null
        val event = MotionEvent.obtain(100, 100, MotionEvent.ACTION_DOWN, 1f, 2f, 0)

        controller.setTouchEventCallback {
            motionEventDuringDispatch = it.motionEvent
            true
        }

        controller.onHandleMotionEvent(event)

        assertNotNull(motionEventDuringDispatch)
        assertEquals(100L, motionEventDuringDispatch!!.eventTime)

        event.recycle()
    }

    @Test
    fun `Motion events are retained by default`() {
        assertEquals(true, TouchOptions().isRetainMotionEvent)
    }

    private fun applyOptions(retainMotionEvent: Boolean, processHistoricalEvents: Boolean) {
        controller.applyTouchOptions(
            TouchOptions()
                .setRunOnUpdateThread(true)
                .setRetainMotionEvent(retainMotionEvent)
                .setProcessHistoricalEvents(processHistoricalEvents)
        )
    }

    /**
     * Dispatches [event] like the UI thread would, then recycles it like the framework would after dispatching.
     */
    private fun dispatch(event: MotionEvent) {
        controller.onHandleMotionEvent(event)
        event.recycle()
    }

    /**
     * A move whose history holds a sample at (1, 2) at 110 ms, followed by the current sample at (3, 4) at 120 ms.
     */
    private fun moveWithHistory() = MotionEvent.obtain(100, 110, MotionEvent.ACTION_MOVE, 1f, 2f, 0).apply {
        addBatch(120, 3f, 4f, 1f, 1f, 0)
    }
}
