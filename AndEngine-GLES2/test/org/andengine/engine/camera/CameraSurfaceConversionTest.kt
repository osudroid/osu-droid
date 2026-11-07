package org.andengine.engine.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraSurfaceConversionTest {

    // A 200 x 100 scene shown on a 400 x 300 surface.
    private fun camera(rotation: Float = 0f) = Camera(0f, 0f, 200f, 100f).apply {
        setSurfaceSize(0, 0, 400, 300)
        this.rotation = rotation
    }

    @Test
    fun sceneToSurface_scalesAndInvertsTheYAxis() {
        val point = camera().convertSceneToSurfaceCoordinates(50f, 25f)

        assertEquals(100f, point[0], 1e-4f)
        assertEquals(225f, point[1], 1e-4f)
    }

    @Test
    fun sceneToSurface_keepsTheYAxisWhenAsked() {
        val point = camera().convertSceneToSurfaceCoordinates(50f, 25f, false)

        assertEquals(100f, point[0], 1e-4f)
        assertEquals(75f, point[1], 1e-4f)
    }

    @Test
    fun sceneToSurface_mirrorsForAHalfTurn() {
        val point = camera(180f).convertSceneToSurfaceCoordinates(50f, 25f, false)

        assertEquals(300f, point[0], 1e-4f)
        assertEquals(225f, point[1], 1e-4f)
    }

    @Test
    fun surfaceToScene_isTheInverseWithTheYAxisDown() {
        val camera = camera()
        val surface = camera.convertSceneToSurfaceCoordinates(floatArrayOf(50f, 25f), false)
        val scene = camera.convertSurfaceToSceneCoordinates(surface)

        assertEquals(50f, scene[0], 1e-4f)
        assertEquals(25f, scene[1], 1e-4f)
    }

    @Test
    fun surfaceToScene_mirrorsForAHalfTurn() {
        val scene = camera(180f).convertSurfaceToSceneCoordinates(300f, 225f)

        assertEquals(50f, scene[0], 1e-4f)
        assertEquals(25f, scene[1], 1e-4f)
    }

    @Test
    fun quarterTurn_roundTrips() {
        val camera = camera(90f)
        val surface = camera.convertSceneToSurfaceCoordinates(floatArrayOf(50f, 25f), false)
        val scene = camera.convertSurfaceToSceneCoordinates(surface)

        assertEquals(50f, scene[0], 1e-3f)
        assertEquals(25f, scene[1], 1e-3f)
    }
}
