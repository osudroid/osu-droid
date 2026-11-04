package org.andengine.graphics.shape

import android.opengl.GLES20
import org.andengine.graphics.BlendMode
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesDebugOptions
import org.andengine.graphics.gles.GlesGraphicsDevice
import org.andengine.graphics.gles.IDENTITY
import org.andengine.graphics.gles.RecordingGL
import org.andengine.graphics.gles.disposeShapeMasks
import org.andengine.graphics.gles.draws
import org.andengine.graphics.gles.installRecordingGL
import org.andengine.graphics.gles.installShapeMasks
import org.andengine.graphics.gles.lastVertices
import org.andengine.graphics.text.GlyphAtlas
import org.andengine.graphics.text.GlyphSlot
import org.andengine.graphics.text.FontManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.andengine.graphics.gles.endOpenPass

@RunWith(RobolectricTestRunner::class)
class ShapeBatchingTest {

    private lateinit var gl: RecordingGL
    private lateinit var atlas: GlyphAtlas
    private lateinit var masks: ShapeMasks
    private val blend = BlendMode.Alpha
    private val white = QuadBatcher.packColor(1f, 1f, 1f, 1f)
    private val red = QuadBatcher.packColor(1f, 0f, 0f, 1f)
    private val green = QuadBatcher.packColor(0f, 1f, 0f, 1f)
    private val blue = QuadBatcher.packColor(0f, 0f, 1f, 1f)

    private class Vertex(val x: Float, val y: Float, val u: Float, val v: Float, val color: Int)

    /** Flushes and returns the quads of the last upload as 4 vertices each. */
    private fun quads(): List<List<Vertex>> {
        QuadBatcher.flush()
        val bytes = gl.lastVertices()
        val stride = QuadBatcher.VERTEX_SIZE_BYTES
        return List(bytes.capacity() / (4 * stride)) { q ->
            List(4) { k ->
                val o = (q * 4 + k) * stride
                Vertex(bytes.getFloat(o), bytes.getFloat(o + 4), bytes.getFloat(o + 8), bytes.getFloat(o + 12), bytes.getInt(o + 16))
            }
        }
    }

    @Before
    fun setUp() {
        GlesDebugOptions.simulateEs30 = false
        FontManager.setRasterScale(1f)
        gl = installRecordingGL()
        GlesBackend.surfaceWidth = 800
        GlesBackend.surfaceHeight = 480
        atlas = GlyphAtlas(GlesGraphicsDevice.instance, 256, 2, 2)
        masks = installShapeMasks(atlas)
        QuadBatcher.setProjection(IDENTITY)
        gl.calls.clear()
    }

    @After
    fun tearDown() {
        QuadBatcher.releaseResources()
        endOpenPass()
        disposeShapeMasks(masks)
        GlesBackend.surfaceWidth = 0
        GlesBackend.surfaceHeight = 0
        FontManager.setRasterScale(1f)
    }

    @Test
    fun rect_oneQuadWithSolidUv() {
        ShapeBatching.rect(IDENTITY, 1f, 2f, 11f, 7f, red, blend)
        val q = quads()
        assertEquals(1, q.size)
        val solid = masks.solid()
        for (vtx in q[0]) {
            assertEquals(ShapeMasks.centerU(solid), vtx.u, 0f)
            assertEquals(ShapeMasks.centerV(solid), vtx.v, 0f)
            assertEquals(red, vtx.color)
        }
        assertEquals(1f, q[0][0].x, 0f)
        assertEquals(2f, q[0][0].y, 0f)
        assertEquals(11f, q[0][1].x, 0f)
        assertEquals(2f, q[0][1].y, 0f)
        assertEquals(11f, q[0][2].x, 0f)
        assertEquals(7f, q[0][2].y, 0f)
        assertEquals(1f, q[0][3].x, 0f)
        assertEquals(7f, q[0][3].y, 0f)
    }

    @Test
    fun gradientRect_colorPerCorner() {
        ShapeBatching.gradientRect(IDENTITY, 0f, 0f, 10f, 10f, red, green, blue, white, blend)
        val q = quads()
        assertEquals(1, q.size)
        assertEquals(listOf(red, green, white, blue), q[0].map { it.color })
    }

    @Test
    fun manyRadii_createBoundedMasks() {
        masks.dispose()
        atlas = GlyphAtlas(GlesGraphicsDevice.instance, 1024, 4, 2)
        masks = installShapeMasks(atlas)
        val slots = HashSet<GlyphSlot>()
        for (r in 100..400) {
            ShapeBatching.roundedRect(IDENTITY, 0f, 0f, 1000f, 1000f, r.toFloat(), white, blend)
            slots += masks.corner(ShapeMasks.quantize(r))
        }
        QuadBatcher.flush()
        // One mask per quantized radius: 100 to 128 in steps of 4 (8), then 144 to 400 in steps of 16 (17).
        assertEquals(25, slots.size)
    }

    @Test
    fun roundedRect_nineQuads() {
        ShapeBatching.roundedRect(IDENTITY, 0f, 0f, 40f, 20f, 5f, white, blend)
        assertEquals(9, quads().size)
    }

    @Test
    fun roundedRect_piecesShareEdges() {
        ShapeBatching.roundedRect(IDENTITY, 0f, 0f, 40f, 20f, 5f, white, blend)
        val q = quads()
        // Corners TL, TR, BL, BR, then top, bottom, left, right, center.
        fun box(i: Int) = floatArrayOf(q[i][0].x, q[i][0].y, q[i][2].x, q[i][2].y)
        val tl = box(0)
        val tr = box(1)
        val bl = box(2)
        val br = box(3)
        val top = box(4)
        val bottom = box(5)
        val left = box(6)
        val right = box(7)
        val center = box(8)
        assertEquals(tl[2], top[0], 0f)
        assertEquals(tr[0], top[2], 0f)
        assertEquals(bl[2], bottom[0], 0f)
        assertEquals(br[0], bottom[2], 0f)
        assertEquals(tl[3], left[1], 0f)
        assertEquals(bl[1], left[3], 0f)
        assertEquals(tr[3], right[1], 0f)
        assertEquals(br[1], right[3], 0f)
        assertEquals(left[2], center[0], 0f)
        assertEquals(right[0], center[2], 0f)
        assertEquals(top[3], center[1], 0f)
        assertEquals(bottom[1], center[3], 0f)
        var area = 0f
        for (i in 0 until 9) area += (box(i)[2] - box(i)[0]) * (box(i)[3] - box(i)[1])
        assertEquals(40f * 20f, area, 0.001f)
    }

    @Test
    fun roundedRect_clampsRadius() {
        ShapeBatching.roundedRect(IDENTITY, 0f, 0f, 40f, 20f, 50f, white, blend)
        val q = quads()
        assertEquals(10f, q[0][2].x - q[0][0].x, 0f)
        assertEquals(10f, q[0][2].y - q[0][0].y, 0f)
        // The 10 unit radius uses the 10 px mask: its UVs, not a mask made on demand.
        val mask = masks.corner(10)
        assertEquals(mask.u, q[0][0].u, 0f)
        assertEquals(ShapeMasks.innerU(mask), q[0][1].u, 0f)
    }

    @Test
    fun roundedRect_cornerUvsMirrored() {
        ShapeBatching.roundedRect(IDENTITY, 0f, 0f, 40f, 20f, 5f, white, blend)
        val q = quads()
        val m = masks.corner(5)
        val iu = ShapeMasks.innerU(m)
        val iv = ShapeMasks.innerV(m)
        assertEquals(listOf(m.u, iu, iu, m.u), q[0].map { it.u })
        assertEquals(listOf(m.v, m.v, iv, iv), q[0].map { it.v })
        assertEquals(listOf(iu, m.u, m.u, iu), q[1].map { it.u })
        assertEquals(listOf(m.v, m.v, iv, iv), q[1].map { it.v })
        assertEquals(listOf(m.u, iu, iu, m.u), q[2].map { it.u })
        assertEquals(listOf(iv, iv, m.v, m.v), q[2].map { it.v })
        assertEquals(listOf(iu, m.u, m.u, iu), q[3].map { it.u })
        assertEquals(listOf(iv, iv, m.v, m.v), q[3].map { it.v })
    }

    @Test
    fun roundedRect_maskSizeFollowsRasterScale() {
        FontManager.setRasterScale(2f)
        ShapeBatching.roundedRect(IDENTITY, 0f, 0f, 40f, 20f, 5f, white, blend)
        val q = quads()
        assertEquals(5f, q[0][2].x - q[0][0].x, 0f)
        val mask = masks.corner(10)
        assertEquals(mask.u, q[0][0].u, 0f)
        assertEquals(ShapeMasks.innerU(mask), q[0][1].u, 0f)
    }

    @Test
    fun roundedOutline_ringCornersAndEdges() {
        ShapeBatching.roundedOutline(IDENTITY, 0f, 0f, 40f, 20f, 6f, 2f, white, blend)
        val q = quads()
        assertEquals(8, q.size)
        val ring = masks.ring(6, 2)
        assertEquals(ring.u, q[0][0].u, 0f)
        assertEquals(ShapeMasks.innerU(ring), q[1][0].u, 0f)
        // Top edge between the corners, 2 high.
        assertEquals(6f, q[4][0].x, 0f)
        assertEquals(34f, q[4][1].x, 0f)
        assertEquals(2f, q[4][2].y - q[4][0].y, 0f)
        // Left edge, 2 wide.
        assertEquals(2f, q[6][1].x - q[6][0].x, 0f)
        assertEquals(6f, q[6][0].y, 0f)
        assertEquals(14f, q[6][2].y, 0f)
    }

    @Test
    fun roundedOutline_noRadius_fourEdgesWithoutOverlap() {
        ShapeBatching.roundedOutline(IDENTITY, 0f, 0f, 40f, 20f, 0f, 2f, white, blend)
        val q = quads()
        assertEquals(4, q.size)
        var area = 0f
        for (quad in q) area += (quad[2].x - quad[0].x) * (quad[2].y - quad[0].y)
        assertEquals(40f * 20f - 36f * 16f, area, 0.001f)
    }

    @Test
    fun line_widthPerpendicular() {
        ShapeBatching.line(IDENTITY, 0f, 0f, 10f, 0f, 4f, white, blend)
        val q = quads()[0]
        assertEquals(0f, q[0].x, 0f)
        assertEquals(2f, q[0].y, 0f)
        assertEquals(10f, q[1].x, 0f)
        assertEquals(2f, q[1].y, 0f)
        assertEquals(10f, q[2].x, 0f)
        assertEquals(-2f, q[2].y, 0f)
        assertEquals(0f, q[3].x, 0f)
        assertEquals(-2f, q[3].y, 0f)
    }

    @Test
    fun triangleOutline_threeExtendedLines() {
        ShapeBatching.triangleOutline(IDENTITY, 0f, 0f, 10f, 0f, 0f, 10f, 2f, white, blend)
        val q = quads()
        assertEquals(3, q.size)
        // The first edge runs along y = 0 from x = 0 to 10, extended by 1 at both ends.
        assertEquals(-1f, q[0][0].x, 1e-4f)
        assertEquals(11f, q[0][1].x, 1e-4f)
        assertEquals(1f, q[0][0].y, 1e-4f)
        assertEquals(-1f, q[0][2].y, 1e-4f)
    }

    @Test
    fun triangle_degenerateQuad() {
        ShapeBatching.triangle(IDENTITY, 0f, 0f, 10f, 0f, 5f, 8f, white, blend)
        val q = quads()[0]
        assertEquals(q[2].x, q[3].x, 0f)
        assertEquals(q[2].y, q[3].y, 0f)
        assertEquals(5f, q[2].x, 0f)
        assertEquals(ShapeMasks.centerU(masks.solid()), q[1].u, 0f)
    }

    @Test
    fun sector_triangleFanOverArcPoints() {
        ShapeBatching.sector(IDENTITY, 10f, 10f, 4f, 4f, 0f, 90f, 4, white, blend)
        val q = quads()
        assertEquals(3, q.size)
        for (quad in q) {
            assertEquals(10f, quad[0].x, 0f)
            assertEquals(10f, quad[0].y, 0f)
            assertEquals(quad[2].x, quad[3].x, 0f)
            assertEquals(quad[2].y, quad[3].y, 0f)
            assertEquals(ShapeMasks.centerU(masks.solid()), quad[1].u, 0f)
        }
        // 0 degrees points up, 90 degrees to the right.
        assertEquals(10f, q[0][1].x, 1e-4f)
        assertEquals(6f, q[0][1].y, 1e-4f)
        assertEquals(14f, q[2][2].x, 1e-4f)
        assertEquals(10f, q[2][2].y, 1e-4f)
        assertEquals(q[0][2].x, q[1][1].x, 0f)
    }

    @Test
    fun arc_stripBetweenInnerAndOuterRadius() {
        ShapeBatching.arc(IDENTITY, 10f, 10f, 4f, 3f, 0f, 90f, 3, 2f, white, blend)
        val q = quads()
        assertEquals(2, q.size)
        // First point straight up: inner radius 2 (ry 3 - 1), outer 4 (ry 3 + 1).
        assertEquals(10f, q[0][0].x, 1e-4f)
        assertEquals(8f, q[0][0].y, 1e-4f)
        assertEquals(10f, q[0][1].x, 1e-4f)
        assertEquals(6f, q[0][1].y, 1e-4f)
        // Last point to the right: inner x radius 3, outer 5.
        assertEquals(13f, q[1][3].x, 1e-4f)
        assertEquals(10f, q[1][3].y, 1e-4f)
        assertEquals(15f, q[1][2].x, 1e-4f)
        assertEquals(q[0][2].x, q[1][1].x, 0f)
    }

    @Test
    fun sectorAndArc_invisibleOrDegenerateAddNothing() {
        val clear = QuadBatcher.packColor(1f, 1f, 1f, 0f)
        ShapeBatching.sector(IDENTITY, 0f, 0f, 3f, 3f, 0f, 90f, 4, clear, blend)
        ShapeBatching.arc(IDENTITY, 0f, 0f, 3f, 3f, 0f, 90f, 4, 1f, clear, blend)
        ShapeBatching.sector(IDENTITY, 0f, 0f, 3f, 3f, 0f, 90f, 1, white, blend)
        ShapeBatching.arc(IDENTITY, 0f, 0f, 3f, 3f, 0f, 90f, 4, 0f, white, blend)
        assertFalse(QuadBatcher.hasPending)
    }

    @Test
    fun circle_fourQuarterMasks() {
        ShapeBatching.circle(IDENTITY, 10f, 10f, 4f, white, blend)
        val q = quads()
        assertEquals(4, q.size)
        val m = masks.corner(4)
        assertEquals(6f, q[0][0].x, 0f)
        assertEquals(10f, q[0][2].x, 0f)
        assertEquals(10f, q[3][0].x, 0f)
        assertEquals(14f, q[3][2].x, 0f)
        for (quad in q) {
            assertTrue(quad.all { it.u == m.u || it.u == ShapeMasks.innerU(m) })
        }
        ShapeBatching.ring(IDENTITY, 10f, 10f, 4f, 1f, white, blend)
        val rq = quads()
        assertEquals(4, rq.size)
        assertEquals(masks.ring(4, 1).u, rq[0][0].u, 0f)
    }

    @Test
    fun shapesAndText_shareOneDraw() {
        val glyph: GlyphSlot = atlas.allocate(8, 8)!!
        glyph.uploaded = true
        ShapeBatching.rect(IDENTITY, 0f, 0f, 10f, 10f, white, blend)
        QuadBatcher.addGlyph(glyph, blend, IDENTITY, 0f, 0f, 8f, 8f, white)
        ShapeBatching.rect(IDENTITY, 20f, 0f, 30f, 10f, white, blend)
        QuadBatcher.flush()
        assertEquals(gl.calls.toString(), 1, gl.draws())
        assertTrue(gl.calls.contains("glDrawElements(${GLES20.GL_TRIANGLES},18,${GLES20.GL_UNSIGNED_SHORT},0)"))
    }

    @Test
    fun addGlyphQuad_doesNotSnap() {
        QuadBatcher.setProjection(floatArrayOf(
            2f / 1280f, 0f, 0f, 0f,
            0f, -2f / 720f, 0f, 0f,
            0f, 0f, 1f, 0f,
            -1f, 1f, 0f, 1f
        ))
        GlesBackend.surfaceWidth = 2400
        GlesBackend.surfaceHeight = 1080
        ShapeBatching.rect(IDENTITY, 10.3f, 20.2f, 20.3f, 40.2f, white, blend)
        val q = quads()[0]
        assertEquals(10.3f, q[0].x, 0f)
        assertEquals(20.2f, q[0].y, 0f)
    }

    @Test
    fun addGlyphQuad_appliesMatrix() {
        val m = IDENTITY.copyOf().also { it[0] = 2f; it[5] = 3f; it[12] = 1f; it[13] = 1f }
        ShapeBatching.rect(m, 1f, 1f, 2f, 2f, white, blend)
        val q = quads()[0]
        assertEquals(3f, q[0].x, 0f)
        assertEquals(4f, q[0].y, 0f)
        assertEquals(5f, q[2].x, 0f)
        assertEquals(7f, q[2].y, 0f)
    }

    @Test
    fun zeroAlpha_addsNothing() {
        val clear = QuadBatcher.packColor(1f, 1f, 1f, 0f)
        ShapeBatching.rect(IDENTITY, 0f, 0f, 10f, 10f, clear, blend)
        ShapeBatching.roundedRect(IDENTITY, 0f, 0f, 10f, 10f, 2f, clear, blend)
        ShapeBatching.roundedOutline(IDENTITY, 0f, 0f, 10f, 10f, 2f, 1f, clear, blend)
        ShapeBatching.gradientRect(IDENTITY, 0f, 0f, 10f, 10f, clear, clear, clear, clear, blend)
        ShapeBatching.line(IDENTITY, 0f, 0f, 10f, 0f, 1f, clear, blend)
        ShapeBatching.triangle(IDENTITY, 0f, 0f, 1f, 0f, 0f, 1f, clear, blend)
        ShapeBatching.circle(IDENTITY, 0f, 0f, 3f, clear, blend)
        ShapeBatching.ring(IDENTITY, 0f, 0f, 3f, 1f, clear, blend)
        assertFalse(QuadBatcher.hasPending)
        QuadBatcher.flush()
        assertEquals(0, gl.draws())
    }
}
