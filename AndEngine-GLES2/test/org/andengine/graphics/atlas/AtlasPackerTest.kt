package org.andengine.graphics.atlas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class AtlasPackerTest {

    @Test
    fun place_firstAtOrigin() {
        val slot = AtlasPacker(256, 2).tryPlace(10, 20)!!
        assertEquals(2, slot.x)
        assertEquals(2, slot.y)
        assertEquals(10, slot.width)
        assertEquals(20, slot.height)
    }

    @Test
    fun place_noOverlap() {
        val p = 2
        val packer = AtlasPacker(1024, p)
        val rnd = Random(1234)
        val slots = ArrayList<AtlasSlot>()
        repeat(100) {
            slots += packer.tryPlace(8 + rnd.nextInt(57), 8 + rnd.nextInt(57))!!
        }
        var area = 0
        for (s in slots) {
            assertTrue(s.x >= p && s.y >= p)
            assertTrue(s.x + s.width + p <= 1024 && s.y + s.height + p <= 1024)
            area += (s.width + 2 * p) * (s.height + 2 * p)
        }
        assertEquals(area, packer.usedArea)
        for (i in slots.indices) for (j in i + 1 until slots.size) {
            val a = slots[i]
            val b = slots[j]
            val disjoint = a.x + a.width + p <= b.x - p || b.x + b.width + p <= a.x - p ||
                a.y + a.height + p <= b.y - p || b.y + b.height + p <= a.y - p
            assertTrue("overlap $i/$j", disjoint)
        }
    }

    @Test
    fun place_tooLarge_returnsNull() {
        val packer = AtlasPacker(64, 2)
        assertNull(packer.tryPlace(61, 10))
        assertNull(packer.tryPlace(10, 61))
        assertEquals(0, packer.usedArea)
        assertNotNull(packer.tryPlace(60, 60))
    }

    @Test
    fun place_fullPage_returnsNull() {
        val packer = AtlasPacker(64, 2)
        assertNotNull(packer.tryPlace(60, 60))
        assertNull(packer.tryPlace(1, 1))
    }

    @Test
    fun atlasPacker_extrudesEdges() {
        val src = intArrayOf(1, 2, 3, 4)
        val out = AtlasEdgeExtrusion.extrude(src, 2, 2, 2)
        assertEquals(36, out.size)
        val expected = arrayOf(
            intArrayOf(1, 1, 1, 2, 2, 2),
            intArrayOf(1, 1, 1, 2, 2, 2),
            intArrayOf(1, 1, 1, 2, 2, 2),
            intArrayOf(3, 3, 3, 4, 4, 4),
            intArrayOf(3, 3, 3, 4, 4, 4),
            intArrayOf(3, 3, 3, 4, 4, 4),
        )
        for (y in 0 until 6) for (x in 0 until 6) assertEquals("($x,$y)", expected[y][x], out[y * 6 + x])
    }
}
