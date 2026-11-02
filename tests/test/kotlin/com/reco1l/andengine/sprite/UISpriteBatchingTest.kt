package com.reco1l.andengine.sprite

import android.opengl.GLES20
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.osudroid.ui.v2.menu.main.KiaiCatJamSprite
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.component.BlendInfo
import com.reco1l.andengine.component.ClearInfo
import com.reco1l.andengine.component.DepthInfo
import com.reco1l.andengine.container.UIContainer
import com.reco1l.andengine.texture.VideoTexture
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.andengine.engine.camera.Camera
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

@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
class UISpriteBatchingTest {

    private val glCalls = ArrayList<String>()
    private val uploads = ArrayList<ByteArray>()
    private var nextId = 100
    private val glState = GLState()
    private val camera = Camera(0f, 0f, 100f, 100f)

    /**
     * Fake GL that records every call as `name(args)`, hands out ids, reports compile and link
     * success and keeps the bytes of every buffer sub-upload (the batch's vertices).
     */
    private fun recordingGl(): GL32 = Proxy.newProxyInstance(GL32::class.java.classLoader, arrayOf(GL32::class.java)) { _, method, args ->
        val name = method.name
        glCalls += "$name(${args?.joinToString(",") { if (it is IntArray || it is java.nio.Buffer) "buf" else "$it" } ?: ""})"
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
            val bytes = ByteArray(minOf(args[1] as Int, copy.remaining()))
            copy.get(bytes)
            uploads += bytes
        } else if (name == "glBufferSubData" && args!![3] is ByteBuffer) {
            val copy = (args[3] as ByteBuffer).duplicate()
            val bytes = ByteArray(minOf(args[2] as Int, copy.remaining()))
            copy.get(bytes)
            uploads += bytes
        }
        when (method.returnType) {
            java.lang.Boolean.TYPE -> false
            java.lang.Integer.TYPE -> if (name.startsWith("glCreate")) nextId++ else 0
            java.lang.Long.TYPE -> 0L
            java.lang.Float.TYPE -> 0f
            String::class.java -> if (args?.get(0) == GLES20.GL_VERSION) "OpenGL ES 3.2 test" else ""
            else -> null
        }
    } as GL32

    private class Page(id: Int, options: TextureOptions = TextureOptions.BILINEAR) : BitmapTextureAtlas(TextureManager(), 16, 16, options) {
        init {
            mHardwareTextureID = id
        }
    }

    /**
     * Exposes the buffers so both paths can be compared, and records the buffer path's draw steps
     * instead of running them.
     */
    private class Probe(region: TextureRegion?, private val batchable: Boolean = true) : UISprite(region) {
        val legacyCalls = ArrayList<String>()
        override val isBatchable: Boolean get() = batchable
        override fun beginDraw(pGLState: GLState) { legacyCalls += "begin" }
        override fun onDrawBuffer(pGLState: GLState) { legacyCalls += "draw" }
        fun position(vertex: Int, axis: Int) = buffer!!.valueAt(vertex * 2 + axis)
        fun uv(vertex: Int, axis: Int) = uvBuffer.valueAt(vertex * 2 + axis)
    }

    private val legacyDraw = listOf("begin", "draw")

    /** A player that ignores every call; nothing is played. */
    private fun silentPlayer(): ExoPlayer =
        Proxy.newProxyInstance(ExoPlayer::class.java.classLoader, arrayOf(ExoPlayer::class.java)) { _, method, _ ->
            when (method.returnType) {
                java.lang.Boolean.TYPE -> false
                java.lang.Integer.TYPE -> 0
                java.lang.Long.TYPE -> 0L
                java.lang.Float.TYPE -> 0f
                java.lang.Double.TYPE -> 0.0
                else -> null
            }
        } as ExoPlayer

    private fun region(page: Page = Page(7)) = TextureRegion(page, 2f, 2f, 8f, 8f)

    private fun batchDraws() = glCalls.count { it.startsWith("glDrawElements") }

    /** Whether the GL blend function was set to [src], [dst] for color and alpha alike. */
    private fun setsBlend(src: Int, dst: Int) =
        glCalls.contains("glBlendFunc($src,$dst)") || glCalls.contains("glBlendFuncSeparate($src,$dst,$src,$dst)")

    /** The vertices of the last batch upload: per vertex x, y, u, v and the color int. */
    private fun lastBatch(): ByteBuffer = ByteBuffer.wrap(uploads.last()).order(ByteOrder.nativeOrder())

    @Before
    fun setUp() {
        GlesBackend.onContextCreatedForTest(recordingGl())
        GlesBackend.surfaceWidth = 100
        GlesBackend.surfaceHeight = 100
        glCalls.clear()
    }

    @After
    fun tearDown() {
        QuadBatcher.releaseResources()
        val context = GlesRenderContext.instance
        if (context.isInPass) context.endPass()
    }

    @Test
    fun batchableSprite_goesThroughBatcher() {
        val sprite = Probe(region()).apply { setSize(8f, 8f) }
        sprite.onHandleInvalidations()
        glCalls.clear()

        sprite.doDraw(glState, camera)

        assertTrue(QuadBatcher.hasPending)
        assertTrue(sprite.legacyCalls.isEmpty())
        assertFalse(glCalls.toString(), glCalls.any { it.startsWith("glBindTexture") || it.startsWith("glUseProgram") })

        QuadBatcher.flush()

        assertEquals(glCalls.toString(), 1, batchDraws())
        assertTrue(glCalls.toString(), glCalls.contains("glBindTexture(${GLES20.GL_TEXTURE_2D},7)"))
        assertEquals(4 * QuadBatcher.VERTEX_SIZE_BYTES, uploads.last().size)
    }

    @Test
    fun twoSpritesSameTexture_oneDraw() {
        val page = Page(7)
        val a = Probe(region(page)).apply { setSize(8f, 8f); onHandleInvalidations() }
        val b = Probe(TextureRegion(page, 0f, 0f, 2f, 2f)).apply { setSize(2f, 2f); onHandleInvalidations() }

        a.doDraw(glState, camera)
        b.doDraw(glState, camera)
        QuadBatcher.flush()

        assertEquals(glCalls.toString(), 1, batchDraws())
        assertTrue(a.legacyCalls.isEmpty() && b.legacyCalls.isEmpty())
        assertEquals(2 * 4 * QuadBatcher.VERTEX_SIZE_BYTES, uploads.last().size)
    }

    @Test
    fun videoSprite_isNotBatchable() {
        val video = UIVideoSprite(VideoTexture("video.mp4", silentPlayer()), TextureManager())
        assertFalse(video.isBatchable)
        assertFalse(KiaiCatJamSprite(emptyArray()).isBatchable)

        val sprite = Probe(region(), batchable = false).apply { setSize(8f, 8f); onHandleInvalidations() }
        glCalls.clear()
        sprite.doDraw(glState, camera)

        assertFalse(QuadBatcher.hasPending)
        assertEquals(legacyDraw, sprite.legacyCalls)
    }

    @Test
    fun depthClearAndUnmappedBlend_useLegacyPath() {
        val cases = listOf<Probe.() -> Unit>(
            { depthInfo = DepthInfo.Default },
            { clearInfo = ClearInfo.ClearDepthBuffer },
            // Not a GL blend factor: nothing to map it to.
            { blendInfo = BlendInfo(GLES20.GL_ONE, 0x7FFF) },
        )
        for (configure in cases) {
            val sprite = Probe(region()).apply { setSize(8f, 8f); onHandleInvalidations() }
            sprite.configure()
            glCalls.clear()

            sprite.doDraw(glState, camera)

            assertFalse(QuadBatcher.hasPending)
            assertEquals(legacyDraw, sprite.legacyCalls)
        }
    }

    @Test
    fun noRegion_usesLegacyPath() {
        val sprite = Probe(null).apply { setSize(8f, 8f); onHandleInvalidations() }
        glCalls.clear()

        sprite.doDraw(glState, camera)

        assertFalse(QuadBatcher.hasPending)
        assertEquals(legacyDraw, sprite.legacyCalls)
    }

    @Test
    fun unloadedTexture_drawsNothing() {
        val sprite = Probe(region(Page(-1))).apply { setSize(8f, 8f); onHandleInvalidations() }
        glCalls.clear()

        sprite.doDraw(glState, camera)

        assertFalse(QuadBatcher.hasPending)
        assertTrue(glCalls.toString(), glCalls.isEmpty())
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
            uploads.clear()

            sprite.doDraw(glState, camera)
            QuadBatcher.flush()

            val vertices = lastBatch()
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

        val vertices = lastBatch()
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

        val color = lastBatch().getInt(16)
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
            glCalls.clear()

            sprite.doDraw(glState, camera)
            QuadBatcher.flush()

            val src = info.sourceFactor
            val dst = info.destinationFactor
            assertTrue("$info: $glCalls", setsBlend(src, dst))
        }
    }

    @Test
    fun inheritedBlend_usesParentBlend() {
        val parent = UISprite(region()).apply { blendInfo = BlendInfo.Additive }
        val sprite = Probe(region()).apply { setSize(8f, 8f) }
        parent.attachChild(sprite)
        sprite.onHandleInvalidations()
        sprite.blendInfo = BlendInfo.Inherit
        glCalls.clear()

        sprite.doDraw(glState, camera)
        QuadBatcher.flush()

        val src = GLES20.GL_SRC_ALPHA
        val dst = GLES20.GL_ONE
        assertTrue(glCalls.toString(), setsBlend(src, dst))
    }
}
