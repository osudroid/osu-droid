package com.reco1l.andengine.shape

import com.reco1l.andengine.component.ClearInfo
import com.reco1l.andengine.component.DepthInfo
import com.osudroid.ui.v2.menu.main.Spectrum
import com.reco1l.andengine.component.BlendInfo
import com.reco1l.andengine.container.UIContainer
import com.reco1l.andengine.ui.UIGradientBox
import com.reco1l.andengine.text.UIText
import com.reco1l.framework.math.Vec2
import com.osudroid.testing.RecordingGl
import org.andengine.engine.camera.Camera
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.shape.ShapeMasks
import org.andengine.opengl.font.Font
import org.andengine.opengl.font.FontManager
import org.andengine.graphics.DrawContext
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
import com.osudroid.testing.endOpenPass
import com.osudroid.testing.disposeTestFontAndMasks
import com.osudroid.testing.installTestFontAndMasks

@RunWith(RobolectricTestRunner::class)
class UIShapeBatchingTest {

    /** A box that records the steps of its buffer draw instead of running them (they call GL past the fake). */
    private class BufferPathBox : UIBox() {
        val bufferCalls = ArrayList<String>()
        override fun beginDraw(pGLState: DrawContext) { bufferCalls += "begin" }
        override fun onDrawBuffer(gl: DrawContext) { bufferCalls += "draw" }
    }

    private lateinit var gl: RecordingGl
    private val glState = DrawContext()
    private val camera = Camera(0f, 0f, 100f, 100f)

    private lateinit var masks: ShapeMasks
    private lateinit var font: Font

    @Before
    fun setUp() {
        gl = RecordingGl().install()
        GlesBackend.surfaceWidth = 0
        GlesBackend.surfaceHeight = 0
        val fixture = installTestFontAndMasks()
        font = fixture.font
        masks = fixture.masks
        gl.calls.clear()
    }

    @After
    fun tearDown() {
        QuadBatcher.releaseResources()
        endOpenPass()
        disposeTestFontAndMasks(font, masks)
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

        assertEquals(1, gl.count("glDrawElements"))
        val quads = gl.lastQuads()
        assertEquals(1, quads.size)
        assertEquals(listOf(0f, 0f, 20f, 10f), quads[0].toList())
        assertNull(box.buffer)
    }

    @Test
    fun uiBox_rounded_nineQuads() {
        val box = box { cornerRadius = 4f }
        drawAndFlush { box.doDraw(glState, camera) }

        assertEquals(1, gl.count("glDrawElements"))
        assertEquals(9, gl.lastQuads().size)
    }

    @Test
    fun uiBox_radiusClampedToHalfShortSide() {
        val box = box(10f, 6f) { cornerRadius = 20f }
        drawAndFlush { box.doDraw(glState, camera) }

        // The first quad is the top-left corner, as large as the clamped radius: half the short side.
        assertEquals(listOf(0f, 0f, 3f, 3f), gl.lastQuads()[0].toList())
    }

    @Test
    fun uiBox_outline_widthInVirtualUnits() {
        FontManager.setRasterScale(2f)
        val box = box(20f, 20f) {
            paintStyle = PaintStyle.Outline
            lineWidth = 2f
        }
        drawAndFlush { box.doDraw(glState, camera) }

        val quads = gl.lastQuads()
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

        assertEquals(1, gl.count("glDrawElements"))
        val quads = gl.lastQuads()
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

        assertEquals(1, gl.count("glDrawElements"))
        assertEquals(1, gl.lastQuads().size)
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

        assertEquals(1, gl.count("glDrawElements"))
        assertEquals(3, gl.lastQuads().size)
    }

    @Test
    fun depthOrClear_usesBufferPath() {
        val cases = listOf<UIBox.() -> Unit>(
            { depthInfo = DepthInfo.Default },
            { clearInfo = ClearInfo.ClearDepthBuffer },
        )
        for (configure in cases) {
            val box = BufferPathBox().apply { setSize(20f, 10f) }
            box.onHandleInvalidations()
            box.configure()
            gl.calls.clear()

            box.doDraw(glState, camera)

            assertFalse(QuadBatcher.hasPending)
            assertEquals(listOf("begin", "draw"), box.bufferCalls)
            // The buffer is built on demand once the component is drawn through its buffer.
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
    fun zeroAlpha_depthMask_stillUsesBufferPath() {
        val cases = listOf<BufferPathBox.() -> Unit>(
            { clearInfo = ClearInfo.ClearDepthBuffer },
            { depthInfo = DepthInfo.Default },
        )
        for (configure in cases) {
            val box = BufferPathBox().apply {
                setSize(20f, 10f)
                alpha = 0f
            }
            box.configure()
            box.onHandleInvalidations()

            box.doDraw(glState, camera)

            assertFalse(QuadBatcher.hasPending)
            assertEquals(listOf("begin", "draw"), box.bufferCalls)
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

        assertEquals(1, gl.count("glDrawElements"))
        assertEquals(4, gl.lastQuads().size)
        assertNull(circle.buffer)
    }

    @Test
    fun uiCircle_depthMask_usesBufferPath() {
        val circle = object : UICircle() {
            val bufferCalls = ArrayList<String>()
            override fun beginDraw(pGLState: DrawContext) { bufferCalls += "begin" }
            override fun onDrawBuffer(gl: DrawContext) { bufferCalls += "draw" }
        }
        circle.setSize(20f, 20f)
        circle.setPortion(0.5f)
        circle.depthInfo = DepthInfo.Default
        circle.onHandleInvalidations()

        circle.doDraw(glState, camera)

        assertFalse(QuadBatcher.hasPending)
        assertEquals(listOf("begin", "draw"), circle.bufferCalls)
        assertNotNull(circle.buffer)
    }

    @Test
    fun portionCircle_batchedWithoutBuffer() {
        val circle = circle { setPortion(0.25f) }
        drawAndFlush { circle.doDraw(glState, camera) }

        assertEquals(1, gl.count("glDrawElements"))
        assertEquals(UICircle.approximateSegments(20f, 20f) - 1, gl.lastQuadCorners().size)
        assertNull(circle.buffer)
    }

    @Test
    fun ellipse_batched() {
        val ellipse = UICircle().apply { setSize(30f, 10f) }
        ellipse.onHandleInvalidations()
        drawAndFlush { ellipse.doDraw(glState, camera) }

        assertEquals(1, gl.count("glDrawElements"))
        assertNull(ellipse.buffer)
        val quads = gl.lastQuadCorners()
        assertEquals(UICircle.approximateSegments(30f, 10f) - 1, quads.size)
        for (q in quads) {
            // Every triangle starts at the center and stays inside the bounding box.
            assertEquals(15f, q[0][0], 1e-4f)
            assertEquals(5f, q[0][1], 1e-4f)
            for (c in q) {
                assertTrue(c[0] in -1e-3f..30.001f && c[1] in -1e-3f..10.001f)
            }
        }
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
            val quads = gl.lastQuadCorners()

            assertEquals(segments - 1, quads.size)
            // Buffer layout: center, then the arc points; fan triangle i is (center, point i, point i + 1).
            for ((i, q) in quads.withIndex()) {
                assertEquals(buffer.data[0], q[0][0], 1e-3f)
                assertEquals(buffer.data[1], q[0][1], 1e-3f)
                assertEquals(buffer.data[2 * (i + 1)], q[1][0], 1e-3f)
                assertEquals(buffer.data[2 * (i + 1) + 1], q[1][1], 1e-3f)
                assertEquals(buffer.data[2 * (i + 2)], q[2][0], 1e-3f)
                assertEquals(buffer.data[2 * (i + 2) + 1], q[2][1], 1e-3f)
            }
            gl.vertexUploads.clear()
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
        val quads = gl.lastQuadCorners()

        assertEquals(segments - 1, quads.size)
        // The strip is centered on the buffer's line strip: the midpoint of inner and outer corner is the point.
        for ((i, q) in quads.withIndex()) {
            assertEquals(buffer.data[2 * i], (q[0][0] + q[1][0]) / 2f, 1e-3f)
            assertEquals(buffer.data[2 * i + 1], (q[0][1] + q[1][1]) / 2f, 1e-3f)
            assertEquals(buffer.data[2 * (i + 1)], (q[3][0] + q[2][0]) / 2f, 1e-3f)
            assertEquals(buffer.data[2 * (i + 1) + 1], (q[3][1] + q[2][1]) / 2f, 1e-3f)
        }
    }

    @Test
    fun uiCircle_outline_ring() {
        val circle = circle {
            paintStyle = PaintStyle.Outline
            lineWidth = 2f
        }
        drawAndFlush { circle.doDraw(glState, camera) }

        assertEquals(1, gl.count("glDrawElements"))
        assertEquals(4, gl.lastQuads().size)
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

        assertEquals(1, gl.count("glDrawElements"))
        assertEquals(listOf(0f, 0f, 20f, 10f), gl.lastQuads()[0].toList())
        assertNull(box.buffer)

        // Corners are top-left, top-right, bottom-right, bottom-left; the packed color is at byte 16.
        val buffer = gl.lastUpload()
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

        assertEquals(1, gl.count("glDrawElements"))
        // Two live bars in each of the three rounds.
        assertEquals(6, gl.lastQuads().size)
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
        font.update()
        gl.calls.clear()
        gl.vertexUploads.clear()

        drawAndFlush {
            first.doDraw(glState, camera)
            text.doDraw(glState, camera)
            second.doDraw(glState, camera)
        }

        assertEquals(1, gl.count("glDrawElements"))
        // Two boxes and one glyph.
        assertEquals(3, gl.lastQuads().size)
    }
}
