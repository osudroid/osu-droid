package com.reco1l.andengine

import android.view.MotionEvent
import com.reco1l.andengine.container.*
import org.andengine.input.touch.TouchEvent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.*
import org.robolectric.*
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
class UIScrollableContainerTest {
    @Test
    fun `Slow drag processed in a single tick does not fling fast`() {
        val container = createContainer()

        // 2 px every 16 ms = 125 px/s.
        container.touch(TouchEvent.ACTION_DOWN, 1000, 50f, 50f)

        for (i in 1..20) {
            container.touch(TouchEvent.ACTION_MOVE, 1000L + i * 16, 50f, 50f + i * 2)
        }

        container.touch(TouchEvent.ACTION_UP, 1000L + 20 * 16, 50f, 50f + 20 * 2)

        assertEquals(125f, container.velocityY, 5f)
    }

    @Test
    fun `Samples with identical timestamps do not produce runaway velocity`() {
        val container = createContainer()

        container.touch(TouchEvent.ACTION_DOWN, 1000, 50f, 50f)
        container.touch(TouchEvent.ACTION_MOVE, 1000, 50f, 75f)
        container.touch(TouchEvent.ACTION_MOVE, 1000, 50f, 78f)

        assertTrue(container.velocityY.isFinite())
        assertTrue(abs(container.velocityY) < UIScrollableContainer.DEFAULT_MAX_VELOCITY)
    }

    @Test
    fun `Releasing after holding still does not fling`() {
        val container = createContainer()

        container.touch(TouchEvent.ACTION_DOWN, 1000, 50f, 50f)
        container.touch(TouchEvent.ACTION_MOVE, 1016, 50f, 80f)
        container.touch(TouchEvent.ACTION_MOVE, 1032, 50f, 110f)

        assertNotEquals(0f, container.velocityY)

        // No movement for 500 ms, then release.
        container.touch(TouchEvent.ACTION_UP, 1532, 50f, 110f)

        assertEquals(0f, container.velocityY)
        assertEquals(0f, container.velocityX)
    }

    @Test
    fun `Quick flick still flings`() {
        val container = createContainer()

        // 30 px every 8 ms = 3750 px/s, capped at the maximum velocity.
        container.touch(TouchEvent.ACTION_DOWN, 1000, 50f, 0f)
        container.touch(TouchEvent.ACTION_MOVE, 1008, 50f, 30f)
        container.touch(TouchEvent.ACTION_MOVE, 1016, 50f, 60f)
        container.touch(TouchEvent.ACTION_MOVE, 1024, 50f, 90f)
        container.touch(TouchEvent.ACTION_UP, 1024, 50f, 90f)

        assertEquals(UIScrollableContainer.DEFAULT_MAX_VELOCITY, container.velocityY, 0f)
    }

    @Test
    fun `Minimum travel is subtracted in the direction of travel`() {
        val container = createContainer()

        container.touch(TouchEvent.ACTION_DOWN, 1000, 50f, 50f)
        // Dragging 30 px upwards crosses the 20 px threshold by 10 px.
        container.touch(TouchEvent.ACTION_MOVE, 1100, 50f, 20f)

        assertEquals(510f, container.scrollY, 0.001f)
    }

    @Test
    fun `Minimum travel only moves the axis that crossed it`() {
        val container = createContainer()

        container.touch(TouchEvent.ACTION_DOWN, 1000, 50f, 50f)
        container.touch(TouchEvent.ACTION_MOVE, 1100, 80f, 55f)

        assertEquals(490f, container.scrollX, 0.001f)
        assertEquals(500f, container.scrollY, 0.001f)
    }

    private fun createContainer() = UIScrollableContainer().apply {
        width = 100f
        height = 100f

        +UIDummyComponent().apply {
            width = 2000f
            height = 2000f
        }

        onHandleInvalidations()

        scrollX = 500f
        scrollY = 500f
    }

    /**
     * Dispatches a [TouchEvent] to this [UIScrollableContainer] at [timeMillis].
     *
     * Every call in a test runs back-to-back, the same way queued [TouchEvent]s are drained within one update tick.
     */
    private fun UIScrollableContainer.touch(action: Int, timeMillis: Long, x: Float, y: Float) {
        val motionEvent = MotionEvent.obtain(0, timeMillis, action, x, y, 0)
        val touchEvent = TouchEvent.obtain(x, y, action, 0, motionEvent)

        onAreaTouched(touchEvent, x, y)

        touchEvent.recycle()
        motionEvent.recycle()
    }
}
