package org.andengine.graphics.batch

import android.opengl.GLES20
import android.opengl.GLES30
import org.andengine.graphics.BlendMode
import org.andengine.graphics.PrimitiveType
import org.andengine.graphics.Texture
import org.andengine.graphics.TextureFormat
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesDebugOptions
import org.andengine.graphics.gles.GlesGraphicsDevice
import org.andengine.graphics.gles.GlesRenderContext
import org.andengine.graphics.gles.GlesTexture
import org.andengine.graphics.gles.IDENTITY
import org.andengine.graphics.gles.RecordingGL
import org.andengine.graphics.gles.draws
import org.andengine.graphics.gles.indexOfCall
import org.andengine.graphics.gles.installRecordingGL
import org.andengine.graphics.gles.lastCallBefore
import org.andengine.graphics.gles.lastVertices
import org.andengine.graphics.text.GlyphAtlas
import org.andengine.graphics.text.GlyphSlot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.andengine.graphics.gles.endOpenPass

/** Sprites and glyphs share one program, one vertex format and one draw. */
@RunWith(RobolectricTestRunner::class)
class QuadBatcherMixedTest {

    private lateinit var gl: RecordingGL
    private lateinit var atlas: GlyphAtlas
    private lateinit var spriteA: GlesTexture
    private lateinit var spriteB: GlesTexture
    private val slots = ArrayList<GlyphSlot>()

    private val triangle = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f)

    private fun drawMesh() = MeshRenderer.draw(
        PrimitiveType.Triangles, triangle, 0, 3, IDENTITY, IDENTITY, -1, BlendMode.Alpha, null, false
    )

    private fun sprite(texture: Texture = spriteA) =
        QuadBatcher.add(texture, BlendMode.Alpha, IDENTITY, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, -1)

    private fun glyph(slot: GlyphSlot = slots[0]) =
        QuadBatcher.addGlyph(slot, BlendMode.Alpha, IDENTITY, 0f, 0f, 4f, 4f, -1)

    private fun id(texture: Texture) = (texture as GlesTexture).textureId

    @Before
    fun setUp() {
        GlesDebugOptions.simulateEs30 = false
        gl = installRecordingGL()
        GlesBackend.surfaceWidth = 800
        GlesBackend.surfaceHeight = 480
        // Two layers per array: the third 60x60 slot lands in the second array.
        atlas = GlyphAtlas(GlesGraphicsDevice.instance, 64, 2, 2)
        spriteA = GlesGraphicsDevice.instance.createTexture(4, 4, TextureFormat.RGBA8, label = "a") as GlesTexture
        spriteB = GlesGraphicsDevice.instance.createTexture(4, 4, TextureFormat.RGBA8, label = "b") as GlesTexture
        slots += atlas.allocate(60, 60)!!
        slots += atlas.allocate(60, 60)!!
        slots += atlas.allocate(60, 60)!!
        for (s in slots) s.uploaded = true
        // Created up front, so the textures exist before any batch is pending.
        for (s in slots) s.array.texture
        QuadBatcher.setProjection(IDENTITY)
        gl.calls.clear()
    }

    @After
    fun tearDown() {
        QuadBatcher.releaseResources()
        MeshRenderer.releaseResources()
        endOpenPass()
        spriteA.dispose()
        spriteB.dispose()
        for (s in slots) if (s.array.hasTexture) s.array.texture.dispose()
        GlesBackend.surfaceWidth = 0
        GlesBackend.surfaceHeight = 0
    }

    @Test
    fun spriteGlyphSprite_sameTextures_oneDraw() {
        sprite()
        glyph()
        sprite()
        glyph()
        assertEquals(gl.calls.toString(), 0, gl.draws())

        QuadBatcher.flush()

        assertEquals(gl.calls.toString(), 1, gl.draws())
        assertTrue(gl.calls.contains("glDrawElements(${GLES20.GL_TRIANGLES},24,${GLES20.GL_UNSIGNED_SHORT},0)"))
        assertEquals(4 * 4 * QuadBatcher.VERTEX_SIZE_BYTES, gl.vertexUploads.last().size)
        assertEquals(1, QuadBatcher.pipelineCount)
        assertEquals(gl.calls.toString(), 1, gl.calls.count { it.startsWith("glCreateProgram") })
    }

    @Test
    fun differentSpriteTexture_flushes() {
        sprite(spriteA)
        glyph()
        sprite(spriteB)
        assertEquals(gl.calls.toString(), 1, gl.draws())
        assertEquals(2 * 4 * QuadBatcher.VERTEX_SIZE_BYTES, gl.vertexUploads.last().size)

        // The glyph array stays the same: no flush for the glyph after the new sprite texture.
        glyph()
        assertEquals(gl.calls.toString(), 1, gl.draws())
        QuadBatcher.flush()
        assertEquals(gl.calls.toString(), 2, gl.draws())
        assertEquals(2 * 4 * QuadBatcher.VERTEX_SIZE_BYTES, gl.vertexUploads.last().size)
    }

    @Test
    fun differentGlyphArray_flushes() {
        assertSame(slots[0].array, slots[1].array)
        assertNotSame(slots[0].array, slots[2].array)
        glyph(slots[0])
        sprite()
        glyph(slots[2])
        assertEquals(gl.calls.toString(), 1, gl.draws())

        // The sprite texture stays the same: no flush for the sprite after the new array.
        sprite()
        assertEquals(gl.calls.toString(), 1, gl.draws())
        QuadBatcher.flush()
        assertEquals(gl.calls.toString(), 2, gl.draws())

        val first = gl.calls.indexOfFirst { it.startsWith("glDrawElements") }
        val bind = gl.indexOfCall("glBindTexture(${GLES30.GL_TEXTURE_2D_ARRAY},${id(slots[2].array.texture)})")
        assertTrue(gl.calls.toString(), first < bind)
    }

    @Test
    fun blendChange_flushesMixedBatch() {
        sprite()
        glyph()
        QuadBatcher.add(spriteA, BlendMode.Additive, IDENTITY, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, -1)
        assertEquals(gl.calls.toString(), 1, gl.draws())
    }

    @Test
    fun spriteVertex_hasSpriteLayerMarker() {
        sprite()
        glyph()
        QuadBatcher.flush()

        assertEquals(24, QuadBatcher.VERTEX_SIZE_BYTES)
        val b = gl.lastVertices()
        for (vertex in 0 until 4) {
            val base = vertex * QuadBatcher.VERTEX_SIZE_BYTES
            assertEquals(QuadBatcher.SPRITE_LAYER, b.getShort(base + 20).toInt() and 0xFFFF)
            // The color stays at offset 16.
            assertEquals(-1, b.getInt(base + 16))
        }
        for (vertex in 4 until 8) {
            val base = vertex * QuadBatcher.VERTEX_SIZE_BYTES
            assertEquals(slots[0].layer, b.getShort(base + 20).toInt() and 0xFFFF)
        }
        assertTrue(slots[0].layer != QuadBatcher.SPRITE_LAYER)
        assertTrue(gl.calls.toString(), gl.calls.contains("glVertexAttribPointer(3,1,${GLES20.GL_UNSIGNED_SHORT},false,24,20)"))
    }

    @Test
    fun bindsSpriteOnUnit0AndGlyphsOnUnit1() {
        sprite()
        glyph()
        QuadBatcher.flush()

        val draw = gl.calls.indexOfFirst { it.startsWith("glDrawElements") }
        val unit1 = gl.calls.subList(0, draw).lastIndexOf("glActiveTexture(${GLES20.GL_TEXTURE1})")
        val arrayBind = gl.calls.subList(0, draw).lastIndexOf("glBindTexture(${GLES30.GL_TEXTURE_2D_ARRAY},${id(slots[0].array.texture)})")
        val unit0 = gl.calls.subList(0, draw).lastIndexOf("glActiveTexture(${GLES20.GL_TEXTURE0})")
        val spriteBind = gl.calls.subList(0, draw).lastIndexOf("glBindTexture(${GLES20.GL_TEXTURE_2D},${spriteA.textureId})")
        assertTrue(gl.calls.toString(), unit1 in 0 until arrayBind)
        assertTrue(gl.calls.toString(), arrayBind < unit0 && unit0 < spriteBind)
        // Nothing switches the unit between the array bind and the sprite unit.
        assertEquals(gl.calls.toString(), unit0, gl.calls.subList(0, spriteBind).indexOfLast { it.startsWith("glActiveTexture") })

        // The samplers read their units: set once, when the program is built.
        assertEquals(gl.calls.toString(), listOf("glUniform1i(0,0)", "glUniform1i(0,1)"), gl.calls.filter { it.startsWith("glUniform1i") })
        sprite()
        glyph()
        QuadBatcher.flush()
        assertEquals(gl.calls.toString(), 2, gl.calls.count { it.startsWith("glUniform1i") })
    }

    @Test
    fun glyphOnlyBatch_bindsNoSpriteTexture() {
        glyph()
        QuadBatcher.flush()

        assertEquals(gl.calls.toString(), 1, gl.draws())
        assertFalse(gl.calls.toString(), gl.calls.any { it.startsWith("glBindTexture(${GLES20.GL_TEXTURE_2D},") })
        assertEquals("glActiveTexture(${GLES20.GL_TEXTURE0})", gl.calls.last { it.startsWith("glActiveTexture") })
    }

    @Test
    fun externalFlush_restoresUnit0State() {
        // Set up by someone else before the quads were added: unit 0 active with its texture.
        val state = GlesBackend.state
        state.activeTexture(0)
        state.bindTexture(GLES20.GL_TEXTURE_2D, 55)
        sprite()
        glyph()

        // The mesh flushes the quads first and binds no texture of its own.
        drawMesh()

        val draw = gl.indexOfCall("glDrawArrays(${GLES20.GL_TRIANGLES},0,3)")
        val batch = gl.calls.indexOfFirst { it.startsWith("glDrawElements") }
        assertTrue(gl.calls.toString(), batch in 0 until draw)
        assertEquals("glActiveTexture(${GLES20.GL_TEXTURE0})", gl.lastCallBefore(draw, "glActiveTexture"))
        assertEquals("glBindTexture(${GLES20.GL_TEXTURE_2D},55)", gl.lastCallBefore(draw, "glBindTexture(${GLES20.GL_TEXTURE_2D},"))
        assertFalse(QuadBatcher.hasPending)
    }

    @Test
    fun externalFlush_restoresUnit1Array() {
        // Set up by someone else: an array of its own on unit 1, unit 0 active.
        val state = GlesBackend.state
        state.activeTexture(1)
        state.bindTexture(GLES30.GL_TEXTURE_2D_ARRAY, 66)
        state.activeTexture(0)
        glyph()

        QuadBatcher.flush()

        val draw = gl.calls.indexOfFirst { it.startsWith("glDrawElements") }
        assertTrue(gl.calls.toString(), draw >= 0)
        val after = gl.calls.subList(draw, gl.calls.size)
        assertEquals("glBindTexture(${GLES30.GL_TEXTURE_2D_ARRAY},66)", after.last { it.startsWith("glBindTexture(${GLES30.GL_TEXTURE_2D_ARRAY},") })
        assertEquals("glActiveTexture(${GLES20.GL_TEXTURE0})", gl.calls.last { it.startsWith("glActiveTexture") })
    }

    @Test
    fun internalFlushInPass_leavesUnit0Active() {
        val context = GlesRenderContext.instance
        context.beginInlinePass()
        sprite()
        glyph(slots[0])
        // Another array: flushes inside the open pass, which the batch does not end.
        glyph(slots[2])
        assertEquals(gl.calls.toString(), 1, gl.draws())
        assertTrue(context.isInPass)

        assertEquals("glActiveTexture(${GLES20.GL_TEXTURE0})", gl.calls.last { it.startsWith("glActiveTexture") })
        // A bind through the state cache lands on unit 0.
        GlesBackend.state.bindTexture(GLES20.GL_TEXTURE_2D, 77)
        val bind = gl.indexOfCall("glBindTexture(${GLES20.GL_TEXTURE_2D},77)")
        assertEquals("glActiveTexture(${GLES20.GL_TEXTURE0})", gl.lastCallBefore(bind, "glActiveTexture"))
    }
}
