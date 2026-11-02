package org.andengine.graphics.shape

import org.andengine.graphics.gles.GlesGraphicsDevice
import org.andengine.graphics.gles.RecordingGL
import org.andengine.graphics.gles.installRecordingGL
import org.andengine.graphics.text.GlyphAtlas
import org.andengine.graphics.text.GlyphSlot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShapeMasksTest {

    private lateinit var gl: RecordingGL
    private lateinit var atlas: GlyphAtlas
    private lateinit var masks: ShapeMasks
    private val used = ArrayList<GlyphSlot>()

    @Before
    fun setUp() {
        gl = installRecordingGL()
        atlas = GlyphAtlas(GlesGraphicsDevice.instance, 256, 2, 2)
        masks = ShapeMasks(atlas)
    }

    @After
    fun tearDown() {
        masks.dispose()
        for (array in used.map { it.array }.distinct()) if (array.hasTexture) array.texture.dispose()
    }

    private fun track(s: GlyphSlot): GlyphSlot {
        if (s !in used) used += s
        return s
    }

    private fun trackC(r: Int) = track(masks.corner(r))
    private fun trackR(r: Int, w: Int) = track(masks.ring(r, w))

    /** Last uploaded image without its 1 texel border, row-major. */
    private fun lastContent(w: Int, h: Int): ByteArray {
        val bytes = gl.texSubImage3DBytes.last()
        val out = ByteArray(w * h)
        for (y in 0 until h) for (x in 0 until w) out[y * w + x] = bytes[(y + 1) * (w + 2) + x + 1]
        return out
    }

    private fun at(b: ByteArray, w: Int, x: Int, y: Int) = b[y * w + x].toInt() and 0xFF

    @Test
    fun quantize_roundsUpInSteps() {
        assertEquals(7, ShapeMasks.quantize(7))
        assertEquals(32, ShapeMasks.quantize(32))
        assertEquals(36, ShapeMasks.quantize(33))
        assertEquals(128, ShapeMasks.quantize(128))
        assertEquals(144, ShapeMasks.quantize(130))
        assertEquals(512, ShapeMasks.quantize(600))
        assertEquals(1, ShapeMasks.quantize(0))
    }

    @Test
    fun solid_isFullyCovered() {
        val slot = track(masks.solid())
        for (v in lastContent(4, 4)) assertEquals(255, v.toInt() and 0xFF)
        val u = ShapeMasks.centerU(slot)
        val v = ShapeMasks.centerV(slot)
        assertTrue(u > slot.u && u < slot.u2)
        assertTrue(v > slot.v && v < slot.v2)
        assertTrue(slot.uploaded)
    }

    @Test
    fun corner_coverageFollowsCircle() {
        trackC(8)
        val c = lastContent(9, 9)
        assertEquals(255, at(c, 9, 7, 7))
        assertEquals(0, at(c, 9, 0, 0))
        // Texel (2,2): center (2.5,2.5), distance 7.78 from (8,8) -> coverage 0.72.
        val rim = at(c, 9, 2, 2)
        assertTrue("rim $rim", rim in 64..192)
    }

    @Test
    fun ring_isHollow() {
        trackR(8, 2)
        val c = lastContent(9, 9)
        // Near the circle center (inner radius 6): hollow.
        assertEquals(0, at(c, 9, 7, 7))
        assertEquals(0, at(c, 9, 6, 6))
        // Texel (7,1): center (7.5,1.5), distance 6.52 -> fully inside the ring.
        assertEquals(255, at(c, 9, 7, 1))
        assertEquals(0, at(c, 9, 0, 0))
    }

    @Test
    fun cornerAndRing_extraColumnAndRowRepeatLastReal() {
        for (build in listOf({ trackC(8) }, { trackR(8, 2) })) {
            build()
            val c = lastContent(9, 9)
            for (i in 0 until 9) {
                assertEquals(at(c, 9, 7, i.coerceAtMost(7)), at(c, 9, 8, i.coerceAtMost(7)))
                assertEquals(at(c, 9, i.coerceAtMost(7), 7), at(c, 9, i.coerceAtMost(7), 8))
            }
            assertEquals(at(c, 9, 7, 7), at(c, 9, 8, 8))
        }
    }

    @Test
    fun innerUv_isCircleCenterEdge() {
        val slot = trackC(8)
        assertEquals(slot.u + (slot.u2 - slot.u) * 8f / 9f, ShapeMasks.innerU(slot), 1e-6f)
        assertEquals(slot.v + (slot.v2 - slot.v) * 8f / 9f, ShapeMasks.innerV(slot), 1e-6f)
    }

    @Test
    fun sameKey_returnsSameSlot() {
        assertSame(trackC(6), trackC(6))
        assertSame(track(masks.solid()), track(masks.solid()))
        assertSame(trackR(6, 2), trackR(6, 2))
        assertFalse(trackC(6) === trackC(7))
    }

    @Test
    fun contextLoss_reuploadsMasksInPlace() {
        val a = track(masks.solid())
        val b = trackC(5)
        val count = gl.texSubImage3DBytes.size
        masks.onContextLost()
        assertFalse(a.uploaded)
        assertFalse(b.uploaded)
        masks.restore()
        assertTrue(a.uploaded)
        assertTrue(b.uploaded)
        assertEquals(count + 2, gl.texSubImage3DBytes.size)
        assertEquals(1, atlas.arrayCount)
        assertSame(a, track(masks.solid()))
        assertSame(b, trackC(5))
    }

    @Test
    fun hugeRadius_isClampedNotThrown() {
        val slot = trackC(100000)
        assertEquals(atlas.maxImageSize, slot.width)
        assertSame(slot, trackC(atlas.maxImageSize))
        assertEquals(atlas.maxImageSize, trackR(100000, 100000).width)
    }
}
