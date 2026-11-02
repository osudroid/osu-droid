package com.reco1l.andengine.sprite

import android.opengl.GLES20
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.osudroid.testing.Page
import com.osudroid.testing.RecordingGl
import com.osudroid.testing.silentPlayer
import com.osudroid.ui.v2.menu.main.KiaiCatJamSprite
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.component.BlendInfo
import com.reco1l.andengine.component.ClearInfo
import com.reco1l.andengine.component.DepthInfo
import com.reco1l.andengine.container.UIContainer
import com.reco1l.andengine.texture.VideoTexture
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.andengine.engine.camera.Camera
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import org.andengine.opengl.texture.TextureManager
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

@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
class UISpriteBatchingTest {

    private lateinit var gl: RecordingGl
    private val glState = DrawContext()
    private val camera = Camera(0f, 0f, 100f, 100f)

    /**
     * Exposes the buffers so both paths can be compared, and records the buffer path's draw steps
     * instead of running them.
     */
    private class Probe(region: TextureRegion?, private val batchable: Boolean = true) : UISprite(region) {
        val bufferCalls = ArrayList<String>()
        override val isBatchable: Boolean get() = batchable
        override fun beginDraw(pGLState: DrawContext) { bufferCalls += "begin" }
        override fun onDrawBuffer(pGLState: DrawContext) { bufferCalls += "draw" }
        fun position(vertex: Int, axis: Int) = buffer!!.data[vertex * 2 + axis]
        fun uv(vertex: Int, axis: Int) = uvBuffer.data[vertex * 2 + axis]
    }

    private val bufferDraw = listOf("begin", "draw")

    private fun region(page: Page = Page(7)) = TextureRegion(page, 2f, 2f, 8f, 8f)

    private fun batchDraws() = gl.count("glDrawElements")

    /** Whether the GL blend function was set to [src], [dst] for color and alpha alike. */
    @Before
    fun setUp() {
        gl = RecordingGl().install()
        GlesBackend.surfaceWidth = 100
        GlesBackend.surfaceHeight = 100
    }

    @After
    fun tearDown() {
        QuadBatcher.releaseResources()
        endOpenPass()
    }

    @Test
    fun batchableSprite_goesThroughBatcher() {
        val sprite = Probe(region()).apply { setSize(8f, 8f) }
        sprite.onHandleInvalidations()
        gl.calls.clear()

        sprite.doDraw(glState, camera)

        assertTrue(QuadBatcher.hasPending)
        assertTrue(sprite.bufferCalls.isEmpty())
        assertFalse(gl.calls.toString(), gl.calls.any { it.startsWith("glBindTexture") || it.startsWith("glUseProgram") })

        QuadBatcher.flush()

        assertEquals(gl.calls.toString(), 1, batchDraws())
        assertTrue(gl.calls.toString(), gl.calls.contains("glBindTexture(${GLES20.GL_TEXTURE_2D},7)"))
        assertEquals(4 * QuadBatcher.VERTEX_SIZE_BYTES, gl.vertexUploads.last().size)
    }

    @Test
    fun twoSpritesSameTexture_oneDraw() {
        val page = Page(7)
        val a = Probe(region(page)).apply { setSize(8f, 8f); onHandleInvalidations() }
        val b = Probe(TextureRegion(page, 0f, 0f, 2f, 2f)).apply { setSize(2f, 2f); onHandleInvalidations() }

        a.doDraw(glState, camera)
        b.doDraw(glState, camera)
        QuadBatcher.flush()

        assertEquals(gl.calls.toString(), 1, batchDraws())
        assertTrue(a.bufferCalls.isEmpty() && b.bufferCalls.isEmpty())
        assertEquals(2 * 4 * QuadBatcher.VERTEX_SIZE_BYTES, gl.vertexUploads.last().size)
    }

    @Test
    fun nonBatchableSprites_useBufferPath() {
        val video = UIVideoSprite(VideoTexture("video.mp4", silentPlayer()), TextureManager())
        assertFalse(video.isBatchable)
        assertFalse(KiaiCatJamSprite(emptyArray()).isBatchable)

        val sprite = Probe(region(), batchable = false).apply { setSize(8f, 8f); onHandleInvalidations() }
        gl.calls.clear()
        sprite.doDraw(glState, camera)

        assertFalse(QuadBatcher.hasPending)
        assertEquals(bufferDraw, sprite.bufferCalls)
    }

    @Test
    fun depthClearAndUnmappedBlend_useBufferPath() {
        val cases = listOf<Probe.() -> Unit>(
            { depthInfo = DepthInfo.Default },
            { clearInfo = ClearInfo.ClearDepthBuffer },
            // Not a GL blend factor: nothing to map it to.
            { blendInfo = BlendInfo(GLES20.GL_ONE, 0x7FFF) },
        )
        for (configure in cases) {
            val sprite = Probe(region()).apply { setSize(8f, 8f); onHandleInvalidations() }
            sprite.configure()
            gl.calls.clear()

            sprite.doDraw(glState, camera)

            assertFalse(QuadBatcher.hasPending)
            assertEquals(bufferDraw, sprite.bufferCalls)
        }
    }

    @Test
    fun noRegion_usesBufferPath() {
        val sprite = Probe(null).apply { setSize(8f, 8f); onHandleInvalidations() }
        gl.calls.clear()

        sprite.doDraw(glState, camera)

        assertFalse(QuadBatcher.hasPending)
        assertEquals(bufferDraw, sprite.bufferCalls)
    }

    @Test
    fun unloadedTexture_drawsNothing() {
        for (batchable in listOf(true, false)) {
            val sprite = Probe(region(Page(-1)), batchable).apply {
                setSize(8f, 8f)
                onHandleInvalidations()
            }
            gl.calls.clear()

            sprite.doDraw(glState, camera)

            assertFalse(QuadBatcher.hasPending)
            assertTrue(sprite.bufferCalls.isEmpty())
            assertTrue(gl.calls.toString(), gl.calls.isEmpty())
        }
    }

    @Test
    fun flippedSprite_sameCornersAsBuffer() {
        val gravities = listOf(Anchor.TopLeft, Anchor.Center, Anchor.BottomRight)
        // Batch corner order: (x0,y0), (x1,y0), (x1,y1), (x0,y1); buffer order: TL, BL, TR, BR.
        val bufferVertexOfBatchCorner = intArrayOf(0, 2, 3, 1)

        for (flipH in listOf(false, true)) for (flipV in listOf(false, true)) for (scale in ScaleType.entries) for (gravity in gravities) {
            val label = "flipH=$flipH flipV=$flipV scale=$scale gravity=$gravity"
            val sprite = Probe(region()).apply {
                setSize(20f, 10f)
                flippedHorizontal = flipH
                flippedVertical = flipV
                scaleType = scale
                this.gravity = gravity
                onHandleInvalidations()
            }
            gl.vertexUploads.clear()

            sprite.doDraw(glState, camera)
            QuadBatcher.flush()

            val vertices = gl.lastUpload()
            for (corner in 0 until 4) {
                val base = corner * QuadBatcher.VERTEX_SIZE_BYTES
                val vertex = bufferVertexOfBatchCorner[corner]
                assertEquals("$label x of corner $corner", sprite.position(vertex, 0), vertices.getFloat(base), 0f)
                assertEquals("$label y of corner $corner", sprite.position(vertex, 1), vertices.getFloat(base + 4), 0f)
                assertEquals("$label u of corner $corner", sprite.uv(vertex, 0), vertices.getFloat(base + 8), 0f)
                assertEquals("$label v of corner $corner", sprite.uv(vertex, 1), vertices.getFloat(base + 12), 0f)
            }
        }
    }

    @Test
    fun modelView_transformsCorners() {
        val sprite = Probe(region()).apply { setSize(8f, 8f); onHandleInvalidations() }
        glState.pushModelViewGLMatrix()
        glState.translateModelViewGLMatrixf(30f, 40f, 0f)

        sprite.doDraw(glState, camera)
        glState.popModelViewGLMatrix()
        QuadBatcher.flush()

        val vertices = gl.lastUpload()
        assertEquals(30f, vertices.getFloat(0), 0f)
        assertEquals(40f, vertices.getFloat(4), 0f)
        assertEquals(38f, vertices.getFloat(2 * QuadBatcher.VERTEX_SIZE_BYTES), 0f)
        assertEquals(48f, vertices.getFloat(2 * QuadBatcher.VERTEX_SIZE_BYTES + 4), 0f)
    }

    @Test
    fun alphaInheritance_applied() {
        val sprite = Probe(region()).apply {
            setSize(8f, 8f)
            setColor(1f, 0.6f, 0.2f)
            alpha = 0.8f
        }
        UIContainer().apply {
            alpha = 0.5f
            attachChild(sprite)
        }
        sprite.onHandleInvalidations()

        sprite.doDraw(glState, camera)
        QuadBatcher.flush()

        val color = gl.lastUpload().getInt(16)
        val bytes = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder()).putInt(color).array()
        // Memory order R, G, B, A.
        assertEquals(255, bytes[0].toInt() and 0xFF)
        assertEquals(153, bytes[1].toInt() and 0xFF)
        assertEquals(51, bytes[2].toInt() and 0xFF)
        assertEquals(102, bytes[3].toInt() and 0xFF)
    }

    @Test
    fun blendInfo_mapsToTheSameGlBlendFunction() {
        val cases = listOf(
            BlendInfo.Mixture,
            BlendInfo.Additive,
            BlendInfo.PreMultiply,
            BlendInfo(GLES20.GL_SRC_ALPHA, GLES20.GL_DST_ALPHA)
        )
        for (info in cases) {
            val sprite = Probe(region()).apply { setSize(8f, 8f); onHandleInvalidations() }
            sprite.blendInfo = info
            gl.calls.clear()

            sprite.doDraw(glState, camera)
            QuadBatcher.flush()

            val src = info.sourceFactor
            val dst = info.destinationFactor
            assertTrue("$info: ${gl.calls}", gl.setsBlend(src, dst))
        }
    }

    @Test
    fun inheritedBlend_usesParentBlend() {
        val parent = UISprite(region()).apply { blendInfo = BlendInfo.Additive }
        val sprite = Probe(region()).apply { setSize(8f, 8f) }
        parent.attachChild(sprite)
        sprite.onHandleInvalidations()
        sprite.blendInfo = BlendInfo.Inherit
        gl.calls.clear()

        sprite.doDraw(glState, camera)
        QuadBatcher.flush()

        val src = GLES20.GL_SRC_ALPHA
        val dst = GLES20.GL_ONE
        assertTrue(gl.calls.toString(), gl.setsBlend(src, dst))
    }
}
