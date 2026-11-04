package org.andengine.graphics.batch

import android.opengl.GLES20
import java.nio.ByteBuffer
import org.andengine.graphics.BlendMode
import org.andengine.graphics.TextureFormat
import org.andengine.graphics.debug.RenderStats
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesDebugOptions
import org.andengine.graphics.gles.GlesGraphicsDevice
import org.andengine.graphics.gles.GlesTexture
import org.andengine.graphics.gles.IDENTITY
import org.andengine.graphics.gles.RecordingGL
import org.andengine.graphics.gles.draws
import org.andengine.graphics.gles.installRecordingGL
import org.andengine.graphics.gles.lastVertices
import org.andengine.graphics.text.GlyphAtlas
import org.andengine.graphics.text.GlyphSlot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.andengine.graphics.gles.endOpenPass

/** Quads are cut to the clip rectangle on the CPU, so clipped quads still share one draw. */
@RunWith(RobolectricTestRunner::class)
class QuadBatcherClipTest {

    private lateinit var gl: RecordingGL
    private lateinit var texture: GlesTexture
    private lateinit var textureB: GlesTexture
    private lateinit var atlas: GlyphAtlas
    private lateinit var slot: GlyphSlot

    /** World units are surface pixels, y grows downwards. */
    private val ortho = floatArrayOf(
        2f / 800f, 0f, 0f, 0f,
        0f, -2f / 480f, 0f, 0f,
        0f, 0f, 1f, 0f,
        -1f, 1f, 0f, 1f
    )

    private fun indexOfDraw() = gl.calls.indexOfFirst { it.startsWith("glDrawElements") }

    private fun ByteBuffer.x(vertex: Int) = getFloat(vertex * QuadBatcher.VERTEX_SIZE_BYTES)
    private fun ByteBuffer.y(vertex: Int) = getFloat(vertex * QuadBatcher.VERTEX_SIZE_BYTES + 4)
    private fun ByteBuffer.u(vertex: Int) = getFloat(vertex * QuadBatcher.VERTEX_SIZE_BYTES + 8)
    private fun ByteBuffer.v(vertex: Int) = getFloat(vertex * QuadBatcher.VERTEX_SIZE_BYTES + 12)
    private fun ByteBuffer.red(vertex: Int) = get(vertex * QuadBatcher.VERTEX_SIZE_BYTES + 16).toInt() and 0xFF
    private fun ByteBuffer.alpha(vertex: Int) = get(vertex * QuadBatcher.VERTEX_SIZE_BYTES + 19).toInt() and 0xFF
    private fun ByteBuffer.layer(vertex: Int) = getShort(vertex * QuadBatcher.VERTEX_SIZE_BYTES + 20).toInt() and 0xFFFF

    private fun assertNear(expected: List<Float>, actual: List<Float>) {
        assertEquals(actual.toString(), expected.size, actual.size)
        for (i in expected.indices) assertEquals(actual.toString(), expected[i], actual[i], 1e-3f)
    }

    private fun add(x0: Float, y0: Float, x1: Float, y1: Float, matrix: FloatArray = IDENTITY, tex: GlesTexture = texture) =
        QuadBatcher.add(tex, BlendMode.Alpha, matrix, x0, y0, x1, y1, 0f, 0f, 1f, 1f, -1)

    /** A square around ([cx], [cy]) turned by 45 degrees. */
    private fun rotated(cx: Float, cy: Float): FloatArray {
        val s = Math.sqrt(0.5).toFloat()
        return floatArrayOf(
            s, s, 0f, 0f,
            -s, s, 0f, 0f,
            0f, 0f, 1f, 0f,
            cx, cy, 0f, 1f
        )
    }

    /** The world rectangle x 100..300, y 100..200 (y down) as GL window pixels. */
    private fun clip() = QuadBatcher.setClip(100, 280, 200, 100)

    @Before
    fun setUp() {
        GlesDebugOptions.simulateEs30 = false
        gl = installRecordingGL()
        GlesBackend.surfaceWidth = 800
        GlesBackend.surfaceHeight = 480
        texture = GlesGraphicsDevice.instance.createTexture(4, 4, TextureFormat.RGBA8, label = "a") as GlesTexture
        textureB = GlesGraphicsDevice.instance.createTexture(4, 4, TextureFormat.RGBA8, label = "b") as GlesTexture
        atlas = GlyphAtlas(GlesGraphicsDevice.instance, 64, 2, 2)
        slot = atlas.allocate(40, 40)!!
        slot.uploaded = true
        QuadBatcher.setProjection(ortho)
        gl.calls.clear()
    }

    @After
    fun tearDown() {
        QuadBatcher.clearClip()
        QuadBatcher.releaseResources()
        endOpenPass()
        texture.dispose()
        textureB.dispose()
        if (slot.array.hasTexture) slot.array.texture.dispose()
        GlesBackend.surfaceWidth = 0
        GlesBackend.surfaceHeight = 0
        RenderStats.enabled = false
    }

    @Test
    fun quadOutsideClip_dropped() {
        clip()

        add(400f, 120f, 500f, 180f)
        add(150f, 300f, 250f, 400f)

        assertFalse(QuadBatcher.hasPending)
        assertTrue("no state change for a dropped quad: ${gl.calls}", gl.calls.isEmpty())
        QuadBatcher.flush()
        assertEquals(0, gl.draws())
    }

    @Test
    fun quadInsideClip_unchanged() {
        clip()

        add(150f, 120f, 250f, 180f)
        QuadBatcher.flush()

        assertEquals(1, gl.draws())
        val b = gl.lastVertices()
        assertNear(listOf(150f, 250f, 250f, 150f), (0 until 4).map { b.x(it) })
        assertNear(listOf(120f, 120f, 180f, 180f), (0 until 4).map { b.y(it) })
        assertNear(listOf(0f, 1f, 1f, 0f), (0 until 4).map { b.u(it) })
        assertNear(listOf(0f, 0f, 1f, 1f), (0 until 4).map { b.v(it) })
        assertFalse(gl.calls.toString(), gl.calls.contains("glEnable(${GLES20.GL_SCISSOR_TEST})"))
    }

    @Test
    fun partialQuad_positionsClamped() {
        clip()

        add(50f, 150f, 150f, 250f)
        // Through a matrix, overlapping the right and top edge.
        val shifted = IDENTITY.copyOf().also { it[12] = 250f; it[13] = 50f }
        add(0f, 0f, 100f, 100f, matrix = shifted)
        QuadBatcher.flush()

        assertEquals(1, gl.draws())
        val b = gl.lastVertices()
        assertNear(listOf(100f, 150f, 150f, 100f), (0 until 4).map { b.x(it) })
        assertNear(listOf(150f, 150f, 200f, 200f), (0 until 4).map { b.y(it) })
        assertNear(listOf(250f, 300f, 300f, 250f), (4 until 8).map { b.x(it) })
        assertNear(listOf(100f, 100f, 150f, 150f), (4 until 8).map { b.y(it) })
        assertFalse(gl.calls.toString(), gl.calls.contains("glEnable(${GLES20.GL_SCISSOR_TEST})"))
    }

    @Test
    fun partialQuad_uvsAndColorsInterpolated() {
        clip()

        // Pre-transformed corners, overlapping the left and bottom edge.
        QuadBatcher.addQuad(
            texture, BlendMode.Alpha,
            floatArrayOf(50f, 150f, 150f, 50f), floatArrayOf(150f, 150f, 250f, 250f),
            floatArrayOf(0f, 1f, 1f, 0f), floatArrayOf(0f, 0f, 1f, 1f),
            -1
        )
        // Red rises from 0 on the left to 255 on the right, alpha from 255 at the top to 51 below.
        val colors = intArrayOf(0xFF000000.toInt(), 0xFF0000FF.toInt(), 0x330000FF, 0x33000000)
        QuadBatcher.addGlyphQuad(
            slot, BlendMode.Alpha, IDENTITY,
            floatArrayOf(50f, 150f, 150f, 50f), floatArrayOf(150f, 150f, 250f, 250f),
            floatArrayOf(0f, 1f, 1f, 0f), floatArrayOf(0f, 0f, 1f, 1f),
            colors
        )
        QuadBatcher.flush()

        assertEquals(1, gl.draws())
        val b = gl.lastVertices()
        for (quad in 0 until 2) {
            val o = quad * 4
            assertNear(listOf(0.5f, 1f, 1f, 0.5f), (o until o + 4).map { b.u(it) })
            assertNear(listOf(0f, 0f, 0.5f, 0.5f), (o until o + 4).map { b.v(it) })
        }
        assertEquals(listOf(128, 255, 255, 128), (4 until 8).map { b.red(it) })
        assertEquals(listOf(255, 255, 153, 153), (4 until 8).map { b.alpha(it) })
    }

    @Test
    fun glyphQuad_clippedWithLayerKept() {
        clip()

        // 40 x 40 glyph from (280, 90): cut at the right edge (x 300) and the top edge (y 100).
        QuadBatcher.addGlyph(slot, BlendMode.Alpha, IDENTITY, 280f, 90f, 320f, 130f, -1)
        QuadBatcher.flush()

        assertEquals(1, gl.draws())
        val b = gl.lastVertices()
        assertNear(listOf(280f, 300f, 300f, 280f), (0 until 4).map { b.x(it) })
        assertNear(listOf(100f, 100f, 130f, 130f), (0 until 4).map { b.y(it) })
        val uMid = slot.u + (slot.u2 - slot.u) * 0.5f
        val vQuarter = slot.v + (slot.v2 - slot.v) * 0.25f
        assertNear(listOf(slot.u, uMid, uMid, slot.u), (0 until 4).map { b.u(it) })
        for (k in 0 until 2) assertEquals(vQuarter, b.v(k), 1e-6f)
        for (k in 2 until 4) assertEquals(slot.v2, b.v(k), 1e-6f)
        for (k in 0 until 4) assertEquals(slot.layer, b.layer(k))
    }

    @Test
    fun rotatedQuad_usesGlScissorKey() {
        clip()

        add(-50f, -50f, 50f, 50f, matrix = rotated(110f, 150f))
        QuadBatcher.flush()

        assertEquals(gl.calls.toString(), 1, gl.draws())
        val draw = indexOfDraw()
        val scissor = gl.calls.indexOf("glScissor(100,280,200,100)")
        val enable = gl.calls.indexOf("glEnable(${GLES20.GL_SCISSOR_TEST})")
        assertTrue(gl.calls.toString(), scissor in 0 until draw)
        assertTrue(gl.calls.toString(), enable in 0 until draw)
        // The corners are not moved; the GPU cuts them.
        val b = gl.lastVertices()
        assertEquals(110f, b.x(0) + b.x(2) - 110f, 1e-3f)
        assertTrue(b.x(3) < 100f)
    }

    @Test
    fun clipChange_doesNotFlushClippedQuads() {
        clip()
        add(50f, 150f, 150f, 250f)
        QuadBatcher.setClip(0, 0, 400, 400)
        add(350f, 150f, 450f, 250f)
        QuadBatcher.clearClip()
        add(500f, 300f, 600f, 400f)

        assertEquals(0, gl.draws())
        QuadBatcher.flush()
        assertEquals(gl.calls.toString(), 1, gl.draws())
        assertEquals(3 * 4 * QuadBatcher.VERTEX_SIZE_BYTES, gl.vertexUploads.last().size)
        val b = gl.lastVertices()
        // Second clip: x 0..400, world y 80..480.
        assertNear(listOf(350f, 400f, 400f, 350f), (4 until 8).map { b.x(it) })
        assertNear(listOf(600f, 300f), listOf(b.x(9), b.y(9)))
    }

    @Test
    fun glScissorChange_flushesOnlyDependentBatch() {
        val state = GlesBackend.state
        clip()
        add(50f, 150f, 150f, 250f)

        state.setScissorEnabled(true)
        state.scissor(1, 2, 3, 4)
        assertEquals("a CPU-clipped batch does not depend on the GL scissor", 0, gl.draws())
        assertTrue(QuadBatcher.hasPending)

        RenderStats.enabled = true
        RenderStats.reset()
        add(-50f, -50f, 50f, 50f, matrix = rotated(110f, 150f))
        assertEquals("the scissor key differs from the pending batch", 1, gl.draws())
        assertEquals(1, RenderStats.currentCount(QuadBatcher.flushCounterIndex(QuadBatcher.REASON_SCISSOR)))

        state.scissor(5, 6, 7, 8)
        assertEquals(2, gl.draws())
        val lastDraw = gl.calls.indexOfLast { it.startsWith("glDrawElements") }
        assertTrue(gl.calls.toString(), lastDraw < gl.calls.indexOf("glScissor(5,6,7,8)"))
        assertFalse(QuadBatcher.hasPending)
    }

    @Test
    fun ownDraw_disablesScissorAndRestores() {
        val state = GlesBackend.state
        state.setScissorEnabled(true)
        state.scissor(1, 2, 3, 4)
        gl.calls.clear()

        add(0f, 0f, 10f, 10f)
        assertEquals(0, gl.draws())
        QuadBatcher.flush()

        val draw = indexOfDraw()
        assertTrue(gl.calls.toString(), gl.calls.indexOf("glDisable(${GLES20.GL_SCISSOR_TEST})") in 0 until draw)
        assertTrue(gl.calls.toString(), gl.calls.lastIndexOf("glEnable(${GLES20.GL_SCISSOR_TEST})") > draw)
        assertTrue(state.isScissorEnabled)

        // A flush of the batch itself (another texture) restores the scissor as well.
        add(0f, 0f, 10f, 10f)
        add(0f, 0f, 10f, 10f, tex = textureB)
        assertTrue(state.isScissorEnabled)

        // A scissor-keyed draw sets its rectangle and puts the old one back.
        QuadBatcher.flush()
        clip()
        add(-50f, -50f, 50f, 50f, matrix = rotated(110f, 150f))
        gl.calls.clear()
        QuadBatcher.flush()
        val keyed = indexOfDraw()
        assertTrue(gl.calls.toString(), gl.calls.indexOf("glScissor(100,280,200,100)") in 0 until keyed)
        assertTrue(gl.calls.toString(), gl.calls.lastIndexOf("glScissor(1,2,3,4)") > keyed)
        assertTrue(state.isScissorEnabled)
    }

    @Test
    fun noSurfaceSize_fallsBackToScissorKey() {
        GlesBackend.surfaceWidth = 0
        GlesBackend.surfaceHeight = 0
        clip()

        add(50f, 150f, 150f, 250f)
        QuadBatcher.flush()

        assertEquals(1, gl.draws())
        assertTrue(gl.calls.toString(), gl.calls.indexOf("glScissor(100,280,200,100)") in 0 until indexOfDraw())
        assertEquals(50f, gl.lastVertices().x(0), 1e-3f)
    }
}
