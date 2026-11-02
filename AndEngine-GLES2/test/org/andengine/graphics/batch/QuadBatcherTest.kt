package org.andengine.graphics.batch

import android.opengl.GLES20
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.andengine.graphics.BlendMode
import org.andengine.graphics.Texture
import org.andengine.graphics.TextureFormat
import org.andengine.graphics.debug.RenderStats
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesDebugOptions
import org.andengine.graphics.gles.GlesGraphicsDevice
import org.andengine.graphics.gles.GlesLegacyTextureView
import org.andengine.graphics.gles.GlesPlaceholderTexture
import org.andengine.graphics.gles.GlesRenderContext
import org.andengine.graphics.gles.GlesTexture
import org.andengine.graphics.gles.RecordingGL
import org.andengine.opengl.texture.ITexture
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuadBatcherTest {

    private lateinit var gl: RecordingGL
    private lateinit var textureA: GlesTexture
    private lateinit var textureB: GlesTexture

    private val identity = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    private fun newContext() {
        gl = RecordingGL()
        gl.intValues[GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS] = 16
        gl.intValues[GLES20.GL_MAX_TEXTURE_SIZE] = 4096
        GlesBackend.onContextCreatedForTest(gl)
    }

    private fun texture(label: String) =
        GlesGraphicsDevice.instance.createTexture(4, 4, TextureFormat.RGBA8, label = label) as GlesTexture

    private fun add(texture: Texture = textureA, blend: BlendMode = BlendMode.Alpha, matrix: FloatArray = identity, rgba: Int = -1) =
        QuadBatcher.add(texture, blend, matrix, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, rgba)

    private fun draws() = gl.calls.count { it.startsWith("glDrawElements") }

    /** The vertex data of the last vertex upload, in the device's byte order. */
    private fun lastVertices(): ByteBuffer =
        ByteBuffer.wrap(gl.vertexUploads.last()).order(ByteOrder.nativeOrder())

    @Before
    fun setUp() {
        GlesDebugOptions.simulateEs30 = false
        newContext()
        GlesBackend.surfaceWidth = 800
        GlesBackend.surfaceHeight = 480
        textureA = texture("a")
        textureB = texture("b")
        QuadBatcher.setProjection(identity)
        gl.calls.clear()
    }

    @After
    fun tearDown() {
        QuadBatcher.releaseResources()
        QuadBatcher.counterRegistration = QuadBatcher.DEFAULT_COUNTER_REGISTRATION
        val context = GlesRenderContext.instance
        if (context.isInPass) context.endPass()
        textureA.dispose()
        textureB.dispose()
        RenderStats.enabled = false
    }

    @Test
    fun packColor_isAbgrClampedAndRounded() {
        assertEquals(0x663399FF, QuadBatcher.packColor(1f, 0.6f, 0.2f, 0.4f))
        assertEquals(0x00000000, QuadBatcher.packColor(-1f, -0.5f, 0f, 0f))
        assertEquals(0xFFFFFFFF.toInt(), QuadBatcher.packColor(2f, 1.5f, 1f, 3f))
        // 0.5 * 255 = 127.5 rounds up
        assertEquals(0x80, QuadBatcher.packColor(0.5f, 0f, 0f, 0f))
    }

    @Test
    fun sameTextureAndBlend_oneDraw() {
        repeat(100) { add() }
        assertTrue(QuadBatcher.hasPending)
        assertEquals(0, draws())

        QuadBatcher.flush()

        assertEquals(gl.calls.toString(), 1, draws())
        assertTrue(gl.calls.contains("glDrawElements(${GLES20.GL_TRIANGLES},600,${GLES20.GL_UNSIGNED_SHORT},0)"))
        assertFalse(QuadBatcher.hasPending)
        assertEquals(100 * 4 * QuadBatcher.VERTEX_SIZE_BYTES, gl.vertexUploads.last().size)
    }

    @Test
    fun emptyFlush_drawsNothing() {
        QuadBatcher.flush()
        assertEquals(0, draws())
        assertEquals(gl.calls.toString(), 0, gl.calls.size)
    }

    @Test
    fun textureChange_flushes() {
        add(textureA)
        add(textureA)
        add(textureB)

        assertEquals(1, draws())
        assertTrue(gl.calls.contains("glDrawElements(${GLES20.GL_TRIANGLES},12,${GLES20.GL_UNSIGNED_SHORT},0)"))
        assertTrue(gl.calls.toString(), gl.calls.contains("glBindTexture(${GLES20.GL_TEXTURE_2D},${textureA.textureId})"))

        QuadBatcher.flush()

        assertEquals(2, draws())
        assertTrue(gl.calls.toString(), gl.calls.contains("glBindTexture(${GLES20.GL_TEXTURE_2D},${textureB.textureId})"))
    }

    @Test
    fun blendChange_flushes() {
        add(blend = BlendMode.Alpha)
        add(blend = BlendMode.Alpha)
        assertEquals(0, draws())

        add(blend = BlendMode.Additive)
        assertEquals(1, draws())

        // Equal content, other instance: same batch.
        add(blend = BlendMode(BlendMode.Additive.srcColor, BlendMode.Additive.dstColor, BlendMode.Additive.srcAlpha, BlendMode.Additive.dstAlpha))
        assertEquals(1, draws())

        QuadBatcher.flush()
        assertEquals(2, draws())
        assertTrue(
            gl.calls.toString(),
            gl.calls.contains("glBlendFuncSeparate(${GLES20.GL_SRC_ALPHA},${GLES20.GL_ONE},${GLES20.GL_ONE},${GLES20.GL_ONE})")
        )
    }

    @Test
    fun flushReasons_counted() {
        RenderStats.enabled = true
        RenderStats.reset()
        add()
        add(texture = textureB)
        add(texture = textureB, blend = BlendMode.Additive)
        add(texture = textureA, blend = BlendMode.Additive)
        QuadBatcher.flush()
        add()
        QuadBatcher.flushFrame()
        repeat(QuadBatcher.MAX_QUADS) { add() }
        add()
        QuadBatcher.flush()
        QuadBatcher.flush()

        fun count(reason: Int) = RenderStats.currentCount(QuadBatcher.flushCounterIndex(reason))
        // texture twice (A->B, B->A with the blend change counted as texture), blend once.
        assertEquals(2, count(QuadBatcher.REASON_TEXTURE))
        assertEquals(1, count(QuadBatcher.REASON_BLEND))
        assertEquals(2, count(QuadBatcher.REASON_FOREIGN))
        assertEquals(1, count(QuadBatcher.REASON_FULL))
        assertEquals(1, count(QuadBatcher.REASON_FRAME))
    }

    @Test
    fun projectionChange_flushes() {
        add()
        QuadBatcher.setProjection(identity.copyOf())
        assertEquals("same content must not flush", 0, draws())

        val moved = identity.copyOf().also { it[12] = 5f }
        QuadBatcher.setProjection(moved)
        assertEquals(1, draws())
        assertTrue("the old projection belongs to the flushed quads", identity.contentEquals(gl.uniformMatrices.last()))

        add()
        QuadBatcher.flush()
        assertEquals(2, draws())
        assertTrue(moved.contentEquals(gl.uniformMatrices.last()))
    }

    @Test
    fun setProjection_copiesTheMatrix() {
        val m = identity.copyOf()
        QuadBatcher.setProjection(m)
        m[12] = 9f
        add()
        QuadBatcher.flush()
        assertTrue(identity.contentEquals(gl.uniformMatrices.last()))
    }

    @Test
    fun projection_uploadedEveryFlush() {
        add()
        QuadBatcher.flush()
        add()
        QuadBatcher.flush()
        assertEquals(gl.calls.toString(), 2, gl.calls.count { it.startsWith("glUniformMatrix4fv") })
    }

    @Test
    fun capacity_flushesAt4096() {
        repeat(QuadBatcher.MAX_QUADS) { add() }
        assertEquals(0, draws())

        add()

        assertEquals(1, draws())
        assertTrue(gl.calls.contains("glDrawElements(${GLES20.GL_TRIANGLES},${QuadBatcher.MAX_QUADS * 6},${GLES20.GL_UNSIGNED_SHORT},0)"))
        assertTrue(QuadBatcher.hasPending)

        QuadBatcher.flush()
        assertEquals(2, draws())
        assertTrue(gl.calls.contains("glDrawElements(${GLES20.GL_TRIANGLES},6,${GLES20.GL_UNSIGNED_SHORT},0)"))
    }

    @Test
    fun indexBuffer_twoTrianglesPerQuad() {
        add()
        QuadBatcher.flush()
        val indices = ByteBuffer.wrap(gl.bufferSubDataBytes.single { it.size == QuadBatcher.MAX_QUADS * 6 * 2 })
            .order(ByteOrder.nativeOrder()).asShortBuffer()
        val firstTwo = ShortArray(12).also { indices.get(it) }
        assertTrue(firstTwo.contentToString(), firstTwo.contentEquals(shortArrayOf(0, 1, 2, 2, 3, 0, 4, 5, 6, 6, 7, 4)))
        assertEquals((QuadBatcher.MAX_QUADS * 4 - 1).toShort(), indices.get(QuadBatcher.MAX_QUADS * 6 - 2))
    }

    @Test
    fun matrixApplied_onCpu() {
        val translate = identity.copyOf().also {
            it[12] = 10f
            it[13] = 20f
        }
        QuadBatcher.add(textureA, BlendMode.Alpha, translate, 0f, 0f, 2f, 3f, 0.25f, 0.5f, 0.75f, 1f, -1)
        QuadBatcher.flush()

        val v = lastVertices()
        val stride = QuadBatcher.VERTEX_SIZE_BYTES
        val expected = arrayOf(
            floatArrayOf(10f, 20f, 0.25f, 0.5f),
            floatArrayOf(12f, 20f, 0.75f, 0.5f),
            floatArrayOf(12f, 23f, 0.75f, 1f),
            floatArrayOf(10f, 23f, 0.25f, 1f)
        )
        for (i in 0 until 4) {
            val e = expected[i]
            val actual = floatArrayOf(v.getFloat(i * stride), v.getFloat(i * stride + 4), v.getFloat(i * stride + 8), v.getFloat(i * stride + 12))
            assertTrue("vertex $i: ${actual.contentToString()}", e.contentEquals(actual))
        }
    }

    @Test
    fun matrixApplied_rotationAndScale() {
        // Column-major: x' = 0*x - 2*y, y' = 2*x + 0*y (rotation by 90 degrees, scale 2).
        val m = identity.copyOf().also {
            it[0] = 0f; it[1] = 2f
            it[4] = -2f; it[5] = 0f
        }
        QuadBatcher.add(textureA, BlendMode.Alpha, m, 1f, 0f, 2f, 1f, 0f, 0f, 1f, 1f, -1)
        QuadBatcher.flush()

        val v = lastVertices()
        val stride = QuadBatcher.VERTEX_SIZE_BYTES
        // Corner 2 is (x1, y1) = (2, 1) -> (-2, 4).
        assertEquals(-2f, v.getFloat(2 * stride), 0f)
        assertEquals(4f, v.getFloat(2 * stride + 4), 0f)
    }

    @Test
    fun addQuad_usesCornersAsGiven() {
        QuadBatcher.addQuad(
            textureA, BlendMode.Alpha,
            floatArrayOf(1f, 5f, 6f, 2f), floatArrayOf(1f, 2f, 7f, 8f),
            floatArrayOf(0f, 1f, 1f, 0f), floatArrayOf(0f, 0f, 1f, 1f),
            -1
        )
        QuadBatcher.flush()

        val v = lastVertices()
        val stride = QuadBatcher.VERTEX_SIZE_BYTES
        assertEquals(6f, v.getFloat(2 * stride), 0f)
        assertEquals(7f, v.getFloat(2 * stride + 4), 0f)
        assertEquals(1f, v.getFloat(2 * stride + 8), 0f)
        assertEquals(1f, v.getFloat(2 * stride + 12), 0f)
        assertEquals(2f, v.getFloat(3 * stride), 0f)
        assertEquals(8f, v.getFloat(3 * stride + 4), 0f)
    }

    @Test
    fun addQuadWithMatrix_transformsCorners() {
        // Scale x by 2, y by 3, then translate by (10, 20); column-major.
        val matrix = floatArrayOf(
            2f, 0f, 0f, 0f,
            0f, 3f, 0f, 0f,
            0f, 0f, 1f, 0f,
            10f, 20f, 0f, 1f
        )
        QuadBatcher.addQuad(
            textureA, BlendMode.Alpha, matrix,
            floatArrayOf(1f, 5f, 6f, 2f), floatArrayOf(1f, 2f, 7f, 8f),
            floatArrayOf(0f, 1f, 1f, 0f), floatArrayOf(0f, 0f, 1f, 1f),
            -1
        )
        QuadBatcher.flush()

        val v = lastVertices()
        val stride = QuadBatcher.VERTEX_SIZE_BYTES
        assertEquals(22f, v.getFloat(2 * stride), 0f)
        assertEquals(41f, v.getFloat(2 * stride + 4), 0f)
        assertEquals(1f, v.getFloat(2 * stride + 8), 0f)
        assertEquals(1f, v.getFloat(2 * stride + 12), 0f)
        assertEquals(14f, v.getFloat(3 * stride), 0f)
        assertEquals(44f, v.getFloat(3 * stride + 4), 0f)
    }

    @Test
    fun color_packedPerVertex() {
        // ABGR packed: alpha 0x80, blue 0xFF, green 0x40, red 0x20.
        val abgr = 0x80FF4020.toInt()
        add(rgba = abgr)
        QuadBatcher.flush()

        val bytes = gl.vertexUploads.last()
        val stride = QuadBatcher.VERTEX_SIZE_BYTES
        for (i in 0 until 4) {
            val o = i * stride + 16
            assertEquals("r of vertex $i", 0x20.toByte(), bytes[o])
            assertEquals("g of vertex $i", 0x40.toByte(), bytes[o + 1])
            assertEquals("b of vertex $i", 0xFF.toByte(), bytes[o + 2])
            assertEquals("a of vertex $i", 0x80.toByte(), bytes[o + 3])
        }
    }

    @Test
    fun flush_keepsLegacyFramebufferViewportAndScissor() {
        GlesBackend.state.bindFramebuffer(9)
        GlesBackend.state.viewport(0, 0, 100, 50)
        GlesBackend.state.setScissorEnabled(true)
        gl.calls.clear()

        add()
        QuadBatcher.flush()

        assertEquals(1, draws())
        assertEquals(gl.calls.toString(), 0, gl.calls.count { it.startsWith("glBindFramebuffer") || it.startsWith("glViewport") })
        // Unclipped quads are drawn without the scissor test, which is switched on again after.
        val draw = gl.calls.indexOfFirst { it.startsWith("glDrawElements") }
        val scissorCalls = setOf("glEnable(${GLES20.GL_SCISSOR_TEST})", "glDisable(${GLES20.GL_SCISSOR_TEST})")
        assertEquals(gl.calls.toString(), "glDisable(${GLES20.GL_SCISSOR_TEST})", gl.calls.subList(0, draw).lastOrNull { it in scissorCalls })
        assertEquals(gl.calls.toString(), "glEnable(${GLES20.GL_SCISSOR_TEST})", gl.calls.lastOrNull { it in scissorCalls })
        assertTrue(GlesBackend.state.isScissorEnabled)
        assertFalse(GlesRenderContext.instance.isInPass)
        assertEquals(0, GlesBackend.state.currentActiveTextureUnit)
    }

    @Test
    fun flush_insideOpenPass_keepsThePass() {
        val context = GlesRenderContext.instance
        context.beginPass(null, null, false)
        add()
        QuadBatcher.flush()
        assertEquals(1, draws())
        assertTrue(context.isInPass)
        context.endPass()
    }

    @Test
    fun pipelines_cachedPerBlend() {
        add(blend = BlendMode.Alpha)
        QuadBatcher.flush()
        add(blend = BlendMode.Additive)
        QuadBatcher.flush()
        add(blend = BlendMode.Alpha)
        QuadBatcher.flush()
        assertEquals(2, QuadBatcher.pipelineCount)
        assertEquals(gl.calls.toString(), 1, gl.calls.count { it.startsWith("glCreateProgram") })
    }

    @Test
    fun resources_restoredAfterContextLoss() {
        add()
        QuadBatcher.flush()
        newContext()

        add()
        QuadBatcher.flush()

        assertEquals(gl.calls.toString(), 1, draws())
        // The registry restored the index buffer from its CPU copy; the batcher did not create a
        // second set of resources.
        assertTrue(gl.bufferDataBytes.any { it.size == QuadBatcher.MAX_QUADS * 6 * 2 })
        assertEquals(0, gl.bufferSubDataBytes.count { it.size == QuadBatcher.MAX_QUADS * 6 * 2 })
    }

    @Test
    fun statsCounter_registeredAtFirstFlush() {
        var registered = 0
        QuadBatcher.counterRegistration = QuadBatcher.CounterRegistration { registered++; 3 }
        repeat(3) { add() }
        assertEquals(0, registered)

        QuadBatcher.flush()
        add()
        QuadBatcher.flush()

        assertEquals(1, registered)
        assertEquals(3, QuadBatcher.quadCounterIndex)
    }

    @Test
    fun statsCounter_registrationFailure_keepsDrawing() {
        RenderStats.enabled = true
        QuadBatcher.counterRegistration = QuadBatcher.CounterRegistration { throw IllegalStateException("full") }

        add()
        QuadBatcher.flush()
        add()
        QuadBatcher.flush()

        assertEquals(2, draws())
        assertEquals(-1, QuadBatcher.quadCounterIndex)
    }

    @Test
    fun everyFlush_givesTheVertexBufferNewStorageWithTheQuads() {
        add()
        QuadBatcher.flush()
        gl.calls.clear()
        add()
        QuadBatcher.flush()
        add()
        QuadBatcher.flush()

        val uploads = gl.calls.filter { it.startsWith("glBufferData") || it.startsWith("glBufferSubData") }
        val replace = "glBufferData(${GLES20.GL_ARRAY_BUFFER},${4 * QuadBatcher.VERTEX_SIZE_BYTES},buf,${android.opengl.GLES30.GL_STREAM_DRAW})"
        assertEquals(listOf(replace, replace), uploads)
    }

    @Test
    fun unloadedLegacyTexture_quadSkippedWithoutFlush() {
        var id = -1
        val view = GlesLegacyTextureView({ id }, 16, 16, "legacy")
        assertFalse(view.isLoaded)
        add(textureA)

        add(view)

        assertEquals("no flush for a skipped quad", 0, draws())
        QuadBatcher.flush()
        assertEquals(gl.calls.toString(), 1, draws())
        assertTrue(gl.calls.contains("glDrawElements(${GLES20.GL_TRIANGLES},6,${GLES20.GL_UNSIGNED_SHORT},0)"))

        QuadBatcher.addQuad(view, BlendMode.Additive, FloatArray(4), FloatArray(4), FloatArray(4), FloatArray(4), -1)
        assertFalse(QuadBatcher.hasPending)

        id = 4
        assertTrue(view.isLoaded)
        add(view)
        assertTrue(QuadBatcher.hasPending)
    }

    private val moved = floatArrayOf(
        2f, 0.5f, 0f, 0f,
        -0.25f, 3f, 0f, 0f,
        0f, 0f, 1f, 0f,
        5f, 7f, 0f, 1f
    )

    private fun valueQuad(texture: Texture = textureA, x: Float = 0f, rgba: Int = 0x40FF8020) =
        QuadBatcher.addQuad(
            texture, BlendMode.Alpha, moved,
            x, 0f, x + 1f, 0.1f, x + 1.2f, 1f, x - 0.1f, 0.9f,
            0.1f, 0.2f, 0.7f, 0.9f, rgba
        )

    private fun arrayQuad(texture: Texture = textureA, x: Float = 0f, rgba: Int = 0x40FF8020) =
        QuadBatcher.addQuad(
            texture, BlendMode.Alpha, moved,
            floatArrayOf(x, x + 1f, x + 1.2f, x - 0.1f), floatArrayOf(0f, 0.1f, 1f, 0.9f),
            floatArrayOf(0.1f, 0.7f, 0.7f, 0.1f), floatArrayOf(0.2f, 0.2f, 0.9f, 0.9f), rgba
        )

    @Test
    fun valueQuad_sameVerticesAsArrayQuad() {
        arrayQuad()
        arrayQuad(x = 2f, rgba = -1)
        QuadBatcher.flush()
        val expected = gl.vertexUploads.last().copyOf()

        valueQuad()
        valueQuad(x = 2f, rgba = -1)
        QuadBatcher.flush()

        assertArrayEquals(expected, gl.vertexUploads.last())
        assertEquals(2 * 4 * QuadBatcher.VERTEX_SIZE_BYTES, expected.size)
    }

    @Test
    fun valueQuad_continuesPendingBatch() {
        valueQuad()
        valueQuad(x = 1f)
        valueQuad(x = 2f)
        assertEquals("no flush within one texture", 0, draws())
        valueQuad(textureB)
        assertEquals("another texture flushes", 1, draws())
        QuadBatcher.flush()
        assertEquals(2, draws())
    }

    @Test
    fun valueQuad_continuingBatch_isStillClipped() {
        // Identity projection: the left half of the surface is x < 0 in world units.
        QuadBatcher.setClip(0, 0, 400, 480)
        QuadBatcher.addQuad(textureA, BlendMode.Alpha, identity, -0.9f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.9f, 0.5f, 0f, 0f, 1f, 1f, -1)
        QuadBatcher.addQuad(textureA, BlendMode.Alpha, identity, 0.2f, -0.5f, 0.6f, -0.5f, 0.6f, 0.5f, 0.2f, 0.5f, 0f, 0f, 1f, 1f, -1)
        QuadBatcher.clearClip()
        QuadBatcher.flush()
        assertEquals("the quad outside the clip is dropped", 4 * QuadBatcher.VERTEX_SIZE_BYTES, gl.vertexUploads.last().size)
    }

    @Test
    fun pendingTexture_notCheckedAgain_untilFlushed() {
        var id = 7
        var checks = 0
        val view = GlesLegacyTextureView({ checks++; id }, 16, 16, "legacy")
        add(view)
        val afterFirst = checks
        add(view)
        add(view)
        assertEquals("quads of the pending texture skip the load check", afterFirst, checks)

        QuadBatcher.flush()
        id = -1
        checks = 0
        add(view)
        assertTrue("checked again once the batch was drawn", checks > 0)
        assertFalse(QuadBatcher.hasPending)
    }

    @Test
    fun batchTexture_endFrame_keepsTheView() {
        val legacy = fakeLegacyTexture(4, 4) { 2 }
        val view = BatchTexture.of(legacy)
        QuadBatcher.flushFrame()
        assertSame(view, BatchTexture.of(legacy))
        BatchTexture.endFrame()
        assertSame(view, BatchTexture.of(legacy))
    }

    @Test
    fun batchTexture_isLoaded() {
        var id = 0
        val legacy = fakeLegacyTexture(4, 4) { id }
        val view = BatchTexture.of(legacy)
        assertFalse(BatchTexture.isLoaded(view))
        id = 3
        assertTrue(BatchTexture.isLoaded(view))
        assertTrue("engine textures are always drawable", BatchTexture.isLoaded(textureA))
    }

    @Test
    fun failedResourceCreation_leavesNothingHalfBuilt() {
        gl.failGenBuffers = true
        add()
        assertThrows(IllegalStateException::class.java) { QuadBatcher.flush() }
        assertTrue("the program created before the failure is released", gl.calls.any { it.startsWith("glDeleteProgram") })

        gl.failGenBuffers = false
        gl.calls.clear()
        add()
        QuadBatcher.flush()

        assertEquals(gl.calls.toString(), 1, draws())
        assertEquals(1, gl.calls.count { it.startsWith("glCreateProgram") })
    }

    @Test
    fun legacyTextureView_readsHardwareIdAtBindTime() {
        var id = 11
        val view = GlesLegacyTextureView({ id }, 16, 16, "legacy")
        add(view)
        id = 12
        QuadBatcher.flush()
        assertTrue(gl.calls.toString(), gl.calls.contains("glBindTexture(${GLES20.GL_TEXTURE_2D},12)"))
        assertEquals(0, gl.calls.count { it == "glBindTexture(${GLES20.GL_TEXTURE_2D},11)" })
    }

    @Test
    fun legacyTextureView_unloadedBeforeFlush_bindsPlaceholder() {
        var id = 5
        val view = GlesLegacyTextureView({ id }, 16, 16, "legacy")
        add(view)
        id = -1
        QuadBatcher.flush()
        assertTrue(gl.calls.toString(), gl.calls.contains("glBindTexture(${GLES20.GL_TEXTURE_2D},${GlesPlaceholderTexture.id})"))
    }

    @Test
    fun legacyTextureView_cannotUpload() {
        val view = GlesLegacyTextureView({ 1 }, 16, 16, "legacy")
        assertThrows(UnsupportedOperationException::class.java) {
            view.upload(0, 0, 0, org.andengine.graphics.TextureData(1, 1, TextureFormat.RGBA8, ByteBuffer.allocateDirect(4)))
        }
    }

    @Test
    fun batchTexture_cachesOneViewPerLegacyTexture() {
        var id = 21
        val legacy = fakeLegacyTexture(64, 32) { id }
        val other = fakeLegacyTexture(8, 8) { 5 }

        val view = BatchTexture.of(legacy)
        assertSame(view, BatchTexture.of(legacy))
        assertNotSame(view, BatchTexture.of(other))
        assertSame(view, BatchTexture.of(legacy))
        assertEquals(64, view.width)
        assertEquals(32, view.height)

        id = 22
        add(view)
        QuadBatcher.flush()
        assertTrue(gl.calls.toString(), gl.calls.contains("glBindTexture(${GLES20.GL_TEXTURE_2D},22)"))
    }

    private fun fakeLegacyTexture(width: Int, height: Int, id: () -> Int): ITexture {
        lateinit var self: ITexture
        self = Proxy.newProxyInstance(ITexture::class.java.classLoader, arrayOf(ITexture::class.java)) { _, method, args ->
            when (method.name) {
                "getHardwareTextureID" -> id()
                "getWidth" -> width
                "getHeight" -> height
                "hashCode" -> System.identityHashCode(self)
                "equals" -> self === args!![0]
                "toString" -> "FakeTexture"
                else -> throw UnsupportedOperationException(method.name)
            }
        } as ITexture
        return self
    }
}
