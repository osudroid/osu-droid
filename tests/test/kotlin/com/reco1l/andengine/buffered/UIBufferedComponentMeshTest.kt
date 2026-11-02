package com.reco1l.andengine.buffered

import android.opengl.GLES20
import com.reco1l.andengine.component.ClearInfo
import com.reco1l.andengine.component.DepthInfo
import com.reco1l.andengine.shape.PaintStyle
import com.reco1l.andengine.shape.UIBox
import com.reco1l.andengine.sprite.UISprite
import com.reco1l.andengine.ui.UIGradientBox
import com.osudroid.testing.Page
import com.osudroid.testing.RecordingGl
import org.andengine.engine.camera.Camera
import org.andengine.graphics.batch.MeshRenderer
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.graphics.DrawContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.osudroid.testing.endOpenPass

/**
 * Buffered components whose depth, clear or blend settings keep them out of the quad batch are
 * drawn through the [MeshRenderer].
 */
@RunWith(RobolectricTestRunner::class)
class UIBufferedComponentMeshTest {

    private lateinit var gl: RecordingGl
    private val drawContext = DrawContext()
    private val camera = Camera(0f, 0f, 100f, 100f)

    @Before
    fun setUp() {
        gl = RecordingGl().install()
        GlesBackend.surfaceWidth = 100
        GlesBackend.surfaceHeight = 100
        drawContext.resetModelViewGLMatrixStack()
        drawContext.resetProjectionGLMatrixStack()
    }

    @After
    fun tearDown() {
        MeshRenderer.releaseResources()
        QuadBatcher.releaseResources()
        endOpenPass()
    }

    private fun draws() = gl.count("glDrawArrays")

    private fun depthMaskedBox(configure: UIBox.() -> Unit = {}) = UIBox().apply {
        setSize(20f, 10f)
        depthInfo = DepthInfo(test = true, mask = true, function = GLES20.GL_ALWAYS)
        configure()
        onHandleInvalidations()
    }

    @Test
    fun depthMaskedBox_drawsThroughMeshRenderer() {
        val box = depthMaskedBox()
        gl.calls.clear()

        box.doDraw(drawContext, camera)

        assertEquals(gl.calls.toString(), 1, draws())
        val draw = gl.calls.indexOfFirst { it.startsWith("glDrawArrays") }
        val depthFunc = gl.calls.indexOf("glDepthFunc(${GLES20.GL_ALWAYS})")
        val depthMask = gl.calls.indexOf("glDepthMask(true)")
        assertTrue(gl.calls.toString(), depthFunc in 0 until draw)
        assertTrue(gl.calls.toString(), depthMask in 0 until draw)
        assertTrue(gl.calls.toString(), gl.calls.contains("glEnable(${GLES20.GL_DEPTH_TEST})"))

        // The fill fan of the buffer, in mesh renderer vertices.
        val vertexCount = box.buffer!!.vertexCount
        assertEquals(vertexCount * MeshRenderer.VERTEX_SIZE_BYTES, gl.vertexUploads.last().size)
        // Later entities are not tested against the depth values the box wrote.
        assertFalse(GlesBackend.state.isDepthTestEnabled)
    }

    @Test
    fun invisibleDepthMask_stillWritesDepth() {
        val box = depthMaskedBox {
            alpha = 0f
            clearInfo = ClearInfo.ClearDepthBuffer
        }
        gl.calls.clear()

        box.doDraw(drawContext, camera)

        assertEquals(gl.calls.toString(), 1, draws())
        val draw = gl.calls.indexOfFirst { it.startsWith("glDrawArrays") }
        assertTrue(gl.calls.toString(), gl.calls.subList(0, draw).contains("glDepthMask(true)"))
        // Fully transparent: only the depth buffer sees the box.
        assertEquals(0, gl.lastUpload().get(8 + 3).toInt())
    }

    @Test
    fun clearDepth_setsDepthMaskFirst() {
        // The depth mask is off, as a mesh draw without depth writes leaves it.
        GlesBackend.state.depthMask(false)
        val box = depthMaskedBox { clearInfo = ClearInfo.ClearDepthBuffer }
        // A batched box is pending; the clear must not overtake it.
        val batched = UIBox().apply { setSize(5f, 5f); onHandleInvalidations() }
        batched.doDraw(drawContext, camera)
        assertTrue(QuadBatcher.hasPending)
        gl.calls.clear()

        box.doDraw(drawContext, camera)

        val clear = gl.calls.indexOf("glClear(${GLES20.GL_DEPTH_BUFFER_BIT})")
        assertTrue(gl.calls.toString(), clear >= 0)
        assertTrue(gl.calls.toString(), gl.calls.subList(0, clear).contains("glDepthMask(true)"))
        val batchDraw = gl.calls.indexOfFirst { it.startsWith("glDrawElements") }
        assertTrue(gl.calls.toString(), batchDraw in 0 until clear)
        assertEquals(gl.calls.toString(), 1, draws())
    }

    @Test
    fun outlineBox_keepsLineWidth() {
        val box = depthMaskedBox {
            paintStyle = PaintStyle.Outline
            lineWidth = 3f
        }
        gl.calls.clear()

        box.doDraw(drawContext, camera)

        val draw = gl.calls.indexOfFirst { it.startsWith("glDrawArrays(${GLES20.GL_LINE_STRIP}") }
        assertTrue(gl.calls.toString(), draw >= 0)
        assertTrue(gl.calls.toString(), gl.calls.subList(0, draw).contains("glLineWidth(3.0)"))
    }

    @Test
    fun depthSprite_drawsTextured() {
        val region = TextureRegion(Page(7), 2f, 2f, 8f, 8f)
        val sprite = UISprite(region).apply {
            setSize(8f, 8f)
            depthInfo = DepthInfo(test = true, mask = true, function = GLES20.GL_EQUAL)
            onHandleInvalidations()
        }
        gl.calls.clear()

        sprite.doDraw(drawContext, camera)

        assertFalse(QuadBatcher.hasPending)
        assertEquals(gl.calls.toString(), 1, draws())
        assertTrue(gl.calls.toString(), gl.calls.contains("glBindTexture(${GLES20.GL_TEXTURE_2D},7)"))
        assertTrue(gl.calls.toString(), gl.calls.contains("glDepthFunc(${GLES20.GL_EQUAL})"))
        assertEquals(4 * MeshRenderer.TEXTURED_VERTEX_SIZE_BYTES, gl.vertexUploads.last().size)
        // Vertex 0 is the top left corner with the region's first texture coordinates.
        val upload = gl.lastUpload()
        assertEquals(region.u, upload.getFloat(8), 1e-6f)
        assertEquals(region.v, upload.getFloat(12), 1e-6f)
        // Vertex 3 is the bottom right corner.
        assertEquals(region.u2, upload.getFloat(3 * MeshRenderer.TEXTURED_VERTEX_SIZE_BYTES + 8), 1e-6f)
        assertEquals(region.v2, upload.getFloat(3 * MeshRenderer.TEXTURED_VERTEX_SIZE_BYTES + 12), 1e-6f)
    }

    @Test
    fun gradientWithDepth_drawsColored() {
        val gradient = UIGradientBox(fromAlpha = 1f, toAlpha = 0f).apply {
            setSize(20f, 10f)
            depthInfo = DepthInfo(test = true, mask = true, function = GLES20.GL_ALWAYS)
            onHandleInvalidations()
        }
        gl.calls.clear()

        gradient.doDraw(drawContext, camera)

        assertEquals(gl.calls.toString(), 1, draws())
        assertTrue(gl.calls.toString(), gl.calls.any { it.startsWith("glDrawArrays(${GLES20.GL_TRIANGLE_STRIP}") })
        assertEquals(4 * MeshRenderer.VERTEX_SIZE_BYTES, gl.vertexUploads.last().size)
        val upload = gl.lastUpload()
        // Left edge opaque, right edge transparent (alpha is the last color byte).
        assertEquals(255, upload.get(8 + 3).toInt() and 0xFF)
        assertEquals(0, upload.get(2 * MeshRenderer.VERTEX_SIZE_BYTES + 8 + 3).toInt() and 0xFF)
    }

    /** Records which draw steps of the buffer contract run. */
    private class RecordingBuffer(private val calls: MutableList<String>) : IBuffer {
        override var sharingMode = BufferSharingMode.Off
        override fun drawMesh(gl: DrawContext, entity: UIBufferedComponent<*>) { calls += "drawMesh" }
    }

    private class RecordingComponent : UIBufferedComponent<RecordingBuffer>() {
        val calls = ArrayList<String>()
        override fun onCreateBuffer() = RecordingBuffer(calls)
        override fun onUpdateBuffer() = Unit
    }

    @Test
    fun bufferedComponent_drawsBufferThroughMesh() {
        val component = RecordingComponent().apply {
            setSize(10f, 10f)
            depthInfo = DepthInfo(test = true, mask = true, function = GLES20.GL_EQUAL)
            onHandleInvalidations()
        }
        gl.calls.clear()

        component.doDraw(drawContext, camera)

        // The buffer is drawn once through the mesh renderer; the component binds no state of its own.
        assertEquals(listOf("drawMesh"), component.calls)
        assertFalse(gl.calls.toString(), gl.calls.any { it.startsWith("glBindBuffer") || it.startsWith("glVertexAttribPointer") })
        assertFalse(GlesBackend.state.isDepthTestEnabled)
    }
}
