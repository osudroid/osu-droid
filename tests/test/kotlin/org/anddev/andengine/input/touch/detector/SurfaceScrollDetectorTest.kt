package org.anddev.andengine.input.touch.detector

import org.anddev.andengine.input.touch.TouchEvent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SurfaceScrollDetectorTest {
    @Test
    fun `Scrolls by the surface distance of touch events without a motion event`() {
        val scrolls = mutableListOf<Pair<Float, Float>>()
        val detector = SurfaceScrollDetector { _, _, distanceX, distanceY -> scrolls.add(distanceX to distanceY) }

        detector.onTouchEvent(sceneTouchEvent(TouchEvent.ACTION_DOWN, surfaceX = 10f, surfaceY = 10f))
        detector.onTouchEvent(sceneTouchEvent(TouchEvent.ACTION_MOVE, surfaceX = 40f, surfaceY = 10f))

        assertEquals(listOf(30f to 0f), scrolls)
    }

    /**
     * Obtains a [TouchEvent] at the given surface coordinates that was converted to scene coordinates unrelated to
     * them. Mimics what the engine does before passing it to a scene.
     */
    private fun sceneTouchEvent(action: Int, surfaceX: Float, surfaceY: Float) =
        TouchEvent.obtain(surfaceX, surfaceY, action, 0, 0L, null).apply { set(surfaceX * 7, surfaceY * 7) }
}
