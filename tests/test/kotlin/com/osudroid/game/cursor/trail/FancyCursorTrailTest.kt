package com.osudroid.game.cursor.trail

import android.opengl.GLES20
import com.osudroid.testing.RecordingGl
import org.andengine.engine.camera.Camera
import org.andengine.graphics.batch.MeshRenderer
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.DrawContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.osudroid.testing.endOpenPass

@RunWith(RobolectricTestRunner::class)
class FancyCursorTrailTest {

    private lateinit var gl: RecordingGl
    private val drawContext = DrawContext()
    private val camera = Camera(0f, 0f, 800f, 480f)

    @Before
    fun setUp() {
        gl = RecordingGl().install()
        GlesBackend.surfaceWidth = 0
        GlesBackend.surfaceHeight = 0
        drawContext.resetModelViewGLMatrixStack()
        drawContext.resetProjectionGLMatrixStack()
    }

    @After
    fun tearDown() {
        MeshRenderer.releaseResources()
        QuadBatcher.releaseResources()
        endOpenPass()
    }

    /** A trail with [points] control points; the cursor moves 10 units to the right per step. */
    private fun trail(points: Int): FancyCursorTrail {
        val trail = FancyCursorTrail()
        trail.cursorX = 100f
        trail.cursorY = 100f
        trail.update(0f)
        repeat(points) {
            trail.cursorX += 10f
            trail.update(0.001f)
        }
        return trail
    }

    @Test
    fun trail_drawsOneStrip() {
        trail(4).doDraw(drawContext, camera)

        assertEquals(1, gl.count("glDrawArrays"))
        // 4 control points make 3 segments of 2 vertices.
        val upload = gl.vertexUploads.last()
        assertEquals(6 * MeshRenderer.VERTEX_SIZE_BYTES, upload.size)
    }

    @Test
    fun trail_additiveBlend() {
        trail(3).doDraw(drawContext, camera)

        assertTrue(gl.calls.toString(), gl.setsBlend(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE))
    }

    @Test
    fun trail_perVertexAlpha() {
        val trail = trail(3)
        trail.update(0.1f)
        trail.doDraw(drawContext, camera)

        val upload = gl.lastUpload()
        // Alpha is the top byte of the packed color; older pieces fade first.
        val first = upload.get(MeshRenderer.VERTEX_SIZE_BYTES * 0 + 11).toInt() and 0xFF
        val last = upload.get(MeshRenderer.VERTEX_SIZE_BYTES * 3 + 11).toInt() and 0xFF
        assertTrue("first=$first last=$last", first < last)
        assertTrue(first in 1..254)
    }
}
