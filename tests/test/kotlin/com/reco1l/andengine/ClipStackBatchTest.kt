package com.reco1l.andengine

import org.andengine.graphics.DepthState
import org.andengine.graphics.ClipStack
import com.reco1l.andengine.component.UIComponent
import com.reco1l.andengine.container.UIContainer
import com.reco1l.andengine.shape.UIBox
import com.reco1l.andengine.text.UIText
import com.osudroid.testing.RecordingGl
import org.andengine.engine.camera.Camera
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.shape.ShapeMasks
import org.andengine.graphics.text.Font
import org.andengine.graphics.DrawContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.osudroid.testing.endOpenPass
import com.osudroid.testing.disposeTestFontAndMasks
import com.osudroid.testing.installTestFontAndMasks

@RunWith(RobolectricTestRunner::class)
class ClipStackBatchTest {

    private lateinit var gl: RecordingGl
    private val drawContext = DrawContext()
    private val camera = Camera(0f, 0f, 100f, 100f)

    private lateinit var masks: ShapeMasks
    private lateinit var font: Font

    @Before
    fun setUp() {
        gl = RecordingGl().install()
        GlesBackend.surfaceWidth = 100
        GlesBackend.surfaceHeight = 100
        camera.setSurfaceSize(0, 0, 100, 100)
        val fixture = installTestFontAndMasks()
        font = fixture.font
        masks = fixture.masks
        gl.calls.clear()
    }

    @After
    fun tearDown() {
        ClipStack.clear()
        QuadBatcher.releaseResources()
        endOpenPass()
        disposeTestFontAndMasks(font, masks)
    }

    /** Orthographic projection, one unit per surface pixel, y growing downwards. */
    private fun beginFrame() {
        drawContext.resetModelViewGLMatrixStack()
        drawContext.resetProjectionGLMatrixStack()
        drawContext.orthoProjectionGLMatrixf(0f, 100f, 100f, 0f, -1f, 1f)
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
        box(0f, 0f, 100f, 100f).doDraw(drawContext, camera)
        QuadBatcher.flush()
    }

    @Test
    fun nestedClips_intersect() {
        beginFrame()
        ClipStack.push(10f, 20f, 50f, 50f)
        ClipStack.push(30f, 10f, 50f, 50f)

        assertEquals(listOf(30, 20, 30, 40), gl.scissors.last())

        // GL window pixels have their origin at the bottom, so the clip covers y 40..80 from the top.
        drawFullSurfaceBox()
        assertEquals(1, gl.count("glDrawElements"))
        val quad = gl.lastQuads().single()
        assertEquals(30f, minOf(quad[0], quad[2]), 1e-3f)
        assertEquals(60f, maxOf(quad[0], quad[2]), 1e-3f)
        assertEquals(40f, minOf(quad[1], quad[3]), 1e-3f)
        assertEquals(80f, maxOf(quad[1], quad[3]), 1e-3f)
    }

    @Test
    fun pop_restoresOuterClip() {
        beginFrame()
        ClipStack.push(0f, 0f, 50f, 50f)
        ClipStack.push(0f, 0f, 20f, 20f)
        ClipStack.pop()
        assertEquals(listOf(0, 0, 50, 50), gl.scissors.last())

        drawFullSurfaceBox()
        val quad = gl.lastQuads().single()
        assertEquals(50f, maxOf(quad[0], quad[2]), 1e-3f)
        assertEquals(50f, maxOf(quad[1], quad[3]) - minOf(quad[1], quad[3]), 1e-3f)

        ClipStack.pop()
        // With no clip left, quads are not cut anymore.
        gl.vertexUploads.clear()
        drawFullSurfaceBox()
        assertEquals(100f, maxOf(gl.lastQuads().single()[0], gl.lastQuads().single()[2]), 1e-3f)
    }

    @Test
    fun twoClippedContainers_oneDraw() {
        val first = clipped(0f, 0f, 30f, 30f)
        first.attachChild(box(20f, 0f, 30f, 10f))
        first.attachChild(UIText().apply { font = this@ClipStackBatchTest.font; text = "A"; setPosition(5f, 5f) })
        val second = clipped(60f, 0f, 30f, 30f)
        second.attachChild(box(-10f, 0f, 30f, 10f))
        second.attachChild(UIText().apply { font = this@ClipStackBatchTest.font; text = "A"; setPosition(5f, 5f) })
        for (container in listOf(first, second)) {
            container.onHandleInvalidations()
            for (i in 0 until container.childCount) (container.getChildByIndex(i) as? UIComponent)?.onHandleInvalidations()
        }
        font.update()
        gl.calls.clear()
        gl.vertexUploads.clear()

        beginFrame()
        first.onDraw(drawContext, camera)
        second.onDraw(drawContext, camera)
        QuadBatcher.flush()

        assertEquals(1, gl.count("glDrawElements"))
        val quads = gl.lastQuads()
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
    fun bufferPathDrawInsideClip_seesGlScissor() {
        val seen = ArrayList<List<Int>?>()
        val buffered = object : UIBox() {
            override fun beginDraw(drawContext: DrawContext) {}
            override fun onDrawBuffer(gl: DrawContext) { seen += this@ClipStackBatchTest.gl.scissors.lastOrNull() }
        }.apply {
            setSize(20f, 10f)
            depthState = DepthState.Less
            onHandleInvalidations()
        }
        val container = clipped(10f, 10f, 40f, 40f)
        container.attachChild(buffered)
        container.onHandleInvalidations()
        buffered.onHandleInvalidations()

        beginFrame()
        container.onDraw(drawContext, camera)

        // The container covers y 10..50 from the top, which is 50..90 above the bottom edge.
        assertEquals(listOf<List<Int>?>(listOf(10, 50, 40, 40)), seen)
    }
}
