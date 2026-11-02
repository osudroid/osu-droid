package org.andengine.entity.sprite

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.andengine.engine.camera.Camera
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.RecordingGL
import org.andengine.opengl.texture.TextureManager
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.opengl.util.GLState
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpriteBatchingTest {

    private lateinit var gl: RecordingGL
    private val glState = GLState()
    private val camera = Camera(0f, 0f, 100f, 100f)

    private class Page(id: Int, width: Int = 16, height: Int = 16) : BitmapTextureAtlas(TextureManager(), width, height, TextureOptions.BILINEAR) {
        init {
            mHardwareTextureID = id
        }
    }

    @Before
    fun setUp() {
        gl = RecordingGL()
        gl.intValues[GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS] = 16
        gl.intValues[GLES20.GL_MAX_TEXTURE_SIZE] = 4096
        GlesBackend.onContextCreatedForTest(gl)
        GlesBackend.surfaceWidth = 800
        GlesBackend.surfaceHeight = 480
        gl.calls.clear()
    }

    @After
    fun tearDown() {
        QuadBatcher.releaseResources()
    }

    private fun sprite(page: Page, x: Float = 0f): Sprite =
        Sprite(x, 0f, 8f, 8f, TextureRegion(page, 2f, 2f, 8f, 8f))

    private fun draws() = gl.calls.count { it.startsWith("glDrawElements") }
    private fun legacyDraws() = gl.calls.count { it.startsWith("glDrawArrays") }

    @Test
    fun engineSprite_goesThroughBatcher() {
        val s = sprite(Page(7), 10f)
        s.setColor(1f, 0f, 0f, 0.5f)
        s.onDraw(glState, camera)

        assertTrue(QuadBatcher.hasPending)
        assertEquals(0, legacyDraws())
        // Batching changes no GL state on its own.
        assertFalse(gl.calls.any { it.startsWith("glBindTexture") || it.startsWith("glBlendFunc") })

        QuadBatcher.flush()
        assertEquals(1, draws())

        val v = ByteBuffer.wrap(gl.vertexUploads.last()).order(ByteOrder.nativeOrder())
        // Corners in order around the quad: (x,y), (x,y2), (x2,y2), (x2,y); 24 bytes per vertex,
        // position at +0, UV at +8, color at +16, the sprite layer marker at +20.
        assertEquals(10f, v.getFloat(0), 0f)
        assertEquals(0f, v.getFloat(4), 0f)
        assertEquals(10f, v.getFloat(24), 0f)
        assertEquals(8f, v.getFloat(28), 0f)
        assertEquals(18f, v.getFloat(48), 0f)
        assertEquals(8f, v.getFloat(52), 0f)
        assertEquals(18f, v.getFloat(72), 0f)
        assertEquals(0f, v.getFloat(76), 0f)
        // UVs of the region (2..10 of 16): top-left and bottom-right.
        assertEquals(2f / 16f, v.getFloat(8), 1e-6f)
        assertEquals(2f / 16f, v.getFloat(12), 1e-6f)
        assertEquals(10f / 16f, v.getFloat(56), 1e-6f)
        assertEquals(10f / 16f, v.getFloat(60), 1e-6f)
        // Color bytes R, G, B, A.
        assertEquals(255, v.get(16).toInt() and 0xFF)
        assertEquals(0, v.get(17).toInt() and 0xFF)
        assertEquals(127, v.get(19).toInt() and 0xFF)
        for (corner in 0 until 4) {
            assertEquals(QuadBatcher.SPRITE_LAYER, v.getShort(corner * 24 + 20).toInt() and 0xFFFF)
        }
    }

    @Test
    fun blendFunction_followsTheSprite() {
        val s = sprite(Page(7))
        s.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)
        s.onDraw(glState, camera)
        QuadBatcher.flush()

        val expected = "glBlendFunc(${GLES20.GL_SRC_ALPHA},${GLES20.GL_ONE})"
        assertTrue(gl.calls.toString(), gl.calls.any { it == expected })
    }

    private fun region() = TextureRegion(Page(7), 2f, 2f, 8f, 8f)

    @Test
    fun spriteSubclassWithOwnDraw_stillBatched() {
        var drawn = 0
        val s = object : Sprite(0f, 0f, 8f, 8f, region()) {
            override fun preDraw(pGLState: GLState, pCamera: Camera) = super.preDraw(pGLState, pCamera)
            override fun draw(pGLState: GLState, pCamera: Camera) {
                drawn++
                super.draw(pGLState, pCamera)
            }
            override fun postDraw(pGLState: GLState, pCamera: Camera) = super.postDraw(pGLState, pCamera)
        }
        s.onDraw(glState, camera)

        assertEquals(1, drawn)
        assertTrue(QuadBatcher.hasPending)
        assertEquals(0, legacyDraws())
        QuadBatcher.flush()
        assertEquals(1, draws())
    }

    @Test
    fun subclassWithoutOverrides_stillBatches() {
        val s = object : Sprite(0f, 0f, 8f, 8f, region()) {}
        s.onDraw(glState, camera)

        assertTrue(QuadBatcher.hasPending)
        assertEquals(0, legacyDraws())
    }

    @Test
    fun sprite_unknownBlendFallsBackToAlpha() {
        val s = sprite(Page(7))
        s.setBlendFunction(GLES20.GL_SRC_ALPHA_SATURATE, GLES20.GL_ONE)
        s.onDraw(glState, camera)

        assertTrue(QuadBatcher.hasPending)
        QuadBatcher.flush()
        assertEquals(1, draws())
        assertEquals(0, legacyDraws())
        val expected = "glBlendFuncSeparate(${GLES20.GL_SRC_ALPHA},${GLES20.GL_ONE_MINUS_SRC_ALPHA},${GLES20.GL_ONE},${GLES20.GL_ONE_MINUS_SRC_ALPHA})"
        assertTrue(gl.calls.toString(), gl.calls.any { it == expected })
    }

    @Test
    fun sprite_colorReadAtDraw() {
        val s = sprite(Page(7))
        s.setColor(1f, 0f, 0f, 1f)
        s.onDraw(glState, camera)
        QuadBatcher.flush()
        var v = ByteBuffer.wrap(gl.vertexUploads.last()).order(ByteOrder.nativeOrder())
        assertEquals(255, v.get(16).toInt() and 0xFF)
        assertEquals(0, v.get(18).toInt() and 0xFF)
        assertEquals(255, v.get(19).toInt() and 0xFF)

        s.setColor(0f, 0f, 1f)
        s.alpha = 0.5f
        s.onDraw(glState, camera)
        QuadBatcher.flush()
        v = ByteBuffer.wrap(gl.vertexUploads.last()).order(ByteOrder.nativeOrder())
        for (corner in 0 until 4) {
            // The color sits 16 bytes into each vertex.
            val o = corner * QuadBatcher.VERTEX_SIZE_BYTES + 16
            assertEquals(0, v.get(o).toInt() and 0xFF)
            assertEquals(0, v.get(o + 1).toInt() and 0xFF)
            assertEquals(255, v.get(o + 2).toInt() and 0xFF)
            assertEquals(127, v.get(o + 3).toInt() and 0xFF)
        }
    }

    /**
     * Corners and UVs in batch order as the removed per-sprite vertex buffer produced them, for a
     * 12x6 sprite on the region (4, 8, 16, 8) of a 64x32 page, unrotated and rotated, in every
     * flip combination.
     */
    @Test
    fun sprite_flippedUvsMatchOldVbo() {
        val page = Page(7, 64, 32)
        val corners = floatArrayOf(0f, 0f, 0f, 6f, 12f, 6f, 12f, 0f)
        // (rotated, flipped horizontal, flipped vertical) to u,v per corner.
        val expected = mapOf(
            Triple(false, false, false) to floatArrayOf(0.0625f, 0.25f, 0.0625f, 0.5f, 0.3125f, 0.5f, 0.3125f, 0.25f),
            Triple(false, false, true) to floatArrayOf(0.0625f, 0.5f, 0.0625f, 0.25f, 0.3125f, 0.25f, 0.3125f, 0.5f),
            Triple(false, true, false) to floatArrayOf(0.3125f, 0.25f, 0.3125f, 0.5f, 0.0625f, 0.5f, 0.0625f, 0.25f),
            Triple(false, true, true) to floatArrayOf(0.3125f, 0.5f, 0.3125f, 0.25f, 0.0625f, 0.25f, 0.0625f, 0.5f),
            Triple(true, false, false) to floatArrayOf(0.1875f, 0.25f, 0.0625f, 0.25f, 0.0625f, 0.75f, 0.1875f, 0.75f),
            Triple(true, false, true) to floatArrayOf(0.1875f, 0.75f, 0.0625f, 0.75f, 0.0625f, 0.25f, 0.1875f, 0.25f),
            Triple(true, true, false) to floatArrayOf(0.0625f, 0.25f, 0.1875f, 0.25f, 0.1875f, 0.75f, 0.0625f, 0.75f),
            Triple(true, true, true) to floatArrayOf(0.0625f, 0.75f, 0.1875f, 0.75f, 0.1875f, 0.25f, 0.0625f, 0.25f),
        )
        for ((key, uvs) in expected) {
            val (rotated, h, v) = key
            val s = Sprite(0f, 0f, 12f, 6f, TextureRegion(page, 4f, 8f, 16f, 8f, rotated))
            s.setFlipped(h, v)
            assertArrayEquals("corners $key", corners, s.corners, 0f)
            assertArrayEquals("uvs $key", uvs, s.textureCoordinates, 0f)
            // Flipping back restores the unflipped coordinates.
            s.setFlipped(false, false)
            assertArrayEquals("unflipped $key", expected.getValue(Triple(rotated, false, false)), s.textureCoordinates, 0f)
        }

        // A size change moves the corners and keeps the coordinates.
        val s = Sprite(0f, 0f, 12f, 6f, TextureRegion(page, 4f, 8f, 16f, 8f))
        s.setSize(20f, 10f)
        assertArrayEquals(floatArrayOf(0f, 0f, 0f, 10f, 20f, 10f, 20f, 0f), s.corners, 0f)
        assertArrayEquals(expected.getValue(Triple(false, false, false)), s.textureCoordinates, 0f)
    }

    @Test
    fun blendingDisabled_replacesDestination() {
        val s = sprite(Page(7))
        s.isBlendingEnabled = false
        s.onDraw(glState, camera)
        QuadBatcher.flush()

        val expected = "glBlendFunc(${GLES20.GL_ONE},${GLES20.GL_ZERO})"
        assertTrue(gl.calls.toString(), gl.calls.any { it == expected })
    }

    @Test
    fun sameTexture_twoSprites_oneDraw() {
        val page = Page(7)
        sprite(page, 0f).onDraw(glState, camera)
        sprite(page, 20f).onDraw(glState, camera)
        QuadBatcher.flush()

        assertEquals(1, draws())
        assertEquals(0, legacyDraws())
    }
}
