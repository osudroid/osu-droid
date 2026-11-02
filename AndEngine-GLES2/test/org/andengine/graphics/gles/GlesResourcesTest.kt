package org.andengine.graphics.gles

import android.opengl.GLES20
import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.andengine.graphics.BufferTarget
import org.andengine.graphics.BufferUsage
import org.andengine.graphics.GpuResource
import org.andengine.graphics.TextureData
import org.andengine.graphics.TextureFilter
import org.andengine.graphics.TextureFormat
import org.andengine.graphics.TextureSource
import org.andengine.graphics.debug.GpuFrameTimer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric for android.util.Log (texture fallback warnings).
@RunWith(RobolectricTestRunner::class)
class GlesResourcesTest {

    private lateinit var gl: RecordingGL
    private val device get() = GlesGraphicsDevice.instance
    private val created = ArrayList<GpuResource>()

    private fun newContext(extensions: String = "") {
        gl = RecordingGL()
        gl.extensionsString = extensions
        gl.intValues[GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS] = 16
        gl.intValues[GLES20.GL_MAX_TEXTURE_SIZE] = 4096
        GlesBackend.onContextCreatedForTest(gl)
    }

    /** Simulates a context loss: same resources, new context on the same fake (ids keep counting). */
    private fun recreateContext() {
        gl.calls.clear()
        GlesBackend.onContextCreatedForTest(gl)
    }

    private fun <T : GpuResource> track(r: T): T {
        created += r
        return r
    }

    private fun pixels(byteCount: Int): ByteBuffer =
        ByteBuffer.allocateDirect(byteCount).order(ByteOrder.nativeOrder())

    @Before
    fun setUp() {
        GlesDebugOptions.simulateEs30 = false
        newContext()
    }

    @After
    fun tearDown() {
        GlesDebugOptions.simulateEs30 = false
        for (r in created) if (!r.isDisposed) r.dispose()
    }

    @Test
    fun texture_restoreReuploadsFromSource() {
        var loads = 0
        val source = TextureSource {
            loads++
            TextureData(4, 4, TextureFormat.RGBA8, pixels(4 * 4 * 4))
        }
        val texture = track(device.createTexture(4, 4, TextureFormat.RGBA8, source = source, label = "t")) as GlesTexture
        val oldId = texture.textureId

        recreateContext()

        assertEquals(2, loads)
        assertTrue(gl.calls.toString(), gl.calls.contains("glTexStorage2D(3553,1,32856,4,4)"))
        assertTrue(gl.calls.toString(), gl.calls.contains("glTexSubImage2D(3553,0,0,0,4,4,6408,5121,buf)"))
        assertNotEquals(oldId, texture.textureId)
        assertEquals(texture.textureId, texture.ownTextureId)
    }

    @Test
    fun texture_sourceFails_bindsPlaceholder() {
        var fail = false
        val source = TextureSource {
            if (fail) null else TextureData(4, 4, TextureFormat.RGBA8, pixels(4 * 4 * 4))
        }
        val texture = track(device.createTexture(4, 4, TextureFormat.RGBA8, source = source, label = "t")) as GlesTexture
        assertNotEquals(GlesPlaceholderTexture.id, texture.textureId)

        fail = true
        recreateContext()

        assertEquals(GlesPlaceholderTexture.id, texture.textureId)
        assertTrue(texture.usesPlaceholder)
    }

    @Test
    fun texture_uploadAfterPlaceholder_recoversOwnTexture() {
        var fail = false
        val source = TextureSource {
            if (fail) null else TextureData(4, 4, TextureFormat.RGBA8, pixels(4 * 4 * 4))
        }
        val texture = track(device.createTexture(4, 4, TextureFormat.RGBA8, source = source, label = "t")) as GlesTexture
        fail = true
        recreateContext()
        assertTrue(texture.usesPlaceholder)
        GlesRenderContext.instance.setTexture(0, texture, null)
        gl.calls.clear()

        texture.upload(0, 0, 0, TextureData(4, 4, TextureFormat.RGBA8, pixels(4 * 4 * 4)))

        assertFalse(texture.usesPlaceholder)
        assertEquals(texture.ownTextureId, texture.textureId)
        assertTrue(gl.calls.toString(), gl.calls.contains("glBindTexture(3553,${texture.ownTextureId})"))
        gl.calls.clear()
        GlesRenderContext.instance.setTexture(0, texture, null)
        assertFalse(gl.calls.toString(), gl.calls.any { it.startsWith("glBindTexture") })
    }

    @Test
    fun texture_sourceThrows_bindsPlaceholder() {
        var fail = false
        val source = TextureSource {
            if (fail) throw RuntimeException("file gone")
            TextureData(4, 4, TextureFormat.RGBA8, pixels(4 * 4 * 4))
        }
        val texture = track(device.createTexture(4, 4, TextureFormat.RGBA8, source = source, label = "t")) as GlesTexture
        assertFalse(texture.usesPlaceholder)

        fail = true
        recreateContext()

        assertTrue(texture.usesPlaceholder)
        assertEquals(GlesPlaceholderTexture.id, texture.textureId)
    }

    @Test
    fun texture_sourceTooShort_bindsPlaceholder() {
        var bytes = 4 * 4 * 4
        val source = TextureSource { TextureData(4, 4, TextureFormat.RGBA8, pixels(bytes)) }
        val texture = track(device.createTexture(4, 4, TextureFormat.RGBA8, source = source, label = "t")) as GlesTexture
        assertFalse(texture.usesPlaceholder)

        bytes = 4 * 4 * 4 - 1
        recreateContext()

        assertTrue(texture.usesPlaceholder)
        assertEquals(GlesPlaceholderTexture.id, texture.textureId)
        assertFalse(gl.calls.toString(), gl.calls.any { it.startsWith("glTexSubImage2D(3553,0,0,0,4,4,") })
    }

    @Test
    fun texture_sourceThrows_laterResourcesStillRestored() {
        var fail = false
        val source = TextureSource {
            if (fail) throw IllegalStateException("decode failed")
            TextureData(4, 4, TextureFormat.RGBA8, pixels(4 * 4 * 4))
        }
        track(device.createTexture(4, 4, TextureFormat.RGBA8, source = source, label = "failing"))
        val later = track(device.createTexture(2, 2, TextureFormat.R8, label = "later")) as GlesTexture
        val buffer = track(device.createBuffer(BufferTarget.Vertex, BufferUsage.Static, 4, "vbo")) as GlesBuffer
        val oldTexture = later.textureId
        val oldBuffer = buffer.bufferId

        fail = true
        recreateContext()

        assertNotEquals(0, later.textureId)
        assertNotEquals(oldTexture, later.textureId)
        assertNotEquals(0, buffer.bufferId)
        assertNotEquals(oldBuffer, buffer.bufferId)
        assertTrue(gl.calls.toString(), gl.calls.contains("glTexStorage2D(3553,1,33321,2,2)"))
    }

    @Test
    fun query_resultNanos_minusOneUntilAvailable() {
        newContext("GL_EXT_disjoint_timer_query")
        val query = track(device.createQuery("q"))
        assertEquals(-1L, query.resultNanos())

        query.begin()
        query.end()
        assertEquals(-1L, query.resultNanos())
        assertTrue(query.isPending)

        gl.queryValues[GLES30.GL_QUERY_RESULT_AVAILABLE] = 1
        gl.queryValues[GLES30.GL_QUERY_RESULT] = 1234
        assertEquals(1234L, query.resultNanos())
        assertFalse(query.isPending)
        assertEquals(-1L, query.resultNanos())
    }

    @Test
    fun query_disjoint_dropsWithMinusOne() {
        newContext("GL_EXT_disjoint_timer_query")
        val query = track(device.createQuery("q"))
        query.begin()
        query.end()
        gl.queryValues[GLES30.GL_QUERY_RESULT_AVAILABLE] = 1
        gl.queryValues[GLES30.GL_QUERY_RESULT] = 1234
        gl.intValues[0x8FBB] = 1

        assertEquals(-1L, query.resultNanos())
        assertFalse(query.isPending)
    }

    @Test
    fun placeholder_isMagentaBlackCheckerboard() {
        val data = GlesPlaceholderTexture.createData()
        assertEquals(8, data.width)
        assertEquals(8, data.height)
        assertEquals(TextureFormat.RGBA8, data.format)
        val p = data.pixels
        // (0,0) magenta, (1,0) black, (0,1) black.
        assertEquals(0xFF.toByte(), p.get(0))
        assertEquals(0x00.toByte(), p.get(1))
        assertEquals(0xFF.toByte(), p.get(2))
        assertEquals(0xFF.toByte(), p.get(3))
        assertEquals(0x00.toByte(), p.get(4))
        assertEquals(0x00.toByte(), p.get(8 * 4))
    }

    @Test
    fun r8_usesRedFormatAndAlignment1() {
        val source = TextureSource { TextureData(8, 8, TextureFormat.R8, pixels(8 * 8)) }
        track(device.createTexture(8, 8, TextureFormat.R8, source = source, label = "r8"))

        val storage = gl.calls.indexOf("glTexStorage2D(3553,1,33321,8,8)")
        val align1 = gl.calls.indexOf("glPixelStorei(3317,1)")
        val upload = gl.calls.indexOf("glTexSubImage2D(3553,0,0,0,8,8,6403,5121,buf)")
        val align4 = gl.calls.lastIndexOf("glPixelStorei(3317,4)")
        assertTrue(gl.calls.toString(), storage >= 0)
        assertTrue(gl.calls.toString(), align1 in (storage + 1) until upload)
        assertTrue(gl.calls.toString(), align4 > upload)
    }

    @Test
    fun textureArray_usesStorage3DAndLayerUpload() {
        val texture = track(device.createTexture(16, 16, TextureFormat.R8, layers = 3, label = "array"))
        texture.upload(2, 4, 5, TextureData(2, 2, TextureFormat.R8, pixels(4)))

        assertTrue(gl.calls.toString(), gl.calls.contains("glTexStorage3D(35866,1,33321,16,16,3)"))
        assertTrue(gl.calls.toString(), gl.calls.contains("glTexSubImage3D(35866,0,4,5,2,2,2,1,6403,5121,buf)"))
    }

    @Test
    fun mipmappedTexture_allocatesLevelsAndGenerates() {
        val source = TextureSource { TextureData(16, 8, TextureFormat.RGBA8, pixels(16 * 8 * 4)) }
        track(device.createTexture(16, 8, TextureFormat.RGBA8, filter = TextureFilter.LinearMipmapLinear, source = source, label = "mip"))

        assertTrue(gl.calls.toString(), gl.calls.contains("glTexStorage2D(3553,5,32856,16,8)"))
        assertTrue(gl.calls.toString(), gl.calls.contains("glGenerateMipmap(3553)"))
    }

    @Test
    fun streamBuffer_restoreAllocatesWithoutData() {
        val buffer = track(device.createBuffer(BufferTarget.Vertex, BufferUsage.Stream, 8, "stream")) as GlesBuffer
        buffer.update(0, pixels(8), 8)

        gl.bufferDataBytes.clear()
        gl.calls.clear()
        recreateContext()

        assertTrue(gl.calls.toString(), gl.calls.contains("glBufferData(34962,8,buf,${GLES30.GL_STREAM_DRAW})"))
        assertEquals(0, gl.bufferDataBytes.size)
        assertNotEquals(0, buffer.bufferId)
    }

    @Test
    fun dynamicBuffer_restoreReuploadsLastData() {
        val buffer = track(device.createBuffer(BufferTarget.Vertex, BufferUsage.Dynamic, 4, "dynamic"))
        val data = pixels(4)
        for (i in 0 until 4) data.put(i, (i + 5).toByte())
        buffer.update(0, data, 4)

        gl.bufferDataBytes.clear()
        recreateContext()

        assertEquals(1, gl.bufferDataBytes.size)
        assertArrayEquals(byteArrayOf(5, 6, 7, 8), gl.bufferDataBytes[0])
    }

    @Test
    fun buffer_restoreReuploadsLastData() {
        val buffer = track(device.createBuffer(BufferTarget.Vertex, BufferUsage.Static, 8, "vbo"))
        val data = pixels(8)
        for (i in 0 until 8) data.put(i, (i + 1).toByte())
        buffer.update(0, data, 8)

        gl.bufferDataBytes.clear()
        recreateContext()

        assertTrue(gl.calls.toString(), gl.calls.contains("glBufferData(34962,8,buf,35044)"))
        assertEquals(1, gl.bufferDataBytes.size)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8), gl.bufferDataBytes[0])
    }

    @Test
    fun buffer_partialFloatUpdate_isKeptAtOffset() {
        val buffer = track(device.createBuffer(BufferTarget.Vertex, BufferUsage.Dynamic, 12, "vbo"))
        val floats = pixels(8).asFloatBuffer()
        floats.put(0, 1.5f)
        floats.put(1, -2f)
        buffer.update(4, floats, 8)
        assertEquals("source position untouched", 0, floats.position())

        gl.bufferDataBytes.clear()
        recreateContext()

        val expected = pixels(12)
        expected.putFloat(4, 1.5f)
        expected.putFloat(8, -2f)
        val bytes = ByteArray(12)
        expected.get(bytes)
        assertArrayEquals(bytes, gl.bufferDataBytes[0])
    }

    @Test
    fun dispose_unregistersAndDeletes() {
        val texture = track(device.createTexture(4, 4, TextureFormat.RGBA8, label = "t"))
        val before = GlesBackend.registry.size

        texture.dispose()

        assertTrue(gl.calls.contains("glDeleteTextures(1,buf,0)"))
        assertEquals(before - 1, GlesBackend.registry.size)
        assertTrue(texture.isDisposed)
    }

    @Test
    fun storageBuffer_onEs30_throws() {
        GlesDebugOptions.simulateEs30 = true
        newContext()

        assertFalse(device.capabilities.supportsStorageBuffers)
        assertThrows(IllegalStateException::class.java) {
            track(device.createBuffer(BufferTarget.Storage, BufferUsage.Static, 16, "ssbo"))
        }
    }

    @Test
    fun program_es32Source_onEs30_throws() {
        GlesDebugOptions.simulateEs30 = true
        newContext()

        assertThrows(IllegalStateException::class.java) {
            track(device.createProgram("#version 320 es\nvoid main() {}", "#version 320 es\nvoid main() {}", "p"))
        }
    }

    @Test
    fun program_restoreRecompiles() {
        val program = track(device.createProgram("#version 300 es\nvoid main() {}", "#version 300 es\nvoid main() {}", "p")) as GlesProgram
        val oldId = program.programId

        recreateContext()

        assertTrue(gl.calls.contains("glLinkProgram(${program.programId})"))
        assertNotEquals(oldId, program.programId)
    }

    @Test
    fun query_withoutTimerExtension_isUnsupported() {
        assertFalse(device.capabilities.supportsTimerQuery)
        assertThrows(IllegalStateException::class.java) { track(device.createQuery("q")) }
    }

    @Test
    fun frameTimer_unsupported_createsNothing() {
        val before = GlesBackend.registry.size
        val timer = GpuFrameTimer(device)

        timer.begin()
        timer.end()
        timer.collect()

        assertFalse(timer.isSupported)
        assertEquals(before, GlesBackend.registry.size)
        assertFalse(gl.calls.any { it.startsWith("glGenQueries") })
    }

    @Test
    fun frameTimer_readsResultAndFiltersImplausible() {
        newContext("GL_EXT_disjoint_timer_query")
        val timer = GpuFrameTimer(device)
        gl.queryValues[GLES30.GL_QUERY_RESULT_AVAILABLE] = 1

        timer.begin()
        timer.end()
        gl.queryValues[GLES30.GL_QUERY_RESULT] = 2_000_000
        assertEquals(2_000_000L, timer.collect())
        assertEquals(1, timer.collectedFrames)
        assertTrue(gl.calls.toString(), gl.calls.any { it.startsWith("glBeginQuery(35007,") })

        timer.begin()
        timer.end()
        gl.queryValues[GLES30.GL_QUERY_RESULT] = -1 // ~2^32 ns
        assertEquals(0L, timer.collect())
        assertEquals(0, timer.collectedFrames)

        timer.dispose()
    }

    @Test
    fun frameTimer_disjointDropsResult() {
        newContext("GL_EXT_disjoint_timer_query")
        val timer = GpuFrameTimer(device)
        gl.queryValues[GLES30.GL_QUERY_RESULT_AVAILABLE] = 1
        gl.queryValues[GLES30.GL_QUERY_RESULT] = 1000

        timer.begin()
        timer.end()
        gl.intValues[0x8FBB] = 1
        assertEquals(0L, timer.collect())
        assertEquals(0, timer.collectedFrames)

        timer.dispose()
    }

    @Test
    fun frameTimer_survivesContextLoss() {
        newContext("GL_EXT_disjoint_timer_query")
        val timer = GpuFrameTimer(device)
        // Fill the ring with pending queries whose results never arrive.
        for (i in 0 until 4) {
            timer.begin()
            timer.end()
        }

        recreateContext()
        gl.queryValues[GLES30.GL_QUERY_RESULT_AVAILABLE] = 1
        gl.queryValues[GLES30.GL_QUERY_RESULT] = 500

        timer.begin()
        timer.end()
        assertEquals(500L, timer.collect())

        timer.dispose()
    }
}
