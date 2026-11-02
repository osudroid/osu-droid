package com.reco1l.andengine.sprite

import android.opengl.GLES20
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.reco1l.andengine.texture.VideoTexture
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.andengine.engine.camera.Camera
import org.andengine.graphics.batch.MeshRenderer
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GL32
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesLegacyTextureView
import org.andengine.graphics.gles.GlesRenderContext
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

@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
class UIVideoSpriteTest {

    private val glCalls = ArrayList<String>()
    private val uploads = ArrayList<ByteArray>()
    private val sources = ArrayList<String>()
    private var nextId = 100
    private val glState = DrawContext()
    private val camera = Camera(0f, 0f, 100f, 100f)

    private fun recordingGl(): GL32 = Proxy.newProxyInstance(GL32::class.java.classLoader, arrayOf(GL32::class.java)) { _, method, args ->
        val name = method.name
        glCalls += "$name(${args?.joinToString(",") { if (it is IntArray || it is java.nio.Buffer) "buf" else "$it" } ?: ""})"
        if (name == "glGetIntegerv") {
            val params = args!![1] as IntArray
            params[args[2] as Int] = when (args[0]) {
                GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS -> 16
                GLES20.GL_MAX_TEXTURE_SIZE -> 4096
                else -> 0
            }
        } else if (name.startsWith("glGen") && args?.size == 3 && args[1] is IntArray) {
            val ids = args[1] as IntArray
            for (i in 0 until args[0] as Int) ids[args[2] as Int + i] = nextId++
        } else if ((name == "glGetShaderiv" || name == "glGetProgramiv") && args!![2] is IntArray) {
            (args[2] as IntArray)[args[3] as Int] = 1
        } else if (name == "glShaderSource") {
            sources += args!![1].toString()
        } else if (name == "glBufferData" && args!![0] == GLES20.GL_ARRAY_BUFFER && args[2] is ByteBuffer) {
            val copy = (args[2] as ByteBuffer).duplicate()
            val bytes = ByteArray(minOf(args[1] as Int, copy.remaining()))
            copy.get(bytes)
            uploads += bytes
        } else if (name == "glBufferSubData" && args!![3] is ByteBuffer) {
            val copy = (args[3] as ByteBuffer).duplicate()
            val bytes = ByteArray(minOf(args[2] as Int, copy.remaining()))
            copy.get(bytes)
            uploads += bytes
        }
        defaultValue(method.returnType, name.startsWith("glCreate"), args)
    } as GL32

    private fun defaultValue(type: Class<*>, create: Boolean, args: Array<Any?>?): Any? = when (type) {
        java.lang.Boolean.TYPE -> false
        java.lang.Integer.TYPE -> if (create) nextId++ else 0
        java.lang.Long.TYPE -> 0L
        java.lang.Float.TYPE -> 0f
        java.lang.Double.TYPE -> 0.0
        String::class.java -> if (args?.getOrNull(0) == GLES20.GL_VERSION) "OpenGL ES 3.2 test" else ""
        else -> null
    }

    /** A player that ignores every call; the tests never play anything. */
    private fun silentPlayer(): ExoPlayer =
        Proxy.newProxyInstance(ExoPlayer::class.java.classLoader, arrayOf(ExoPlayer::class.java)) { _, method, args ->
            defaultValue(method.returnType, false, args)
        } as ExoPlayer

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
            glCalls += "latch"
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

    private fun programDraws() = glCalls.count { it.startsWith("glDrawArrays") }

    private fun lastMesh(): ByteBuffer {
        val bytes = uploads.last { it.size == 4 * MeshRenderer.PROGRAM_VERTEX_SIZE_BYTES }
        return ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder())
    }

    /** The texture coordinate the old vertex shader produced: `(st * vec4(u, v, 0, 1)).xy`, column-major. */
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
        GlesBackend.onContextCreatedForTest(recordingGl())
        GlesBackend.surfaceWidth = 100
        GlesBackend.surfaceHeight = 100
        glCalls.clear()
    }

    @After
    fun tearDown() {
        MeshRenderer.releaseResources()
        UIVideoSprite.releaseProgram()
        QuadBatcher.releaseResources()
        val context = GlesRenderContext.instance
        if (context.isInPass) context.endPass()
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
        glCalls.clear()

        sprite.doDraw(glState, camera)

        assertEquals(glCalls.toString(), 1, programDraws())
        assertFalse(QuadBatcher.hasPending)
        assertTrue(glCalls.toString(), glCalls.contains("glBindTexture(${GlesLegacyTextureView.TEXTURE_EXTERNAL_OES},9)"))
        val fragment = sources.last { it.contains("samplerExternalOES") }
        assertTrue(fragment, fragment.contains("GL_OES_EGL_image_external_essl3"))
        // Straight-alpha texture, straight-alpha blending as the old shader path had.
        assertTrue(glCalls.toString(), glCalls.contains("glBlendFunc(${GLES20.GL_SRC_ALPHA},${GLES20.GL_ONE_MINUS_SRC_ALPHA})") ||
            glCalls.contains("glBlendFuncSeparate(${GLES20.GL_SRC_ALPHA},${GLES20.GL_ONE_MINUS_SRC_ALPHA},${GLES20.GL_SRC_ALPHA},${GLES20.GL_ONE_MINUS_SRC_ALPHA})"))

        // Sprite order top-left, bottom-left, top-right, bottom-right; the sprite flips V, then the
        // transform of the frame applies to those coordinates.
        val bytes = lastMesh()
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
        glCalls.clear()

        sprite.doDraw(glState, camera)
        val firstLatch = glCalls.indexOf("latch")
        val firstDraw = glCalls.indexOfFirst { it.startsWith("glDrawArrays") }
        sprite.doDraw(glState, camera)

        assertTrue(glCalls.toString(), firstLatch >= 0 && firstLatch < firstDraw)
        // Once per frame.
        assertEquals(2, texture.latches)
        assertEquals(2, programDraws())
    }

    @Test
    fun videoSprite_notDrawnBeforeReady() {
        val texture = FakeVideo(-1, IDENTITY)
        val sprite = video(texture)
        glCalls.clear()

        sprite.doDraw(glState, camera)

        assertEquals(glCalls.toString(), 0, programDraws())
        assertEquals(0, texture.latches)
        assertFalse(QuadBatcher.hasPending)
    }

    private companion object {
        val IDENTITY = floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)
    }
}
