package com.osudroid.ui.v2.menu.main

import android.opengl.GLES20
import com.osudroid.testing.Page
import com.osudroid.testing.RecordingGl
import org.andengine.engine.camera.Camera
import org.andengine.graphics.batch.MeshRenderer
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.graphics.DrawContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.osudroid.testing.endOpenPass

@RunWith(RobolectricTestRunner::class)
class KiaiCatJamSpriteTest {

    private lateinit var gl: RecordingGl
    private val glState = DrawContext()
    private val camera = Camera(0f, 0f, 100f, 100f)

    private fun frames(page: Page) = arrayOf(
        TextureRegion(page, 0f, 0f, 4f, 4f),
        TextureRegion(page, 4f, 8f, 4f, 4f)
    )

    private fun cat(page: Page): KiaiCatJamSprite =
        KiaiCatJamSprite(frames(page)).apply {
            setSize(40f, 40f)
            alpha = 1f
            onHandleInvalidations()
        }

    @Before
    fun setUp() {
        gl = RecordingGl().install()
        GlesBackend.surfaceWidth = 100
        GlesBackend.surfaceHeight = 100
    }

    @After
    fun tearDown() {
        MeshRenderer.releaseResources()
        KiaiCatJamSprite.releaseProgram()
        QuadBatcher.releaseResources()
        endOpenPass()
    }

    @Test
    fun catJam_drawsThroughMeshProgramWithCircleUniforms() {
        val sprite = cat(Page(7)).apply { clipRadius = 0.4f }
        gl.calls.clear()

        sprite.doDraw(glState, camera)

        assertEquals(gl.calls.toString(), 1, gl.count("glDrawArrays"))
        assertFalse(QuadBatcher.hasPending)
        // The clip radius reaches the fragment shader.
        assertTrue(gl.calls.toString(), gl.calls.contains("glUniform1f(0,0.4)"))
        assertTrue(gl.calls.toString(), gl.calls.contains("glBindTexture(${GLES20.GL_TEXTURE_2D},7)"))
        // Premultiplied blending, as the fragment shader writes premultiplied color.
        assertTrue(gl.calls.toString(), gl.setsBlend(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA))

        // Full quad, local coordinates 0..1 across it, region UVs of the first frame.
        val bytes = gl.lastProgramQuad()
        val stride = MeshRenderer.PROGRAM_VERTEX_SIZE_BYTES
        val corners = listOf(0f to 0f, 0f to 1f, 1f to 0f, 1f to 1f)
        for ((i, c) in corners.withIndex()) {
            assertEquals(c.first * 40f, bytes.getFloat(i * stride), 1e-4f)
            assertEquals(c.second * 40f, bytes.getFloat(i * stride + 4), 1e-4f)
            assertEquals(c.first, bytes.getFloat(i * stride + 16), 0f)
            assertEquals(c.second, bytes.getFloat(i * stride + 20), 0f)
        }
    }

    @Test
    fun catJam_frameChangesUvs() {
        val sprite = cat(Page(7))
        sprite.doDraw(glState, camera)
        val first = gl.lastProgramQuad()
        val u0 = first.getFloat(8)
        val v0 = first.getFloat(12)
        val u1 = first.getFloat(2 * MeshRenderer.PROGRAM_VERTEX_SIZE_BYTES + 8)

        sprite.textureRegion = sprite.frames[1]
        sprite.onHandleInvalidations()
        sprite.doDraw(glState, camera)
        val second = gl.lastProgramQuad()

        assertEquals(0f, u0, 1e-6f)
        assertEquals(0f, v0, 1e-6f)
        assertEquals(4f / 16f, u1, 1e-6f)
        assertEquals(4f / 16f, second.getFloat(8), 1e-6f)
        assertEquals(8f / 16f, second.getFloat(12), 1e-6f)
        assertEquals(8f / 16f, second.getFloat(2 * MeshRenderer.PROGRAM_VERTEX_SIZE_BYTES + 8), 1e-6f)
        assertEquals(12f / 16f, second.getFloat(MeshRenderer.PROGRAM_VERTEX_SIZE_BYTES + 12), 1e-6f)
    }

    @Test
    fun catJam_notDrawnUntilTextureLoaded() {
        val sprite = cat(Page(-1))
        gl.calls.clear()

        sprite.doDraw(glState, camera)

        assertEquals(gl.calls.toString(), 0, gl.count("glDrawArrays"))
        assertFalse(QuadBatcher.hasPending)
    }
}
