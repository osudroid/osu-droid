package org.andengine.graphics.batch

import android.opengl.GLES20
import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.andengine.graphics.BlendMode
import org.andengine.graphics.TextureFormat
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesDebugOptions
import org.andengine.graphics.gles.GlesGraphicsDevice
import org.andengine.graphics.gles.GlesRenderContext
import org.andengine.graphics.gles.GlesTexture
import org.andengine.graphics.gles.RecordingGL
import org.andengine.graphics.text.GlyphAtlas
import org.andengine.graphics.text.GlyphSlot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuadBatcherGlyphTest {

    private lateinit var gl: RecordingGL
    private lateinit var atlas: GlyphAtlas
    private lateinit var sprite: GlesTexture
    private val slots = ArrayList<GlyphSlot>()

    private val identity = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    private fun draws() = gl.calls.count { it.startsWith("glDrawElements") }

    private fun lastVertices(): ByteBuffer =
        ByteBuffer.wrap(gl.vertexUploads.last()).order(ByteOrder.nativeOrder())

    private fun glyph(slot: GlyphSlot, matrix: FloatArray = identity, x0: Float = 0f, y0: Float = 0f, x1: Float = 4f, y1: Float = 4f) =
        QuadBatcher.addGlyph(slot, BlendMode.Alpha, matrix, x0, y0, x1, y1, -1)

    private fun sprite() =
        QuadBatcher.add(sprite, BlendMode.Alpha, identity, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, -1)

    @Before
    fun setUp() {
        GlesDebugOptions.simulateEs30 = false
        gl = RecordingGL()
        gl.intValues[GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS] = 16
        gl.intValues[GLES20.GL_MAX_TEXTURE_SIZE] = 4096
        GlesBackend.onContextCreatedForTest(gl)
        GlesBackend.surfaceWidth = 800
        GlesBackend.surfaceHeight = 480
        atlas = GlyphAtlas(GlesGraphicsDevice.instance, 64, 2, 2)
        sprite = GlesGraphicsDevice.instance.createTexture(4, 4, TextureFormat.RGBA8, label = "sprite") as GlesTexture
        slots += atlas.allocate(8, 8)!!
        slots += atlas.allocate(60, 60)!!
        for (s in slots) s.uploaded = true
        QuadBatcher.setProjection(identity)
        gl.calls.clear()
    }

    @After
    fun tearDown() {
        QuadBatcher.releaseResources()
        val context = GlesRenderContext.instance
        if (context.isInPass) context.endPass()
        sprite.dispose()
        for (s in slots) if (s.array.hasTexture) s.array.texture.dispose()
        GlesBackend.surfaceWidth = 0
        GlesBackend.surfaceHeight = 0
    }

    @Test
    fun addGlyph_usesGlyphProgramAndArrayTarget() {
        glyph(slots[0])
        assertEquals(0, draws())
        QuadBatcher.flush()

        assertEquals(gl.calls.toString(), 1, draws())
        val id = slots[0].array.texture.let { (it as GlesTexture).textureId }
        assertTrue(gl.calls.toString(), gl.calls.contains("glBindTexture(${GLES30.GL_TEXTURE_2D_ARRAY},$id)"))
        assertTrue(gl.calls.toString(), gl.calls.contains("glVertexAttribPointer(3,1,${GLES20.GL_UNSIGNED_SHORT},false,24,20)"))
        assertEquals(4 * QuadBatcher.VERTEX_SIZE_BYTES, gl.vertexUploads.last().size)
    }

    @Test
    fun addGlyph_writesLayerPerVertex() {
        glyph(slots[0])
        glyph(slots[1])
        QuadBatcher.flush()

        val b = lastVertices()
        for (quad in 0 until 2) {
            for (vertex in 0 until 4) {
                val base = (quad * 4 + vertex) * QuadBatcher.VERTEX_SIZE_BYTES
                assertEquals(slots[quad].layer, b.getShort(base + 20).toInt())
            }
        }
        assertTrue(slots[0].layer != slots[1].layer)
    }

    @Test
    fun glyphAfterSprite_sharesTheDrawInOrder() {
        sprite()
        glyph(slots[0])
        assertEquals(0, draws())
        sprite()
        assertEquals(0, draws())
        QuadBatcher.flush()
        assertEquals(gl.calls.toString(), 1, draws())
        assertTrue(gl.calls.contains("glDrawElements(${GLES20.GL_TRIANGLES},18,${GLES20.GL_UNSIGNED_SHORT},0)"))

        // The quads keep the order of the calls: sprite, glyph, sprite.
        val b = lastVertices()
        val layers = (0 until 3).map { b.getShort(it * 4 * QuadBatcher.VERTEX_SIZE_BYTES + 20).toInt() and 0xFFFF }
        assertEquals(listOf(QuadBatcher.SPRITE_LAYER, slots[0].layer, QuadBatcher.SPRITE_LAYER), layers)

        val arrayId = (slots[0].array.texture as GlesTexture).textureId
        val draw = gl.calls.indexOfFirst { it.startsWith("glDrawElements") }
        val arrayBind = gl.calls.indexOf("glBindTexture(${GLES30.GL_TEXTURE_2D_ARRAY},$arrayId)")
        assertTrue(gl.calls.toString(), arrayBind in 0 until draw)
    }

    @Test
    fun addGlyph_notUploadedSlot_drawsNothing() {
        slots[0].uploaded = false
        glyph(slots[0])
        QuadBatcher.flush()
        assertEquals(gl.calls.toString(), 0, draws())
    }

    @Test
    fun addGlyph_sameArray_oneDraw() {
        glyph(slots[0])
        glyph(slots[1])
        QuadBatcher.flush()
        assertEquals(gl.calls.toString(), 1, draws())
        assertTrue(gl.calls.contains("glDrawElements(${GLES20.GL_TRIANGLES},12,${GLES20.GL_UNSIGNED_SHORT},0)"))
    }

    private fun ortho(width: Float, height: Float) = floatArrayOf(
        2f / width, 0f, 0f, 0f,
        0f, -2f / height, 0f, 0f,
        0f, 0f, 1f, 0f,
        -1f, 1f, 0f, 1f
    )

    @Test
    fun addGlyph_snapsOriginKeepsSize() {
        GlesBackend.surfaceWidth = 2400
        GlesBackend.surfaceHeight = 1080
        QuadBatcher.setProjection(ortho(1280f, 720f))

        glyph(slots[0], x0 = 10.3f, y0 = 20.2f, x1 = 20.3f, y1 = 40.2f)
        QuadBatcher.flush()

        val b = lastVertices()
        val x0 = b.getFloat(0)
        val y0 = b.getFloat(4)
        val x2 = b.getFloat(2 * QuadBatcher.VERTEX_SIZE_BYTES)
        val y2 = b.getFloat(2 * QuadBatcher.VERTEX_SIZE_BYTES + 4)
        assertWholePixel(x0 * 2400f / 1280f, 0.001f)
        assertWholePixel(y0 * 1080f / 720f, 0.001f)
        assertEquals(10f, x2 - x0, 0.001f)
        assertEquals(20f, y2 - y0, 0.001f)
    }

    @Test
    fun addGlyph_snapsWithScaledMatrix() {
        GlesBackend.surfaceWidth = 2400
        GlesBackend.surfaceHeight = 1080
        QuadBatcher.setProjection(ortho(1280f, 720f))
        val scaled = identity.copyOf().also { it[0] = 1.5f; it[5] = 1.5f; it[12] = 3.1f; it[13] = 2.7f }

        glyph(slots[0], scaled, 10.3f, 20.2f, 20.3f, 40.2f)
        QuadBatcher.flush()

        val b = lastVertices()
        val x0 = b.getFloat(0)
        val y0 = b.getFloat(4)
        val x2 = b.getFloat(2 * QuadBatcher.VERTEX_SIZE_BYTES)
        val y2 = b.getFloat(2 * QuadBatcher.VERTEX_SIZE_BYTES + 4)
        assertWholePixel(x0 * 2400f / 1280f, 0.002f)
        assertWholePixel(y0 * 1080f / 720f, 0.002f)
        assertEquals(15f, x2 - x0, 0.001f)
        assertEquals(30f, y2 - y0, 0.001f)
    }

    @Test
    fun addGlyph_withoutSurface_doesNotSnap() {
        GlesBackend.surfaceWidth = 0
        GlesBackend.surfaceHeight = 0
        glyph(slots[0], x0 = 10.3f, y0 = 20.2f, x1 = 14.3f, y1 = 24.2f)
        QuadBatcher.flush()
        assertEquals(10.3f, lastVertices().getFloat(0), 0f)
    }

    private fun assertWholePixel(pixels: Float, tolerance: Float) {
        assertEquals("pixel position $pixels", Math.round(pixels).toFloat(), pixels, tolerance)
    }
}
