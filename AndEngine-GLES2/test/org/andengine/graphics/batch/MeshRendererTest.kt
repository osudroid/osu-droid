package org.andengine.graphics.batch

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.andengine.graphics.BlendMode
import org.andengine.graphics.CompareFunction
import org.andengine.graphics.PrimitiveType
import org.andengine.graphics.TextureFormat
import org.andengine.graphics.debug.RenderStats
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesDebugOptions
import org.andengine.graphics.gles.GlesGraphicsDevice
import org.andengine.graphics.gles.GlesRenderContext
import org.andengine.graphics.gles.GlesLegacyTextureView
import org.andengine.graphics.gles.GlesTexture
import org.andengine.graphics.gles.RecordingGL
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MeshRendererTest {

    private lateinit var gl: RecordingGL
    private lateinit var texture: GlesTexture

    private val identity = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    private fun draws(mode: Int) = gl.calls.count { it.startsWith("glDrawArrays($mode,") }

    private fun lastVertices(): ByteBuffer =
        ByteBuffer.wrap(gl.vertexUploads.last()).order(ByteOrder.nativeOrder())

    @Before
    fun setUp() {
        GlesDebugOptions.simulateEs30 = false
        gl = RecordingGL()
        gl.intValues[GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS] = 16
        gl.intValues[GLES20.GL_MAX_TEXTURE_SIZE] = 4096
        GlesBackend.onContextCreatedForTest(gl)
        GlesBackend.surfaceWidth = 800
        GlesBackend.surfaceHeight = 480
        texture = GlesGraphicsDevice.instance.createTexture(4, 4, TextureFormat.RGBA8, label = "t") as GlesTexture
        QuadBatcher.setProjection(identity)
        gl.calls.clear()
    }

    @After
    fun tearDown() {
        MeshRenderer.releaseResources()
        QuadBatcher.releaseResources()
        val context = GlesRenderContext.instance
        if (context.isInPass) context.endPass()
        texture.dispose()
        RenderStats.enabled = false
    }

    private fun draw(
        primitive: PrimitiveType = PrimitiveType.Triangles,
        positions: FloatArray = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f),
        count: Int = positions.size / 2,
        matrix: FloatArray = identity,
        rgba: Int = -1,
        depth: CompareFunction? = null,
        write: Boolean = false
    ) = MeshRenderer.draw(primitive, positions, 0, count, matrix, identity, rgba, BlendMode.Alpha, depth, write)

    @Test
    fun draw_transformsAndUploadsPositions() {
        val matrix = identity.copyOf()
        matrix[0] = 2f
        matrix[5] = 3f
        matrix[12] = 10f
        matrix[13] = 20f
        draw(positions = floatArrayOf(9f, 9f, 1f, 1f, 2f, 0.5f), matrix = matrix, count = 2, rgba = 0x44332211)
        val bytes = lastVertices()
        assertEquals(2 * MeshRenderer.VERTEX_SIZE_BYTES, bytes.capacity())
        assertEquals(2f * 9f + 10f, bytes.getFloat(0), 1e-5f)
        assertEquals(3f * 9f + 20f, bytes.getFloat(4), 1e-5f)
        assertEquals(12f, bytes.getFloat(12), 1e-5f)
        assertEquals(23f, bytes.getFloat(16), 1e-5f)
        // In memory the bytes are R, G, B, A of the ABGR int.
        assertEquals(0x11, bytes.get(8).toInt() and 0xFF)
        assertEquals(0x44, bytes.get(11).toInt() and 0xFF)
        assertEquals(1, draws(GLES20.GL_TRIANGLES))
        assertTrue(gl.uniformMatrices.isNotEmpty())
    }

    @Test
    fun draw_readsFromOffset() {
        MeshRenderer.draw(
            PrimitiveType.Triangles, floatArrayOf(7f, 7f, 1f, 2f, 3f, 4f, 5f, 6f), 2, 3,
            identity, identity, -1, BlendMode.Alpha, null, false
        )
        val bytes = lastVertices()
        assertEquals(1f, bytes.getFloat(0), 0f)
        assertEquals(6f, bytes.getFloat(2 * 12 + 4), 0f)
    }

    @Test
    fun drawColored_packsPerVertexColor() {
        val vertices = floatArrayOf(
            0f, 0f, 1f, 0f, 0f, 1f,
            1f, 0f, 0f, 1f, 0f, 0.5f,
            0f, 1f, 0f, 0f, 2f, -1f
        )
        MeshRenderer.drawColored(PrimitiveType.Triangles, vertices, 0, 3, identity, identity, BlendMode.Alpha, null, false)
        val bytes = lastVertices()
        fun color(v: Int) = (0 until 4).map { bytes.get(v * 12 + 8 + it).toInt() and 0xFF }
        assertEquals(listOf(255, 0, 0, 255), color(0))
        assertEquals(listOf(0, 255, 0, 128), color(1))
        assertEquals(listOf(0, 0, 255, 0), color(2))
        assertEquals(1f, bytes.getFloat(12), 0f)
    }

    @Test
    fun meshDraw_flushesBatchFirst() {
        QuadBatcher.add(texture, BlendMode.Alpha, identity, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, -1)
        assertTrue(QuadBatcher.hasPending)
        draw()
        assertFalse(QuadBatcher.hasPending)
        val quad = gl.calls.indexOfFirst { it.startsWith("glDrawElements") }
        val mesh = gl.calls.indexOfFirst { it.startsWith("glDrawArrays") }
        assertTrue(gl.calls.toString(), quad in 0 until mesh)
    }

    @Test
    fun depthMaskedMesh_usesPipelineDepth() {
        draw(depth = CompareFunction.LessOrEqual, write = true)
        assertTrue(gl.calls.toString(), gl.calls.contains("glEnable(${GLES20.GL_DEPTH_TEST})"))
        assertTrue(gl.calls.contains("glDepthFunc(${GLES20.GL_LEQUAL})"))
        assertTrue(gl.calls.contains("glDepthMask(true)"))
        gl.calls.clear()
        draw(depth = CompareFunction.Greater, write = false)
        assertTrue(gl.calls.contains("glDepthFunc(${GLES20.GL_GREATER})"))
        assertTrue(gl.calls.contains("glDepthMask(false)"))
    }

    @Test
    fun triangleStrip_usesStripPrimitive() {
        draw(primitive = PrimitiveType.TriangleStrip, positions = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f))
        assertEquals(gl.calls.toString(), 1, draws(GLES20.GL_TRIANGLE_STRIP))
        assertTrue(gl.calls.contains("glDrawArrays(${GLES20.GL_TRIANGLE_STRIP},0,4)"))
        assertEquals(0, draws(GLES20.GL_TRIANGLES))
    }

    @Test
    fun pipelines_areCachedPerBlendDepthAndPrimitive() {
        draw()
        draw()
        assertEquals(1, MeshRenderer.pipelineCount)
        draw(depth = CompareFunction.Less, write = true)
        draw(primitive = PrimitiveType.Lines)
        assertEquals(3, MeshRenderer.pipelineCount)
    }

    @Test
    fun buffer_growsOnlyWhenNeeded() {
        draw()
        val initial = MeshRenderer.capacityVertices
        assertEquals(MeshRenderer.INITIAL_CAPACITY_VERTICES, initial)
        val buffers = gl.calls.count { it.startsWith("glGenBuffers") }

        repeat(5) { draw() }
        assertEquals(initial, MeshRenderer.capacityVertices)
        assertEquals(buffers, gl.calls.count { it.startsWith("glGenBuffers") })

        val big = FloatArray((initial + 1) * 2)
        draw(positions = big)
        assertEquals(initial * 2, MeshRenderer.capacityVertices)
        assertEquals((initial + 1) * 12, lastVertices().capacity())

        val after = gl.calls.count { it.startsWith("glGenBuffers") }
        draw(positions = big)
        draw()
        assertEquals(initial * 2, MeshRenderer.capacityVertices)
        assertEquals(after, gl.calls.count { it.startsWith("glGenBuffers") })
    }

    @Test
    fun emptyMesh_drawsNothing() {
        draw(count = 0)
        assertEquals(0, gl.calls.count { it.startsWith("glDrawArrays") })
    }

    @Test
    fun triangleFan_mapsToGlTriangleFan() {
        draw(primitive = PrimitiveType.TriangleFan, positions = floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f))
        assertTrue(gl.calls.toString(), gl.calls.contains("glDrawArrays(${GLES20.GL_TRIANGLE_FAN},0,4)"))
    }

    @Test
    fun lineLoop_mapsToGlLineLoop() {
        draw(primitive = PrimitiveType.LineLoop, positions = floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f))
        assertTrue(gl.calls.toString(), gl.calls.contains("glDrawArrays(${GLES20.GL_LINE_LOOP},0,3)"))
    }

    private fun drawTextured(
        tex: org.andengine.graphics.Texture = texture,
        vertices: FloatArray = floatArrayOf(0f, 0f, 0f, 0f, 1f, 0f, 1f, 0f, 0f, 1f, 0f, 1f),
        matrix: FloatArray = identity,
        rgba: Int = -1
    ) = MeshRenderer.drawTextured(
        PrimitiveType.Triangles, vertices, 0, vertices.size / 4, matrix, identity, tex, rgba, BlendMode.Alpha, null, false
    )

    @Test
    fun drawTextured_uploadsPositionsUvsAndColor() {
        val matrix = identity.copyOf()
        matrix[0] = 2f
        matrix[12] = 10f
        drawTextured(vertices = floatArrayOf(1f, 2f, 0.25f, 0.75f, 3f, 4f, 0.5f, 1f), matrix = matrix, rgba = 0x44332211)
        val bytes = lastVertices()
        assertEquals(2 * 20, bytes.capacity())
        assertEquals(12f, bytes.getFloat(0), 1e-5f)
        assertEquals(2f, bytes.getFloat(4), 1e-5f)
        assertEquals(0.25f, bytes.getFloat(8), 0f)
        assertEquals(0.75f, bytes.getFloat(12), 0f)
        assertEquals(0x11, bytes.get(16).toInt() and 0xFF)
        assertEquals(0x44, bytes.get(19).toInt() and 0xFF)
        assertEquals(16f, bytes.getFloat(20), 1e-5f)
        assertEquals(1f, bytes.getFloat(32), 0f)
        assertEquals(1, draws(GLES20.GL_TRIANGLES))
    }

    @Test
    fun drawTextured_bindsTextureOnUnit0() {
        // The newest texture is the one the cache sees bound; draw with an older one.
        val other = GlesGraphicsDevice.instance.createTexture(4, 4, TextureFormat.RGBA8, label = "o") as GlesTexture
        try {
            gl.calls.clear()
            drawTextured()
            assertTrue(gl.calls.toString(), gl.calls.contains("glBindTexture(${GLES20.GL_TEXTURE_2D},${texture.textureId})"))
            val bind = gl.calls.indexOf("glBindTexture(${GLES20.GL_TEXTURE_2D},${texture.textureId})")
            val lastActiveBeforeBind = gl.calls.subList(0, bind).lastOrNull { it.startsWith("glActiveTexture") }
            assertTrue(lastActiveBeforeBind == null || lastActiveBeforeBind == "glActiveTexture(${GLES20.GL_TEXTURE0})")
            assertEquals(
                "glBindTexture(${GLES20.GL_TEXTURE_2D},${other.textureId})",
                gl.calls.last { it.startsWith("glBindTexture(${GLES20.GL_TEXTURE_2D}") }
            )
            assertEquals("glActiveTexture(${GLES20.GL_TEXTURE0})", gl.calls.lastOrNull { it.startsWith("glActiveTexture") } ?: "glActiveTexture(${GLES20.GL_TEXTURE0})")
        } finally {
            other.dispose()
        }
    }

    @Test
    fun drawTextured_unloadedLegacyTexture_drawsNothing() {
        val view = GlesLegacyTextureView({ -1 }, 16, 16, "legacy")
        QuadBatcher.add(texture, BlendMode.Alpha, identity, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 1f, -1)
        drawTextured(tex = view)
        assertEquals(0, gl.calls.count { it.startsWith("glDrawArrays") })
        assertEquals(0, gl.vertexUploads.size)
    }
}
