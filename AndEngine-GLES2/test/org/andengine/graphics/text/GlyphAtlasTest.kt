package org.andengine.graphics.text

import android.opengl.GLES20
import android.opengl.GLES30
import java.nio.ByteBuffer
import org.andengine.graphics.gles.GlesGraphicsDevice
import org.andengine.graphics.gles.RecordingGL
import org.andengine.graphics.gles.installRecordingGL
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GlyphAtlasTest {

    private lateinit var gl: RecordingGL
    private val created = ArrayList<GlyphArray>()

    @Before
    fun setUp() {
        gl = installRecordingGL()
    }

    private fun atlas(layerSize: Int = 64, layers: Int = 2) = GlyphAtlas(GlesGraphicsDevice.instance, layerSize, layers, 2)

    private fun storageCalls() = gl.calls.count { it.startsWith("glTexStorage3D") }

    @After
    fun tearDown() {
        for (a in created) if (a.hasTexture) a.texture.dispose()
    }

    @Test
    fun allocate_firstSlotAtPadding() {
        val slot = atlas(1024, 4).allocate(10, 12)!!
        assertEquals(2, slot.x)
        assertEquals(2, slot.y)
        assertEquals(0, slot.layer)
        assertEquals(2 / 1024f, slot.u, 0f)
        assertEquals(2 / 1024f, slot.v, 0f)
        assertEquals(12 / 1024f, slot.u2, 0f)
        assertEquals(14 / 1024f, slot.v2, 0f)
    }

    @Test
    fun allocate_fillsNextLayerWhenFull() {
        val atlas = atlas()
        val a = atlas.allocate(60, 60)!!
        val b = atlas.allocate(60, 60)!!
        assertEquals(0, a.layer)
        assertEquals(1, b.layer)
        assertSame(a.array, b.array)
        assertEquals(1, atlas.arrayCount)
    }

    @Test
    fun full_array_addsNextArray() {
        val atlas = atlas()
        val a = atlas.allocate(60, 60)!!
        atlas.allocate(60, 60)!!
        val c = atlas.allocate(60, 60)!!
        assertNotSame(a.array, c.array)
        assertEquals(0, c.layer)
        assertEquals(2, atlas.arrayCount)

        // Allocating is CPU-only; only arrays that are used on the GPU get a texture.
        assertEquals(0, storageCalls())
        assertFalse(a.array.hasTexture)
        created += a.array
        assertSame(a.array.texture, a.array.texture)
        assertEquals(1, storageCalls())
        assertTrue(a.array.hasTexture)
        assertFalse(c.array.hasTexture)
    }

    @Test
    fun allocate_tooLarge_returnsNull() {
        val atlas = atlas()
        assertNull(atlas.allocate(61, 10))
        assertNull(atlas.allocate(10, 61))
        assertNull(atlas.allocate(0, 10))
        assertNull(atlas.allocate(10, -1))
        assertEquals(0, atlas.arrayCount)
        assertEquals(0, storageCalls())
    }

    @Test
    fun upload_writesR8IntoLayer() {
        val atlas = atlas()
        atlas.allocate(60, 60)!!
        val slot = atlas.allocate(5, 3)!!
        created += slot.array
        atlas.upload(slot, ByteBuffer.allocateDirect(15))
        val expected = "glTexSubImage3D(${GLES30.GL_TEXTURE_2D_ARRAY},0,${slot.x - 1},${slot.y - 1},${slot.layer},7,5,1,${GLES30.GL_RED},${GLES20.GL_UNSIGNED_BYTE},buf)"
        assertTrue(gl.calls.toString(), gl.calls.contains(expected))
    }

    @Test
    fun upload_writesZeroBorderAroundGlyph() {
        val atlas = atlas()
        atlas.allocate(60, 60)!!
        val slot = atlas.allocate(5, 3)!!
        created += slot.array
        val pixels = ByteBuffer.allocateDirect(15)
        for (i in 0 until 15) pixels.put(i, (i + 1).toByte())
        atlas.upload(slot, pixels)

        val expected = "glTexSubImage3D(${GLES30.GL_TEXTURE_2D_ARRAY},0,${slot.x - 1},${slot.y - 1},${slot.layer},7,5,1,${GLES30.GL_RED},${GLES20.GL_UNSIGNED_BYTE},buf)"
        assertTrue(gl.calls.toString(), gl.calls.contains(expected))
        val bytes = gl.texSubImage3DBytes.last()
        assertEquals(35, bytes.size)
        for (y in 0 until 5) for (x in 0 until 7) {
            val inside = x in 1..5 && y in 1..3
            val want = if (inside) ((y - 1) * 5 + (x - 1) + 1).toByte() else 0.toByte()
            assertEquals("texel $x,$y", want, bytes[y * 7 + x])
        }
    }

    @Test
    fun upload_marksSlotUploaded() {
        val atlas = atlas()
        val slot = atlas.allocate(5, 3)!!
        created += slot.array
        assertFalse(slot.uploaded)
        atlas.upload(slot, ByteBuffer.allocateDirect(15))
        assertTrue(slot.uploaded)
    }
}
