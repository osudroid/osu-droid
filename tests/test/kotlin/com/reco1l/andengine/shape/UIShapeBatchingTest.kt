package com.reco1l.andengine.shape

import android.opengl.GLES20
import com.reco1l.andengine.component.ClearInfo
import com.reco1l.andengine.component.DepthInfo
import com.osudroid.ui.v2.menu.main.Spectrum
import com.reco1l.andengine.component.BlendInfo
import com.reco1l.andengine.container.UIContainer
import com.reco1l.andengine.ui.UIGradientBox
import com.reco1l.andengine.text.UIText
import com.reco1l.framework.math.Vec2
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.nsu.ccfit.zuev.osu.ResourceManager

@RunWith(RobolectricTestRunner::class)
class UIShapeBatchingTest {

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

    /** Legacy box that records its draw steps instead of running them (they call GL past the fake). */
    private class LegacyBox : UIBox() {
        val legacyCalls = ArrayList<String>()
        override fun beginDraw(pGLState: GLState) { legacyCalls += "begin" }
        override fun onDeclarePointers(gl: GLState) { legacyCalls += "pointers" }
        override fun onDrawBuffer(gl: GLState) { legacyCalls += "draw" }
    }

    private val glCalls = ArrayList<String>()
    private val vertexUploads = ArrayList<ByteArray>()
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
        GlesBackend.surfaceWidth = 0
        GlesBackend.surfaceHeight = 0
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

    private fun drawAndFlush(draw: () -> Unit) {
        glState.resetModelViewGLMatrixStack()
        glState.resetProjectionGLMatrixStack()
        draw()
        QuadBatcher.flush()
    }

    private fun box(w: Float = 20f, h: Float = 10f, configure: UIBox.() -> Unit = {}): UIBox {
        val box = UIBox()
        box.setSize(w, h)
        box.configure()
        box.onHandleInvalidations()
        return box
    }

    @Test
    fun uiBox_fill_goesThroughBatcher() {
        val box = box()
        drawAndFlush { box.doDraw(glState, camera) }

        assertEquals(1, draws())
        val quads = lastQuads()
        assertEquals(1, quads.size)
        assertEquals(listOf(0f, 0f, 20f, 10f), quads[0].toList())
        assertNull(box.buffer)
    }

    @Test
    fun uiBox_rounded_nineQuads() {
        val box = box { cornerRadius = 4f }
        drawAndFlush { box.doDraw(glState, camera) }

        assertEquals(1, draws())
        assertEquals(9, lastQuads().size)
    }

    @Test
    fun uiBox_radiusClampedLikeBefore() {
        val box = box(10f, 6f) { cornerRadius = 20f }
        drawAndFlush { box.doDraw(glState, camera) }

        // The first quad is the top-left corner, as large as the clamped radius: half the short side.
        assertEquals(listOf(0f, 0f, 3f, 3f), lastQuads()[0].toList())
    }

    @Test
    fun uiBox_outline_widthInVirtualUnits() {
        FontManager.setRasterScale(2f)
        val box = box(20f, 20f) {
            paintStyle = PaintStyle.Outline
            lineWidth = 2f
        }
        drawAndFlush { box.doDraw(glState, camera) }

        val quads = lastQuads()
        assertEquals(4, quads.size)
        // Top edge: 2 physical pixels are 1 virtual unit.
        assertEquals(listOf(0f, 0f, 20f, 1f), quads[0].toList())
    }

    @Test
    fun uiLine_oneQuad() {
        FontManager.setRasterScale(2f)
        val line = UILine().apply {
            lineWidth = 4f
            fromPoint = Vec2(0f, 5f)
            toPoint = Vec2(10f, 5f)
        }
        line.onHandleInvalidations()
        drawAndFlush { line.doDraw(glState, camera) }

        assertEquals(1, draws())
        val quads = lastQuads()
        assertEquals(1, quads.size)
        // 4 physical pixels are 2 virtual units, centered on y = 5.
        assertEquals(4f, minOf(quads[0][1], quads[0][3]), 1e-4f)
        assertEquals(6f, maxOf(quads[0][1], quads[0][3]), 1e-4f)
        assertNull(line.buffer)
    }

    @Test
    fun uiTriangle_oneQuad() {
        val triangle = UITriangle().apply { setSize(10f, 8f) }
        triangle.onHandleInvalidations()
        drawAndFlush { triangle.doDraw(glState, camera) }

        assertEquals(1, draws())
        assertEquals(1, lastQuads().size)
        assertNull(triangle.buffer)
    }

    @Test
    fun uiTriangle_outline_threeLines() {
        val triangle = UITriangle().apply {
            setSize(10f, 8f)
            paintStyle = PaintStyle.Outline
        }
        triangle.onHandleInvalidations()
        drawAndFlush { triangle.doDraw(glState, camera) }

        assertEquals(1, draws())
        assertEquals(3, lastQuads().size)
    }

    @Test
    fun depthOrClear_usesLegacyPath() {
        val cases = listOf<UIBox.() -> Unit>(
            { depthInfo = DepthInfo.Default },
            { clearInfo = ClearInfo.ClearDepthBuffer },
        )
        for (configure in cases) {
            val box = LegacyBox().apply { setSize(20f, 10f) }
            box.onHandleInvalidations()
            box.configure()
            glCalls.clear()

            box.doDraw(glState, camera)

            assertFalse(QuadBatcher.hasPending)
            assertEquals(listOf("begin", "pointers", "draw"), box.legacyCalls)
            // The buffer is built on demand once the legacy path is needed.
            assertNotNull(box.buffer)
        }
    }

    @Test
    fun zeroAlpha_drawsNothing() {
        val batched = box { alpha = 0f }

        batched.doDraw(glState, camera)

        assertFalse(QuadBatcher.hasPending)
        assertNull(batched.buffer)
    }

    @Test
    fun zeroAlpha_depthMask_stillDrawsLegacy() {
        val cases = listOf<LegacyBox.() -> Unit>(
            { clearInfo = ClearInfo.ClearDepthBuffer },
            { depthInfo = DepthInfo.Default },
        )
        for (configure in cases) {
            val legacy = LegacyBox().apply {
                setSize(20f, 10f)
                alpha = 0f
            }
            legacy.configure()
            legacy.onHandleInvalidations()

            legacy.doDraw(glState, camera)

            assertFalse(QuadBatcher.hasPending)
            assertEquals(listOf("begin", "pointers", "draw"), legacy.legacyCalls)
        }
    }

    private fun circle(size: Float = 20f, configure: UICircle.() -> Unit = {}): UICircle {
        val circle = UICircle()
        circle.setSize(size, size)
        circle.configure()
        circle.onHandleInvalidations()
        return circle
    }

    @Test
    fun uiCircle_full_fourQuads() {
        val circle = circle()
        drawAndFlush { circle.doDraw(glState, camera) }

        assertEquals(1, draws())
        assertEquals(4, lastQuads().size)
        assertNull(circle.buffer)
    }

    @Test
    fun uiCircle_depthMask_usesLegacyPath() {
        val circle = object : UICircle() {
            val legacyCalls = ArrayList<String>()
            override fun beginDraw(pGLState: GLState) { legacyCalls += "begin" }
            override fun onDeclarePointers(gl: GLState) { legacyCalls += "pointers" }
            override fun onDrawBuffer(gl: GLState) { legacyCalls += "draw" }
        }
        circle.setSize(20f, 20f)
        circle.setPortion(0.5f)
        circle.depthInfo = DepthInfo.Default
        circle.onHandleInvalidations()

        circle.doDraw(glState, camera)

        assertFalse(QuadBatcher.hasPending)
        assertEquals(listOf("begin", "pointers", "draw"), circle.legacyCalls)
        assertNotNull(circle.buffer)
    }

    @Test
    fun portionCircle_batchedWithoutBuffer() {
        val circle = circle { setPortion(0.25f) }
        drawAndFlush { circle.doDraw(glState, camera) }

        assertEquals(1, draws())
        assertEquals(UICircle.approximateSegments(20f, 20f) - 1, allQuadCorners().size)
        assertNull(circle.buffer)
    }

    @Test
    fun ellipse_batched() {
        val ellipse = UICircle().apply { setSize(30f, 10f) }
        ellipse.onHandleInvalidations()
        drawAndFlush { ellipse.doDraw(glState, camera) }

        assertEquals(1, draws())
        assertNull(ellipse.buffer)
        val quads = allQuadCorners()
        assertEquals(UICircle.approximateSegments(30f, 10f) - 1, quads.size)
        for (q in quads) {
            // Every triangle starts at the center and stays inside the bounding box.
            assertEquals(15f, q[0][0], 1e-4f); assertEquals(5f, q[0][1], 1e-4f)
            for (c in q) {
                assertTrue(c[0] in -1e-3f..30.001f && c[1] in -1e-3f..10.001f)
            }
        }
    }

    /** The four corners (x, y) of every quad of the last vertex upload. */
    private fun allQuadCorners(): List<List<FloatArray>> {
        val buffer = ByteBuffer.wrap(vertexUploads.last()).order(ByteOrder.nativeOrder())
        val stride = QuadBatcher.VERTEX_SIZE_BYTES
        val quads = ArrayList<List<FloatArray>>()
        while (buffer.remaining() >= stride * 4) {
            quads += List(4) {
                val x = buffer.float
                val y = buffer.float
                buffer.position(buffer.position() + stride - 8)
                floatArrayOf(x, y)
            }
        }
        return quads
    }

    @Test
    fun sector_matchesBufferGeometry() {
        val cases = listOf(
            floatArrayOf(20f, 20f, 0f, 90f),
            floatArrayOf(40f, 24f, 30f, 250f),
            floatArrayOf(30f, 30f, 90f, -45f)
        )
        for (case in cases) {
            val circle = UICircle().apply {
                setSize(case[0], case[1])
                startAngle = case[2]
                endAngle = case[3]
            }
            circle.onHandleInvalidations()
            val segments = UICircle.approximateSegments(case[0], case[1])
            val buffer = UICircle.CircleVertexBuffer(segments, PaintStyle.Fill)
            buffer.update(circle)

            drawAndFlush { circle.doDraw(glState, camera) }
            val quads = allQuadCorners()

            assertEquals(segments - 1, quads.size)
            // Buffer layout: center, then the arc points; fan triangle i is (center, point i, point i + 1).
            for ((i, q) in quads.withIndex()) {
                assertEquals(buffer.valueAt(0), q[0][0], 1e-3f); assertEquals(buffer.valueAt(1), q[0][1], 1e-3f)
                assertEquals(buffer.valueAt(2 * (i + 1)), q[1][0], 1e-3f); assertEquals(buffer.valueAt(2 * (i + 1) + 1), q[1][1], 1e-3f)
                assertEquals(buffer.valueAt(2 * (i + 2)), q[2][0], 1e-3f); assertEquals(buffer.valueAt(2 * (i + 2) + 1), q[2][1], 1e-3f)
            }
            vertexUploads.clear()
        }
    }

    @Test
    fun arc_matchesBufferGeometry() {
        val circle = UICircle().apply {
            setSize(40f, 24f)
            paintStyle = PaintStyle.Outline
            lineWidth = 2f
            startAngle = 30f
            endAngle = 250f
        }
        circle.onHandleInvalidations()
        val segments = UICircle.approximateSegments(40f, 24f)
        val buffer = UICircle.CircleVertexBuffer(segments, PaintStyle.Outline)
        buffer.update(circle)

        drawAndFlush { circle.doDraw(glState, camera) }
        val quads = allQuadCorners()

        assertEquals(segments - 1, quads.size)
        // The strip is centered on the buffer's line strip: the midpoint of inner and outer corner is the point.
        for ((i, q) in quads.withIndex()) {
            assertEquals(buffer.valueAt(2 * i), (q[0][0] + q[1][0]) / 2f, 1e-3f)
            assertEquals(buffer.valueAt(2 * i + 1), (q[0][1] + q[1][1]) / 2f, 1e-3f)
            assertEquals(buffer.valueAt(2 * (i + 1)), (q[3][0] + q[2][0]) / 2f, 1e-3f)
            assertEquals(buffer.valueAt(2 * (i + 1) + 1), (q[3][1] + q[2][1]) / 2f, 1e-3f)
        }
    }

    @Test
    fun uiCircle_outline_ring() {
        val circle = circle { paintStyle = PaintStyle.Outline; lineWidth = 2f }
        drawAndFlush { circle.doDraw(glState, camera) }

        assertEquals(1, draws())
        assertEquals(4, lastQuads().size)
        assertNull(circle.buffer)
    }

    @Test
    fun gradientBox_colorsPerCorner() {
        val box = UIGradientBox(1f, 0f).apply {
            setSize(20f, 10f)
            setColor(1f, 0f, 0f)
        }
        box.onHandleInvalidations()
        drawAndFlush { box.doDraw(glState, camera) }

        assertEquals(1, draws())
        assertEquals(listOf(0f, 0f, 20f, 10f), lastQuads()[0].toList())
        assertNull(box.buffer)

        // Corners are top-left, top-right, bottom-right, bottom-left; the packed color is at byte 16.
        val buffer = ByteBuffer.wrap(vertexUploads.last()).order(ByteOrder.nativeOrder())
        val stride = QuadBatcher.VERTEX_SIZE_BYTES
        val alphas = IntArray(4) { corner -> (buffer.getInt(corner * stride + 16) ushr 24) and 0xFF }
        assertEquals(255, alphas[0])
        assertEquals(0, alphas[1])
        assertEquals(0, alphas[2])
        assertEquals(255, alphas[3])
    }

    @Test
    fun spectrum_barsInOneDraw() {
        val spectrum = Spectrum()
        spectrum.configure(100f, 100f, 50f)
        spectrum.onHandleInvalidations()
        val amplitudes = Spectrum::class.java.getDeclaredField("frequencyAmplitudes")
            .apply { isAccessible = true }.get(spectrum) as FloatArray
        amplitudes[0] = 0.5f
        amplitudes[7] = 0.8f
        amplitudes[9] = 0.001f // below the dead zone

        drawAndFlush { spectrum.doDraw(glState, camera) }

        assertEquals(1, draws())
        // Two live bars in each of the three rounds.
        assertEquals(6, lastQuads().size)
        assertNull(spectrum.buffer)
        assertEquals(BlendInfo.Additive, spectrum.blendInfo)
    }

    @Test
    fun boxesAndText_oneDraw() {
        val container = UIContainer().apply { setSize(100f, 100f) }
        val first = UIBox().apply { setSize(20f, 10f) }
        val text = UIText().apply {
            font = this@UIShapeBatchingTest.font
            this.text = "A"
        }
        val second = UIBox().apply { setSize(20f, 10f) }
        container.attachChild(first)
        container.attachChild(text)
        container.attachChild(second)
        first.onHandleInvalidations()
        text.onHandleInvalidations()
        second.onHandleInvalidations()
        font.update(null)
        glCalls.clear()
        vertexUploads.clear()

        drawAndFlush {
            first.doDraw(glState, camera)
            text.doDraw(glState, camera)
            second.doDraw(glState, camera)
        }

        assertEquals(1, draws())
        // Two boxes and one glyph.
        assertEquals(3, lastQuads().size)
    }
}
