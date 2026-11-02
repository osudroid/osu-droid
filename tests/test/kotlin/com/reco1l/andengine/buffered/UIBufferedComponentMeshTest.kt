package com.reco1l.andengine.buffered

import android.opengl.GLES20
import com.reco1l.andengine.component.ClearInfo
import com.reco1l.andengine.component.DepthInfo
import com.reco1l.andengine.shape.PaintStyle
import com.reco1l.andengine.shape.UIBox
import com.reco1l.andengine.sprite.UISprite
import com.reco1l.andengine.ui.UIGradientBox
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.andengine.engine.camera.Camera
import org.andengine.graphics.batch.MeshRenderer
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GL32
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesRenderContext
import org.andengine.opengl.texture.TextureManager
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.opengl.util.GLState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Buffered components whose depth, clear or blend settings keep them out of the quad batch are
 * drawn through the [MeshRenderer].
 */
@RunWith(RobolectricTestRunner::class)
class UIBufferedComponentMeshTest {

    private val glCalls = ArrayList<String>()
    private val uploads = ArrayList<ByteArray>()
    private val createdPrograms = HashSet<Int>()
    private var nextId = 100
    private val glState = GLState()
    private val camera = Camera(0f, 0f, 100f, 100f)

    private fun fakeGl(): GL32 = Proxy.newProxyInstance(GL32::class.java.classLoader, arrayOf(GL32::class.java)) { _, method, args ->
        val name = method.name
        glCalls += "$name(${args?.joinToString(",") { if (it is IntArray || it is FloatArray || it is java.nio.Buffer) "buf" else "$it" } ?: ""})"
        if (name == "glGetIntegerv") {
            val params = args!![1] as IntArray
            params[args[2] as Int] = when (args[0]) {
                GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS -> 16
                GLES20.GL_MAX_TEXTURE_SIZE -> 4096
                else -> 0
            }
        } else if (name.startsWith("glGen") && args?.size == 3 && args[1] is IntArray) {
            val ids = args[1] as IntArray
            for (i in 0 until args[0] as Int) ids[args[2] as Int + i] = nextId++
        } else if ((name == "glGetShaderiv" || name == "glGetProgramiv") && args!![2] is IntArray) {
            (args[2] as IntArray)[args[3] as Int] = 1
        } else if (name == "glBufferData" && args!![0] == GLES20.GL_ARRAY_BUFFER && args[2] is ByteBuffer) {
            val copy = (args[2] as ByteBuffer).duplicate()
            copy.position(0)
            val bytes = ByteArray(minOf(args[1] as Int, copy.remaining()))
            copy.get(bytes)
            uploads += bytes
        } else if (name == "glBufferSubData" && args!![3] is ByteBuffer) {
            val copy = (args[3] as ByteBuffer).duplicate()
            copy.position(0)
            val bytes = ByteArray(minOf(args[2] as Int, copy.remaining()))
            copy.get(bytes)
            uploads += bytes
        }
        when (method.returnType) {
            java.lang.Boolean.TYPE -> false
            java.lang.Integer.TYPE -> if (name.startsWith("glCreate")) {
                val id = nextId++
                if (name == "glCreateProgram") createdPrograms += id
                id
            } else 0
            java.lang.Long.TYPE -> 0L
            java.lang.Float.TYPE -> 0f
            String::class.java -> if (args?.get(0) == GLES20.GL_VERSION) "OpenGL ES 3.2 test" else ""
            else -> null
        }
    } as GL32

    private class Page(id: Int) : BitmapTextureAtlas(TextureManager(), 16, 16, TextureOptions.BILINEAR) {
        init {
            mHardwareTextureID = id
        }
    }

    @Before
    fun setUp() {
        GlesBackend.onContextCreatedForTest(fakeGl())
        GlesBackend.surfaceWidth = 100
        GlesBackend.surfaceHeight = 100
        glState.resetModelViewGLMatrixStack()
        glState.resetProjectionGLMatrixStack()
        glCalls.clear()
    }

    @After
    fun tearDown() {
        MeshRenderer.releaseResources()
        QuadBatcher.releaseResources()
        val context = GlesRenderContext.instance
        if (context.isInPass) context.endPass()
    }

    private fun draws() = glCalls.count { it.startsWith("glDrawArrays") }

    private fun index(call: String) = glCalls.indexOf(call)

    private fun lastUpload(): ByteBuffer = ByteBuffer.wrap(uploads.last()).order(ByteOrder.nativeOrder())

    private fun depthMaskedBox(configure: UIBox.() -> Unit = {}) = UIBox().apply {
        setSize(20f, 10f)
        depthInfo = DepthInfo(test = true, mask = true, function = GLES20.GL_ALWAYS)
        configure()
        onHandleInvalidations()
    }

    @Test
    fun depthMaskedBox_drawsThroughMeshRenderer() {
        val box = depthMaskedBox()
        glCalls.clear()

        box.doDraw(glState, camera)

        assertEquals(glCalls.toString(), 1, draws())
        val draw = glCalls.indexOfFirst { it.startsWith("glDrawArrays") }
        val depthFunc = index("glDepthFunc(${GLES20.GL_ALWAYS})")
        val depthMask = index("glDepthMask(true)")
        assertTrue(glCalls.toString(), depthFunc in 0 until draw)
        assertTrue(glCalls.toString(), depthMask in 0 until draw)
        assertTrue(glCalls.toString(), glCalls.contains("glEnable(${GLES20.GL_DEPTH_TEST})"))

        // Every program in use was made by the engine's render context.
        val used = glCalls.filter { it.startsWith("glUseProgram(") }
            .map { it.removePrefix("glUseProgram(").removeSuffix(")").toInt() }
            .filter { it != 0 }
        assertTrue(glCalls.toString(), used.isNotEmpty())
        assertTrue("$used not in $createdPrograms", createdPrograms.containsAll(used))

        // The fill fan of the buffer, in mesh renderer vertices.
        val vertexCount = box.buffer!!.vertexCount
        assertEquals(vertexCount * MeshRenderer.VERTEX_SIZE_BYTES, uploads.last().size)
        // Later entities are not tested against the depth values the box wrote.
        assertFalse(glState.isDepthTestEnabled)
    }

    @Test
    fun invisibleDepthMask_stillWritesDepth() {
        val box = depthMaskedBox {
            alpha = 0f
            clearInfo = ClearInfo.ClearDepthBuffer
        }
        glCalls.clear()

        box.doDraw(glState, camera)

        assertEquals(glCalls.toString(), 1, draws())
        val draw = glCalls.indexOfFirst { it.startsWith("glDrawArrays") }
        assertTrue(glCalls.toString(), glCalls.subList(0, draw).contains("glDepthMask(true)"))
        // Fully transparent: only the depth buffer sees the box.
        assertEquals(0, lastUpload().get(8 + 3).toInt())
    }

    @Test
    fun clearDepth_setsDepthMaskFirst() {
        // The depth mask is off, as a mesh draw without depth writes leaves it.
        GlesBackend.state.depthMask(false)
        val box = depthMaskedBox { clearInfo = ClearInfo.ClearDepthBuffer }
        // A batched box is pending; the clear must not overtake it.
        val batched = UIBox().apply { setSize(5f, 5f); onHandleInvalidations() }
        batched.doDraw(glState, camera)
        assertTrue(QuadBatcher.hasPending)
        glCalls.clear()

        box.doDraw(glState, camera)

        val clear = index("glClear(${GLES20.GL_DEPTH_BUFFER_BIT})")
        assertTrue(glCalls.toString(), clear >= 0)
        assertTrue(glCalls.toString(), glCalls.subList(0, clear).contains("glDepthMask(true)"))
        val batchDraw = glCalls.indexOfFirst { it.startsWith("glDrawElements") }
        assertTrue(glCalls.toString(), batchDraw in 0 until clear)
        assertEquals(glCalls.toString(), 1, draws())
    }

    @Test
    fun outlineBox_keepsLineWidth() {
        val box = depthMaskedBox {
            paintStyle = PaintStyle.Outline
            lineWidth = 3f
        }
        glCalls.clear()

        box.doDraw(glState, camera)

        val draw = glCalls.indexOfFirst { it.startsWith("glDrawArrays(${GLES20.GL_LINE_STRIP}") }
        assertTrue(glCalls.toString(), draw >= 0)
        assertTrue(glCalls.toString(), glCalls.subList(0, draw).contains("glLineWidth(3.0)"))
    }

    @Test
    fun depthSprite_drawsTextured() {
        val region = TextureRegion(Page(7), 2f, 2f, 8f, 8f)
        val sprite = UISprite(region).apply {
            setSize(8f, 8f)
            depthInfo = DepthInfo(test = true, mask = true, function = GLES20.GL_EQUAL)
            onHandleInvalidations()
        }
        glCalls.clear()

        sprite.doDraw(glState, camera)

        assertFalse(QuadBatcher.hasPending)
        assertEquals(glCalls.toString(), 1, draws())
        assertTrue(glCalls.toString(), glCalls.contains("glBindTexture(${GLES20.GL_TEXTURE_2D},7)"))
        assertTrue(glCalls.toString(), glCalls.contains("glDepthFunc(${GLES20.GL_EQUAL})"))
        assertEquals(4 * MeshRenderer.TEXTURED_VERTEX_SIZE_BYTES, uploads.last().size)
        // Vertex 0 is the top left corner with the region's first texture coordinates.
        val upload = lastUpload()
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
        glCalls.clear()

        gradient.doDraw(glState, camera)

        assertEquals(glCalls.toString(), 1, draws())
        assertTrue(glCalls.toString(), glCalls.any { it.startsWith("glDrawArrays(${GLES20.GL_TRIANGLE_STRIP}") })
        assertEquals(4 * MeshRenderer.VERTEX_SIZE_BYTES, uploads.last().size)
        val upload = lastUpload()
        // Left edge opaque, right edge transparent (alpha is the last color byte).
        assertEquals(255, upload.get(8 + 3).toInt() and 0xFF)
        assertEquals(0, upload.get(2 * MeshRenderer.VERTEX_SIZE_BYTES + 8 + 3).toInt() and 0xFF)
    }

    /** Records which draw steps of the buffer contract run. */
    private class RecordingBuffer(private val calls: MutableList<String>) : IBuffer {
        override var sharingMode = BufferSharingMode.Off
        override fun drawMesh(gl: GLState, entity: UIBufferedComponent<*>) { calls += "drawMesh" }
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
        glCalls.clear()

        component.doDraw(glState, camera)

        // The buffer is drawn once through the mesh renderer; the component binds no state of its own.
        assertEquals(listOf("drawMesh"), component.calls)
        assertFalse(glCalls.toString(), glCalls.any { it.startsWith("glBindBuffer") || it.startsWith("glVertexAttribPointer") })
        assertFalse(glState.isDepthTestEnabled)
    }
}
