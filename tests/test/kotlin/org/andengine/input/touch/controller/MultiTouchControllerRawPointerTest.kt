package org.andengine.input.touch.controller

import android.view.MotionEvent
import org.andengine.engine.options.TouchOptions
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MultiTouchControllerRawPointerTest {
    private val controller = MultiTouchController().apply {
        setTouchEventCallback { true }
        applyTouchOptions(TouchOptions().setUseRawPointer(true))
    }

    private val position = FloatArray(2)

    @Test
    fun `Raw pointer position is the latest dispatched position`() {
        dispatch(MotionEvent.ACTION_DOWN, 1f, 2f)
        dispatch(MotionEvent.ACTION_MOVE, 3f, 4f)

        assertTrue(controller.getRawPointerSurfacePosition(0, position))
        assertArrayEquals(floatArrayOf(3f, 4f), position, 0f)
    }

    @Test
    fun `Raw pointer position keeps negative and fractional coordinates`() {
        dispatch(MotionEvent.ACTION_DOWN, -12.5f, 1080.75f)

        assertTrue(controller.getRawPointerSurfacePosition(0, position))
        assertArrayEquals(floatArrayOf(-12.5f, 1080.75f), position, 0f)
    }

    @Test
    fun `Raw pointer position is cleared on reset`() {
        dispatch(MotionEvent.ACTION_DOWN, 1f, 2f)
        controller.resetRawPointers()

        assertTrue(controller.getRawPointerSurfacePosition(0, position))
        assertArrayEquals(floatArrayOf(0f, 0f), position, 0f)
    }

    @Test
    fun `Pointers outside of the capacity have no raw position`() {
        assertFalse(controller.getRawPointerSurfacePosition(-1, position))
        assertFalse(controller.getRawPointerSurfacePosition(controller.rawPointerCapacity, position))
    }

    private fun dispatch(action: Int, x: Float, y: Float) {
        val event = MotionEvent.obtain(100, 100, action, x, y, 0)
        controller.onHandleMotionEvent(event)
        event.recycle()
    }
}
