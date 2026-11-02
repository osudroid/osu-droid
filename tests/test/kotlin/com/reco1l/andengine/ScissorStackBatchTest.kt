package com.reco1l.andengine

import android.opengl.GLES20
import com.reco1l.andengine.component.DepthInfo
import com.reco1l.andengine.component.UIComponent
import com.reco1l.andengine.container.UIContainer
import com.reco1l.andengine.shape.UIBox
import com.reco1l.andengine.text.UIText
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.andengine.engine.camera.Camera
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GL32
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesGraphicsDevice
import org.andengine.graphics.gles.GlesRenderContext
import org.andengine.graphics.shape.ShapeBatching
import org.andengine.graphics.shape.ShapeMasks
import org.andengine.graphics.text.GlyphArray
import org.andengine.graphics.text.GlyphAtlas
import org.andengine.graphics.text.GlyphMetrics
import org.andengine.graphics.text.GlyphRasterizer
import org.andengine.opengl.font.Font
import org.andengine.opengl.font.FontManager
import org.andengine.opengl.util.GLState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.nsu.ccfit.zuev.osu.ResourceManager

@RunWith(RobolectricTestRunner::class)
class ScissorStackBatchTest {

    /** Every visible glyph is 20 x 30 pixels; the space has none. */
    private class FakeRasterizer : GlyphRasterizer {
        override val ascent: Float get() = -40f
        override val descent: Float get() = 10f

        override fun measure(c: Char, out: GlyphMetrics) {
            out.width = if (c == ' ') 0 else 20
            out.height = if (c == ' ') 0 else 30
            out.offsetX = 2f
            out.offsetY = -30f
            out.advance = 22f
        }

        override fun rasterize(c: Char, layer: Int, out: ByteBuffer) {
            for (i in 0 until 20 * 30) out.put(0xFF.toByte())
        }
    }

    private val glCalls = ArrayList<String>()
    private val vertexUploads = ArrayList<ByteArray>()
    private val scissors = ArrayList<List<Int>>()
    private var nextId = 1
    private val glState = GLState()
    private val camera = Camera(0f, 0f, 100f, 100f)

    private lateinit var atlas: GlyphAtlas
    private lateinit var masks: ShapeMasks
    private lateinit var font: Font
    private val fontManager = FontManager()

    private fun fakeGl(): GL32 = Proxy.newProxyInstance(GL32::class.java.classLoader, arrayOf(GL32::class.java)) { _, method, args ->
        glCalls += method.name
        when (method.name) {
            "glScissor" -> scissors += listOf(args!![0] as Int, args[1] as Int, args[2] as Int, args[3] as Int)
            "glGetIntegerv" -> {
                val params = args!![1] as IntArray
                params[args[2] as Int] = when (args[0]) {
                    GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS -> 16
                    GLES20.GL_MAX_TEXTURE_SIZE -> 4096
                    else -> 0
                }
            }
            "glGetShaderiv", "glGetProgramiv" -> (args!![2] as IntArray)[args[3] as Int] = 1
            "glGenBuffers", "glGenTextures", "glGenVertexArrays", "glGenSamplers", "glGenFramebuffers" -> {
                val ids = args!![1] as IntArray
                for (i in 0 until args[0] as Int) ids[args[2] as Int + i] = nextId++
            }
            "glBufferData" -> if (args!![0] == GLES20.GL_ARRAY_BUFFER && args[2] is ByteBuffer) {
                val data = (args[2] as ByteBuffer).duplicate()
                data.position(0)
                val bytes = ByteArray(args[1] as Int)
                data.get(bytes)
                vertexUploads += bytes
            }
            "glBufferSubData" -> if (args!![0] == GLES20.GL_ARRAY_BUFFER) {
                val data = (args[3] as ByteBuffer).duplicate()
                data.position(0)
                val bytes = ByteArray(args[2] as Int)
                data.get(bytes)
                vertexUploads += bytes
            }
        }
        when (method.returnType) {
            java.lang.Boolean.TYPE -> false
            java.lang.Integer.TYPE -> if (method.name == "glCreateShader" || method.name == "glCreateProgram") nextId++ else 0
            java.lang.Long.TYPE -> 0L
            java.lang.Float.TYPE -> 0f
            String::class.java -> if (args?.get(0) == GLES20.GL_VERSION) "OpenGL ES 3.2 test" else ""
            else -> null
        }
    } as GL32

    @Before
    fun setUp() {
        GlesBackend.onContextCreatedForTest(fakeGl())
        GlesBackend.surfaceWidth = 100
        GlesBackend.surfaceHeight = 100
        camera.setSurfaceSize(0, 0, 100, 100)
        FontManager.setRasterScale(1f)

        atlas = GlyphAtlas(GlesGraphicsDevice.instance, 256, 2, 2)
        masks = ShapeMasks(atlas)
        ShapeBatching.masks = masks
        font = Font(fontManager, FakeRasterizer(), atlas)

        // UIText looks up its default font on creation, give the store one so it needs no engine.
        val resources = ResourceManager.getInstance()
        val store = ResourceManager::class.java.getDeclaredField("fontStore").apply { isAccessible = true }.get(resources)
        @Suppress("UNCHECKED_CAST")
        val fonts = store.javaClass.getDeclaredField("fonts").apply { isAccessible = true }.get(store) as MutableMap<String, Font>
        fonts["smallFont"] = font
        glCalls.clear()
    }

    @After
    fun tearDown() {
        while (!ScissorStack.empty()) ScissorStack.pop()
        QuadBatcher.releaseResources()
        val context = GlesRenderContext.instance
        if (context.isInPass) context.endPass()
        ShapeBatching.masks = null

        val arrays = HashSet<GlyphArray>()
        arrays += masks.solid().array
        font.getLetter('A').mGlyph?.let { arrays += it.array }
        masks.dispose()
        for (a in arrays) if (a.hasTexture) a.texture.dispose()
        FontManager.setRasterScale(1f)
    }

    private fun draws() = glCalls.count { it == "glDrawElements" }

    /** Corners of the quads in the last vertex upload: x0, y0, x1, y1 (top-left and bottom-right). */
    private fun lastQuads(): List<FloatArray> {
        val buffer = ByteBuffer.wrap(vertexUploads.last()).order(ByteOrder.nativeOrder())
        val stride = QuadBatcher.VERTEX_SIZE_BYTES
        val quads = ArrayList<FloatArray>()
        while (buffer.remaining() >= stride * 4) {
            val xs = FloatArray(4)
            val ys = FloatArray(4)
            for (corner in 0 until 4) {
                xs[corner] = buffer.float
                ys[corner] = buffer.float
                buffer.position(buffer.position() + stride - 8)
            }
            quads += floatArrayOf(xs[0], ys[0], xs[2], ys[2])
        }
        return quads
    }

    /** Orthographic projection, one unit per surface pixel, y growing downwards. */
    private fun beginFrame() {
        glState.resetModelViewGLMatrixStack()
        glState.resetProjectionGLMatrixStack()
        glState.orthoProjectionGLMatrixf(0f, 100f, 100f, 0f, -1f, 1f)
    }

    private fun box(x: Float, y: Float, w: Float, h: Float) = UIBox().apply {
        setPosition(x, y)
        setSize(w, h)
        onHandleInvalidations()
    }

    private fun clipped(x: Float, y: Float, w: Float, h: Float) = UIContainer().apply {
        setPosition(x, y)
        setSize(w, h)
        clipToBounds = true
    }

    private fun drawFullSurfaceBox() {
        box(0f, 0f, 100f, 100f).doDraw(glState, camera)
        QuadBatcher.flush()
    }

    @Test
    fun nestedClips_intersect() {
        beginFrame()
        ScissorStack.pushScissor(10f, 20f, 50f, 50f)
        ScissorStack.pushScissor(30f, 10f, 50f, 50f)

        assertEquals(listOf(30, 20, 30, 40), scissors.last())

        // GL window pixels have their origin at the bottom, so the clip covers y 40..80 from the top.
        drawFullSurfaceBox()
        assertEquals(1, draws())
        val quad = lastQuads().single()
        assertEquals(30f, minOf(quad[0], quad[2]), 1e-3f)
        assertEquals(60f, maxOf(quad[0], quad[2]), 1e-3f)
        assertEquals(40f, minOf(quad[1], quad[3]), 1e-3f)
        assertEquals(80f, maxOf(quad[1], quad[3]), 1e-3f)
    }

    @Test
    fun pop_restoresOuterClip() {
        beginFrame()
        ScissorStack.pushScissor(0f, 0f, 50f, 50f)
        ScissorStack.pushScissor(0f, 0f, 20f, 20f)
        ScissorStack.pop()
        assertEquals(listOf(0, 0, 50, 50), scissors.last())

        drawFullSurfaceBox()
        val quad = lastQuads().single()
        assertEquals(50f, maxOf(quad[0], quad[2]), 1e-3f)
        assertEquals(50f, maxOf(quad[1], quad[3]) - minOf(quad[1], quad[3]), 1e-3f)

        ScissorStack.pop()
        // With no clip left, quads are not cut anymore.
        vertexUploads.clear()
        drawFullSurfaceBox()
        assertEquals(100f, maxOf(lastQuads().single()[0], lastQuads().single()[2]), 1e-3f)
    }

    @Test
    fun twoClippedContainers_oneDraw() {
        val first = clipped(0f, 0f, 30f, 30f)
        first.attachChild(box(20f, 0f, 30f, 10f))
        first.attachChild(UIText().apply { font = this@ScissorStackBatchTest.font; text = "A"; setPosition(5f, 5f) })
        val second = clipped(60f, 0f, 30f, 30f)
        second.attachChild(box(-10f, 0f, 30f, 10f))
        second.attachChild(UIText().apply { font = this@ScissorStackBatchTest.font; text = "A"; setPosition(5f, 5f) })
        for (container in listOf(first, second)) {
            container.onHandleInvalidations()
            for (i in 0 until container.childCount) (container.getChildByIndex(i) as? UIComponent)?.onHandleInvalidations()
        }
        font.update(null)
        glCalls.clear()
        vertexUploads.clear()

        beginFrame()
        first.onDraw(glState, camera)
        second.onDraw(glState, camera)
        QuadBatcher.flush()

        assertEquals(1, draws())
        val quads = lastQuads()
        assertEquals(4, quads.size)
        for (q in quads) {
            val x0 = minOf(q[0], q[2])
            val x1 = maxOf(q[0], q[2])
            val inFirst = x0 >= -1e-3f && x1 <= 30f + 1e-3f
            val inSecond = x0 >= 60f - 1e-3f && x1 <= 90f + 1e-3f
            assertTrue("Quad ${q.toList()} escapes its clip", inFirst || inSecond)
            assertTrue(maxOf(q[1], q[3]) <= 30f + 1e-3f)
        }
        // The box of the first container is cut from x 20..50 to 20..30, the second from 50..80 to 60..80.
        assertTrue(quads.joinToString { it.toList().toString() }, quads.any { Math.abs(minOf(it[0], it[2]) - 20f) < 1e-3f && Math.abs(maxOf(it[0], it[2]) - 30f) < 1e-3f })
        assertTrue(quads.joinToString { it.toList().toString() }, quads.any { Math.abs(minOf(it[0], it[2]) - 60f) < 1e-3f && Math.abs(maxOf(it[0], it[2]) - 80f) < 1e-3f })
    }

    @Test
    fun legacyDrawInsideClip_keepsGlScissor() {
        val seen = ArrayList<List<Int>?>()
        val legacy = object : UIBox() {
            override fun beginDraw(pGLState: GLState) {}
            override fun onDrawBuffer(gl: GLState) { seen += scissors.lastOrNull() }
        }.apply {
            setSize(20f, 10f)
            depthInfo = DepthInfo.Default
            onHandleInvalidations()
        }
        val container = clipped(10f, 10f, 40f, 40f)
        container.attachChild(legacy)
        container.onHandleInvalidations()
        legacy.onHandleInvalidations()

        beginFrame()
        container.onDraw(glState, camera)

        // The container covers y 10..50 from the top, which is 50..90 above the bottom edge.
        assertEquals(listOf<List<Int>?>(listOf(10, 50, 40, 40)), seen)
    }
}
