package com.osudroid.storyboard.renderer

import android.opengl.GLES20
import com.osudroid.storyboard.model.StoryboardLayerType
import com.osudroid.storyboard.model.StoryboardOrigin
import com.osudroid.storyboard.model.StoryboardSprite
import com.osudroid.storyboard.playback.PlayableSprite
import com.osudroid.testing.Page
import com.osudroid.testing.RecordingGl
import java.nio.ByteBuffer
import kotlin.math.PI
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
class StoryboardBatchTest {

    private lateinit var gl: RecordingGl
    private val drawContext = DrawContext()
    private val batch = StoryboardBatch()

    // u 0.125..0.625, v 0.125..0.625, 8 x 8 pixels.
    private fun region(page: Page = Page(7)) = TextureRegion(page, 2f, 2f, 8f, 8f)

    private fun sprite(x: Float = 0f, y: Float = 0f) =
        PlayableSprite(StoryboardSprite(StoryboardLayerType.Foreground, StoryboardOrigin.TopLeft, "a.png", x, y)).apply {
            this.x = x
            this.y = y
        }

    private fun ByteBuffer.x(corner: Int) = getFloat(corner * QuadBatcher.VERTEX_SIZE_BYTES)
    private fun ByteBuffer.y(corner: Int) = getFloat(corner * QuadBatcher.VERTEX_SIZE_BYTES + 4)
    private fun ByteBuffer.u(corner: Int) = getFloat(corner * QuadBatcher.VERTEX_SIZE_BYTES + 8)
    private fun ByteBuffer.v(corner: Int) = getFloat(corner * QuadBatcher.VERTEX_SIZE_BYTES + 12)
    private fun ByteBuffer.color(corner: Int) = getInt(corner * QuadBatcher.VERTEX_SIZE_BYTES + 16)

    private fun drawOne(sprite: PlayableSprite, region: TextureRegion = region(), alpha: Float = 1f): ByteBuffer {
        batch.begin(drawContext)
        batch.draw(sprite, region, alpha)
        QuadBatcher.flush()
        return gl.lastUpload()
    }

    @Before
    fun setUp() {
        gl = RecordingGl().install()
        GlesBackend.surfaceWidth = 100
        GlesBackend.surfaceHeight = 100
    }

    @After
    fun tearDown() {
        QuadBatcher.releaseResources()
        endOpenPass()
    }

    @Test
    fun storyboardQuads_goThroughBatch() {
        val page = Page(7)
        val region = region(page)

        batch.begin(drawContext)
        batch.drawQuad(region, 0f, 0f, 100f, 100f, 0f, 0f, 0f, 1f)
        batch.setSpace(5f, 5f, 2f)
        batch.draw(sprite(1f, 1f), region, 1f)
        batch.draw(sprite(10f, 10f), region, 1f)
        batch.resetSpace()
        batch.drawQuad(region, 0f, 0f, 100f, 100f, 0f, 0f, 0f, 0.5f)

        assertTrue(QuadBatcher.hasPending)
        assertFalse(gl.calls.toString(), gl.calls.any { it.startsWith("glDrawElements") || it.startsWith("glVertexAttribPointer") })

        QuadBatcher.flush()

        assertEquals(gl.calls.toString(), 1, gl.count("glDrawElements"))
        assertTrue(gl.calls.toString(), gl.calls.contains("glBindTexture(${GLES20.GL_TEXTURE_2D},7)"))
        assertEquals(4 * 4 * QuadBatcher.VERTEX_SIZE_BYTES, gl.vertexUploads.last().size)

        // The second quad is the first sprite, mapped by the storyboard space: 5 + 1 * 2 = 7, 8 * 2 wide.
        val vertices = gl.lastUpload()
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

        batch.begin(drawContext)
        batch.draw(sprite(), region, 1f)
        batch.draw(sprite().apply { additiveBlend = true }, region, 1f)
        QuadBatcher.flush()

        assertEquals(gl.calls.toString(), 2, gl.count("glDrawElements"))
        assertTrue(gl.calls.toString(), gl.setsBlend(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA))
        assertTrue(gl.calls.toString(), gl.setsBlend(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE))

        // The blend set up last before the second (additive) draw.
        val lastDraw = gl.calls.indexOfLast { it.startsWith("glDrawElements") }
        val additiveAt = gl.calls.subList(0, lastDraw).indexOfLast { it.startsWith("glBlendFunc") }
        assertTrue(gl.calls.toString(), additiveAt >= 0)
        assertTrue(
            gl.calls[additiveAt],
            gl.calls[additiveAt] == "glBlendFunc(${GLES20.GL_SRC_ALPHA},${GLES20.GL_ONE})" ||
                gl.calls[additiveAt] == "glBlendFuncSeparate(${GLES20.GL_SRC_ALPHA},${GLES20.GL_ONE},${GLES20.GL_SRC_ALPHA},${GLES20.GL_ONE})"
        )
    }

    @Test
    fun mirroredSprite_uvsSwapped() {
        val plain = drawOne(sprite(10f, 20f))
        // Corners: top-left, top-right, bottom-right, bottom-left.
        assertEquals(10f, plain.x(0), 0f)
        assertEquals(20f, plain.y(0), 0f)
        assertEquals(18f, plain.x(1), 0f)
        assertEquals(20f, plain.y(1), 0f)
        assertEquals(18f, plain.x(2), 0f)
        assertEquals(28f, plain.y(2), 0f)
        assertEquals(10f, plain.x(3), 0f)
        assertEquals(28f, plain.y(3), 0f)
        assertEquals(0.125f, plain.u(0), 0f)
        assertEquals(0.125f, plain.v(0), 0f)
        assertEquals(0.625f, plain.u(2), 0f)
        assertEquals(0.625f, plain.v(2), 0f)

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
            batch.begin(drawContext)
            batch.draw(sprite().apply { red = 1f; green = 1f; blue = 1f }, region(), 1f, 0.5f)
            QuadBatcher.flush()
            gl.lastUpload()
        }
        assertEquals(QuadBatcher.packColor(0.5f, 0.5f, 0.5f, 1f), dimmed.color(0))
    }
}
