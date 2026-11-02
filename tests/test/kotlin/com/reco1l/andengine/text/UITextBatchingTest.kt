package com.reco1l.andengine.text

import com.osudroid.testing.IDENTITY
import com.osudroid.testing.RecordingGl
import android.opengl.GLES20
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.component.ClearInfo
import com.reco1l.andengine.component.InvalidationFlag
import org.andengine.engine.camera.Camera
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import org.andengine.opengl.font.Font
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
import org.andengine.graphics.shape.ShapeMasks

@RunWith(RobolectricTestRunner::class)
class UITextBatchingTest {

    private lateinit var gl: RecordingGl
    private val glState = DrawContext()
    private val camera = Camera(0f, 0f, 100f, 100f)

    private lateinit var masks: ShapeMasks
    private lateinit var font: Font

    @Before
    fun setUp() {
        gl = RecordingGl().install()
        // No surface size: the glyphs are not rounded to whole pixels, so the corners stay exact.
        GlesBackend.surfaceWidth = 0
        GlesBackend.surfaceHeight = 0
        QuadBatcher.setProjection(IDENTITY)
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

    private fun uploadGlyphs() {
        font.update()
        gl.calls.clear()
        gl.vertexUploads.clear()
    }

    private fun draw(draw: (DrawContext, Camera) -> Unit) {
        glState.resetModelViewGLMatrixStack()
        glState.resetProjectionGLMatrixStack()
        draw(glState, camera)
        QuadBatcher.flush()
    }

    private fun text(content: String): UIText {
        val text = UIText()
        text.font = font
        text.text = content
        text.onHandleInvalidations()
        uploadGlyphs()
        return text
    }

    @Test
    fun uiText_drawsGlyphsThroughBatch() {
        val text = text("AB A")
        draw(text::doDraw)

        assertEquals(1, gl.count("glDrawElements"))
        // The space has no glyph.
        assertEquals(3, gl.lastQuads().size)
    }

    @Test
    fun uiText_contentWidthMatchesAdvances() {
        val text = text("AiB A")

        assertEquals(22f + 14f + 22f + 12f + 22f, text.contentWidth, 0f)
        assertEquals(font.lineHeight, text.contentHeight, 0f)
    }

    @Test
    fun uiText_alignmentAndLines() {
        val text = text("AB\nA")
        text.alignment = Anchor.Center
        text.width = 100f
        text.height = 80f
        draw(text::doDraw)

        val lineHeight = font.lineHeight
        val letter = font.getLetter('A')
        val expected = ArrayList<FloatArray>()
        val widths = floatArrayOf(44f, 22f)
        val rows = arrayOf("AB", "A")
        for (line in rows.indices) {
            var lineX = 100f * 0.5f - widths[line] * 0.5f
            val lineY = 80f * 0.5f - 2 * lineHeight * 0.5f + line * lineHeight
            for (c in rows[line]) {
                val l = font.getLetter(c)
                expected += floatArrayOf(lineX + l.mOffsetX, lineY + l.mOffsetY, lineX + l.mOffsetX + l.mWidth, lineY + l.mOffsetY + l.mHeight)
                lineX += l.mAdvance
            }
        }

        val actual = gl.lastQuads()
        assertEquals(expected.size, actual.size)
        for (i in expected.indices) {
            for (k in 0 until 4) assertEquals("glyph $i value $k", expected[i][k], actual[i][k], 1e-3f)
        }
        // The glyphs keep their own height, they are not stretched to the line.
        assertEquals(letter.mHeight, actual[0][3] - actual[0][1], 1e-3f)
    }

    @Test
    fun fontAwesomeIcon_usesGlyphBoundsCentered() {
        val icon = FontAwesomeIcon('A'.code)
        FontAwesomeIcon::class.java.getDeclaredField("font").apply { isAccessible = true }.set(icon, font)
        icon.invalidate(InvalidationFlag.Content)
        icon.onHandleInvalidations()
        uploadGlyphs()
        icon.width = 30f
        icon.height = 60f

        draw(icon::doDraw)

        val letter = font.getLetter('A')
        val scale = minOf(30f / letter.mAdvance, 60f / font.lineHeight)

        val quad = gl.lastQuads().single()
        assertEquals(15f - letter.mWidth * scale * 0.5f, quad[0], 1e-3f)
        assertEquals(30f - letter.mHeight * scale * 0.5f, quad[1], 1e-3f)
        assertEquals(letter.mWidth * scale, quad[2] - quad[0], 1e-3f)
        assertEquals(letter.mHeight * scale, quad[3] - quad[1], 1e-3f)
    }

    @Test
    fun uiText_clearFlushesPendingQuadsFirst() {
        // The depth mask is off, as a mesh draw without depth writes leaves it.
        GlesBackend.state.depthMask(false)
        val pending = text("AB")
        val clearing = text("A").apply { clearInfo = ClearInfo.ClearDepthBuffer }
        glState.resetModelViewGLMatrixStack()
        glState.resetProjectionGLMatrixStack()
        pending.doDraw(glState, camera)
        assertTrue(QuadBatcher.hasPending)
        gl.calls.clear()

        clearing.doDraw(glState, camera)

        val clear = gl.calls.indexOf("glClear(${GLES20.GL_DEPTH_BUFFER_BIT})")
        assertTrue(gl.calls.toString(), clear >= 0)
        val batchDraw = gl.calls.indexOfFirst { it.startsWith("glDrawElements") }
        assertTrue(gl.calls.toString(), batchDraw in 0 until clear)
        assertTrue(gl.calls.toString(), gl.calls.subList(0, clear).contains("glDepthMask(true)"))
        QuadBatcher.flush()
    }
}
