package com.reco1l.andengine.sprite

import android.opengl.GLES20
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.osudroid.testing.IDENTITY
import com.osudroid.testing.RecordingGl
import com.osudroid.testing.silentPlayer
import com.reco1l.andengine.texture.VideoTexture
import java.nio.ByteOrder
import org.andengine.engine.camera.Camera
import org.andengine.graphics.batch.MeshRenderer
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesLegacyTextureView
import org.andengine.opengl.texture.TextureManager
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

@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
class UIVideoSpriteTest {

    private lateinit var gl: RecordingGl
    private val drawContext = DrawContext()
    private val camera = Camera(0f, 0f, 100f, 100f)

    /**
     * A video texture without a decoder: [latch] records itself in the GL call log and hands out
     * [matrix] as the transform of the latched frame.
     */
    private inner class FakeVideo(id: Int, private val matrix: FloatArray) : VideoTexture("video.mp4", silentPlayer()) {
        var latches = 0

        init {
            mHardwareTextureID = id
        }

        override fun latch() {
            latches++
            gl.calls += "latch"
        }

        override fun getTransformMatrix(): FloatArray = matrix

        override fun getWidth() = 1
        override fun getHeight() = 1
    }

    private fun video(texture: VideoTexture): UIVideoSprite =
        UIVideoSprite(texture, TextureManager()).apply {
            scaleType = ScaleType.Stretch
            setSize(40f, 30f)
            alpha = 0.5f
            setColor(1f, 0.5f, 0.25f)
            onHandleInvalidations()
        }

    private fun programDraws() = gl.count("glDrawArrays")

    /** Applies the frame transform [st] to a texture coordinate: `(st * vec4(u, v, 0, 1)).xy`, column-major. */
    private fun transformed(st: FloatArray, u: Float, v: Float): Pair<Float, Float> {
        val input = floatArrayOf(u, v, 0f, 1f)
        val out = FloatArray(4)
        for (row in 0 until 4) {
            var sum = 0f
            for (col in 0 until 4) sum += st[col * 4 + row] * input[col]
            out[row] = sum
        }
        return out[0] to out[1]
    }

    @Before
    fun setUp() {
        gl = RecordingGl(EXTERNAL_ESSL3).install()
        GlesBackend.surfaceWidth = 100
        GlesBackend.surfaceHeight = 100
    }

    @After
    fun tearDown() {
        MeshRenderer.releaseResources()
        UIVideoSprite.releaseProgram()
        QuadBatcher.releaseResources()
        endOpenPass()
    }

    @Test
    fun videoSprite_drawsExternalTextureWithTransformedUvs() {
        // A decoder transform that scales, mixes and offsets both axes, so any axis mix-up shows.
        val st = floatArrayOf(
            0.5f, 0.25f, 0f, 0f,
            -0.125f, -1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0.1f, 0.9f, 0f, 1f
        )
        val sprite = video(FakeVideo(9, st))
        gl.calls.clear()

        sprite.doDraw(drawContext, camera)

        assertEquals(gl.calls.toString(), 1, programDraws())
        assertFalse(QuadBatcher.hasPending)
        assertTrue(gl.calls.toString(), gl.calls.contains("glBindTexture(${GlesLegacyTextureView.TEXTURE_EXTERNAL_OES},9)"))
        // Straight-alpha texture, so straight-alpha blending.
        assertTrue(gl.calls.toString(), gl.setsBlend(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA))

        // Sprite order top-left, bottom-left, top-right, bottom-right; the sprite flips V, then the
        // transform of the frame applies to those coordinates.
        val bytes = gl.lastProgramQuad()
        val stride = MeshRenderer.PROGRAM_VERTEX_SIZE_BYTES
        val corners = listOf(0f to 0f, 0f to 1f, 1f to 0f, 1f to 1f)
        for ((i, c) in corners.withIndex()) {
            assertEquals(c.first * 40f, bytes.getFloat(i * stride), 1e-4f)
            assertEquals(c.second * 30f, bytes.getFloat(i * stride + 4), 1e-4f)
            val (u, v) = transformed(st, c.first, 1f - c.second)
            assertEquals("u of vertex $i", u, bytes.getFloat(i * stride + 8), 1e-6f)
            assertEquals("v of vertex $i", v, bytes.getFloat(i * stride + 12), 1e-6f)
        }
        // Draw color, straight alpha.
        val color = bytes.getInt(24)
        val expected = QuadBatcher.packColor(1f, 0.5f, 0.25f, 0.5f)
        assertEquals(if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) expected else Integer.reverseBytes(expected), color)
    }

    @Test
    fun videoSprite_latchesBeforeDraw() {
        val texture = FakeVideo(9, IDENTITY)
        val sprite = video(texture)
        gl.calls.clear()

        sprite.doDraw(drawContext, camera)
        val firstLatch = gl.calls.indexOf("latch")
        val firstDraw = gl.calls.indexOfFirst { it.startsWith("glDrawArrays") }
        sprite.doDraw(drawContext, camera)

        assertTrue(gl.calls.toString(), firstLatch >= 0 && firstLatch < firstDraw)
        // Once per frame.
        assertEquals(2, texture.latches)
        assertEquals(2, programDraws())
    }

    @Test
    fun videoSprite_notDrawnBeforeReady() {
        val texture = FakeVideo(-1, IDENTITY)
        val sprite = video(texture)
        gl.calls.clear()

        sprite.doDraw(drawContext, camera)

        assertEquals(gl.calls.toString(), 0, programDraws())
        assertEquals(0, texture.latches)
        assertFalse(QuadBatcher.hasPending)
    }

    @Test
    fun isSupported_followsTheExtensionOfTheCurrentContext() {
        assertTrue(UIVideoSprite.isSupported())

        gl = RecordingGl("GL_OES_EGL_image_external GL_KHR_debug").install()
        assertFalse(UIVideoSprite.isSupported())

        gl = RecordingGl(EXTERNAL_ESSL3).install()
        assertTrue(UIVideoSprite.isSupported())
    }

    @Test
    fun videoSprite_notDrawnWithoutExternalTextureSupport() {
        gl = RecordingGl("").install()
        val texture = FakeVideo(9, IDENTITY)
        val sprite = video(texture)
        gl.calls.clear()

        sprite.doDraw(drawContext, camera)

        assertEquals(gl.calls.toString(), 0, programDraws())
        assertEquals(0, texture.latches)
        // The program that needs the extension is never built.
        assertTrue(gl.calls.toString(), gl.calls.none { it.startsWith("glShaderSource(") && it.contains("samplerExternalOES") })
    }

    private companion object {
        const val EXTERNAL_ESSL3 = "GL_OES_EGL_image_external GL_OES_EGL_image_external_essl3"
    }
}
