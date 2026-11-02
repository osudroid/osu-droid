package com.osudroid.storyboard.renderer

import android.opengl.GLES20
import com.osudroid.storyboard.model.StoryboardLayerType
import com.osudroid.storyboard.model.StoryboardOrigin
import com.osudroid.storyboard.model.StoryboardSprite
import com.osudroid.storyboard.playback.PlayableSprite
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GL32
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesRenderContext
import org.andengine.opengl.texture.TextureManager
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
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

@RunWith(RobolectricTestRunner::class)
class StoryboardBatchTest {

    private val glCalls = ArrayList<String>()
    private val uploads = ArrayList<ByteArray>()
    private var nextId = 100
    private val glState = DrawContext()
    private val batch = StoryboardBatch()

    /**
     * Fake GL that records every call as `name(args)`, hands out ids, reports compile and link
     * success and keeps the bytes of every array buffer upload (the batch's vertices).
     */
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
        when (method.returnType) {
            java.lang.Boolean.TYPE -> false
            java.lang.Integer.TYPE -> if (name.startsWith("glCreate")) nextId++ else 0
            java.lang.Long.TYPE -> 0L
            java.lang.Float.TYPE -> 0f
            String::class.java -> if (args?.get(0) == GLES20.GL_VERSION) "OpenGL ES 3.2 test" else ""
            else -> null
        }
    } as GL32

    private class Page(id: Int) : BitmapTextureAtlas(TextureManager(), 16, 16, TextureOptions.BILINEAR) {
        init {
            mHardwareTextureID = id
        }
    }

    // u 0.125..0.625, v 0.125..0.625, 8 x 8 pixels.
    private fun region(page: Page = Page(7)) = TextureRegion(page, 2f, 2f, 8f, 8f)

    private fun sprite(x: Float = 0f, y: Float = 0f) =
        PlayableSprite(StoryboardSprite(StoryboardLayerType.Foreground, StoryboardOrigin.TopLeft, "a.png", x, y)).apply {
            this.x = x
            this.y = y
        }

    private fun batchDraws() = glCalls.count { it.startsWith("glDrawElements") }

    private fun setsBlend(src: Int, dst: Int) =
        glCalls.contains("glBlendFunc($src,$dst)") || glCalls.contains("glBlendFuncSeparate($src,$dst,$src,$dst)")

    private fun lastBatch(): ByteBuffer = ByteBuffer.wrap(uploads.last()).order(ByteOrder.nativeOrder())

    private fun ByteBuffer.x(corner: Int) = getFloat(corner * QuadBatcher.VERTEX_SIZE_BYTES)
    private fun ByteBuffer.y(corner: Int) = getFloat(corner * QuadBatcher.VERTEX_SIZE_BYTES + 4)
    private fun ByteBuffer.u(corner: Int) = getFloat(corner * QuadBatcher.VERTEX_SIZE_BYTES + 8)
    private fun ByteBuffer.v(corner: Int) = getFloat(corner * QuadBatcher.VERTEX_SIZE_BYTES + 12)
    private fun ByteBuffer.color(corner: Int) = getInt(corner * QuadBatcher.VERTEX_SIZE_BYTES + 16)

    private fun drawOne(sprite: PlayableSprite, region: TextureRegion = region(), alpha: Float = 1f): ByteBuffer {
        batch.begin(glState)
        batch.draw(sprite, region, alpha)
        batch.end()
        QuadBatcher.flush()
        return lastBatch()
    }

    @Before
    fun setUp() {
        GlesBackend.onContextCreatedForTest(recordingGl())
        GlesBackend.surfaceWidth = 100
        GlesBackend.surfaceHeight = 100
        glCalls.clear()
        uploads.clear()
    }

    @After
    fun tearDown() {
        QuadBatcher.releaseResources()
        val context = GlesRenderContext.instance
        if (context.isInPass) context.endPass()
    }

    @Test
    fun storyboardQuads_goThroughBatch() {
        val page = Page(7)
        val region = region(page)

        batch.begin(glState)
        batch.drawQuad(region, 0f, 0f, 100f, 100f, 0f, 0f, 0f, 1f)
        batch.setSpace(5f, 5f, 2f)
        batch.draw(sprite(1f, 1f), region, 1f)
        batch.draw(sprite(10f, 10f), region, 1f)
        batch.resetSpace()
        batch.drawQuad(region, 0f, 0f, 100f, 100f, 0f, 0f, 0f, 0.5f)
        batch.end()

        assertTrue(QuadBatcher.hasPending)
        assertFalse(glCalls.toString(), glCalls.any { it.startsWith("glDrawElements") || it.startsWith("glVertexAttribPointer") })

        QuadBatcher.flush()

        assertEquals(glCalls.toString(), 1, batchDraws())
        assertTrue(glCalls.toString(), glCalls.contains("glBindTexture(${GLES20.GL_TEXTURE_2D},7)"))
        assertEquals(4 * 4 * QuadBatcher.VERTEX_SIZE_BYTES, uploads.last().size)

        // The second quad is the first sprite, mapped by the storyboard space: 5 + 1 * 2 = 7, 8 * 2 wide.
        val vertices = lastBatch()
        val base = 4
        assertEquals(7f, vertices.x(base), 0f)
        assertEquals(7f, vertices.y(base), 0f)
        assertEquals(23f, vertices.x(base + 2), 0f)
        assertEquals(23f, vertices.y(base + 2), 0f)
        // Sprites carry the sprite layer.
        assertEquals(0xFFFF, vertices.getInt(base * QuadBatcher.VERTEX_SIZE_BYTES + 20) and 0xFFFF)
    }

    @Test
    fun additiveSprite_usesAdditiveBlend() {
        val region = region()

        batch.begin(glState)
        batch.draw(sprite(), region, 1f)
        batch.draw(sprite().apply { additiveBlend = true }, region, 1f)
        batch.end()
        QuadBatcher.flush()

        assertEquals(glCalls.toString(), 2, batchDraws())
        assertTrue(glCalls.toString(), setsBlend(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA))
        assertTrue(glCalls.toString(), setsBlend(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE))

        // The blend set up last before the second (additive) draw.
        val lastDraw = glCalls.indexOfLast { it.startsWith("glDrawElements") }
        val additiveAt = glCalls.subList(0, lastDraw).indexOfLast { it.startsWith("glBlendFunc") }
        assertTrue(glCalls.toString(), additiveAt >= 0)
        assertTrue(
            glCalls[additiveAt],
            glCalls[additiveAt] == "glBlendFunc(${GLES20.GL_SRC_ALPHA},${GLES20.GL_ONE})" ||
                glCalls[additiveAt] == "glBlendFuncSeparate(${GLES20.GL_SRC_ALPHA},${GLES20.GL_ONE},${GLES20.GL_SRC_ALPHA},${GLES20.GL_ONE})"
        )
    }

    @Test
    fun mirroredSprite_uvsSwapped() {
        val plain = drawOne(sprite(10f, 20f))
        // Corners: top-left, top-right, bottom-right, bottom-left.
        assertEquals(10f, plain.x(0), 0f); assertEquals(20f, plain.y(0), 0f)
        assertEquals(18f, plain.x(1), 0f); assertEquals(20f, plain.y(1), 0f)
        assertEquals(18f, plain.x(2), 0f); assertEquals(28f, plain.y(2), 0f)
        assertEquals(10f, plain.x(3), 0f); assertEquals(28f, plain.y(3), 0f)
        assertEquals(0.125f, plain.u(0), 0f); assertEquals(0.125f, plain.v(0), 0f)
        assertEquals(0.625f, plain.u(2), 0f); assertEquals(0.625f, plain.v(2), 0f)

        val flipped = drawOne(sprite(10f, 20f).apply { flipHorizontal = true; flipVertical = true })
        for (corner in 0 until 4) {
            assertEquals(plain.x(corner), flipped.x(corner), 0f)
            assertEquals(plain.y(corner), flipped.y(corner), 0f)
            assertEquals(plain.u((corner + 2) % 4), flipped.u(corner), 0f)
            assertEquals(plain.v((corner + 2) % 4), flipped.v(corner), 0f)
        }

        // A negative scale mirrors like a flip; together on one axis they cancel out.
        val negative = drawOne(sprite(10f, 20f).apply { scaleX = -1f })
        assertEquals(0.625f, negative.u(0), 0f)
        assertEquals(0.125f, negative.v(0), 0f)
        assertEquals(10f, negative.x(0), 0f)

        val cancelled = drawOne(sprite(10f, 20f).apply { scaleX = -1f; flipHorizontal = true })
        assertEquals(0.125f, cancelled.u(0), 0f)
    }

    @Test
    fun rotatedSprite_cornersRotated() {
        val vertices = drawOne(sprite(30f, 40f).apply { rotation = (PI / 2).toFloat() })

        // A quarter turn clockwise (y down) about the top-left origin at (30, 40).
        val expected = arrayOf(30f to 40f, 30f to 48f, 22f to 48f, 22f to 40f)
        for (corner in 0 until 4) {
            assertEquals("x of corner $corner", expected[corner].first, vertices.x(corner), 1e-4f)
            assertEquals("y of corner $corner", expected[corner].second, vertices.y(corner), 1e-4f)
        }
        assertEquals(0.125f, vertices.u(0), 0f)
        assertEquals(0.625f, vertices.u(1), 0f)
    }

    @Test
    fun colorStraightAlpha() {
        val vertices = drawOne(sprite().apply { red = 1f; green = 0.5f; blue = 0f; alpha = 0.5f }, alpha = 0.5f)

        // Not premultiplied: the color keeps its full value, only alpha carries the multipliers.
        val expected = QuadBatcher.packColor(1f, 0.5f, 0f, 0.25f)
        for (corner in 0 until 4) assertEquals(expected, vertices.color(corner))
        assertEquals(0xFF, vertices.color(0) and 0xFF)
        assertEquals(64, (vertices.color(0) ushr 24) and 0xFF)

        val dimmed = run {
            batch.begin(glState)
            batch.draw(sprite().apply { red = 1f; green = 1f; blue = 1f }, region(), 1f, 0.5f)
            QuadBatcher.flush()
            lastBatch()
        }
        assertEquals(QuadBatcher.packColor(0.5f, 0.5f, 0.5f, 1f), dimmed.color(0))
    }
}
