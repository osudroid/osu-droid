package com.reco1l.andengine.text

import android.opengl.GLES20
import com.reco1l.andengine.Anchor
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.andengine.engine.camera.Camera
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GL32
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesGraphicsDevice
import org.andengine.graphics.gles.GlesRenderContext
import org.andengine.graphics.text.GlyphArray
import org.andengine.graphics.text.GlyphAtlas
import org.andengine.graphics.text.GlyphMetrics
import org.andengine.graphics.text.GlyphRasterizer
import org.andengine.opengl.font.Font
import org.andengine.opengl.font.FontManager
import org.andengine.opengl.util.GLState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.nsu.ccfit.zuev.osu.ResourceManager
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UITextBatchingTest {

    /** Visible glyphs are 20 pixels wide with advance 22 (14 for 'i'); ' ' has no pixels. */
    private class FakeRasterizer : GlyphRasterizer {
        override val ascent: Float get() = -40f
        override val descent: Float get() = 10f

        override fun measure(c: Char, out: GlyphMetrics) {
            if (c == ' ') {
                out.width = 0
                out.height = 0
                out.offsetX = 0f
                out.offsetY = 0f
                out.advance = 12f
                return
            }
            out.width = 20
            out.height = if (c == 'i') 24 else 30
            out.offsetX = 2f
            out.offsetY = -30f
            out.advance = if (c == 'i') 14f else 22f
        }

        override fun rasterize(c: Char, layer: Int, out: ByteBuffer) {
            val height = if (c == 'i') 24 else 30
            for (i in 0 until 20 * height) out.put(0xFF.toByte())
        }
    }

    private val glCalls = ArrayList<String>()

    /** The bytes of every vertex upload (glBufferData or glBufferSubData on the array buffer), in order. */
    private val vertexUploads = ArrayList<ByteArray>()

    private var nextId = 1
    private val glState = GLState()
    private val camera = Camera(0f, 0f, 100f, 100f)

    private val identity = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

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

    private lateinit var atlas: GlyphAtlas
    private lateinit var font: Font
    private val fontManager = FontManager()

    @Before
    fun setUp() {
        GlesBackend.onContextCreatedForTest(fakeGl())
        // No surface size: the glyphs are not rounded to whole pixels, so the corners stay exact.
        GlesBackend.surfaceWidth = 0
        GlesBackend.surfaceHeight = 0
        QuadBatcher.setProjection(identity)
        atlas = GlyphAtlas(GlesGraphicsDevice.instance, 256, 2, 2)
        font = Font(fontManager, FakeRasterizer(), atlas)

        // UIText looks up its default font on creation, give the store one so it needs no engine.
        val resources = ResourceManager.getInstance()
        val store = ResourceManager::class.java.getDeclaredField("fontStore").apply { isAccessible = true }.get(resources)
        val fonts = store.javaClass.getDeclaredField("fonts").apply { isAccessible = true }.get(store) as MutableMap<String, Font>
        fonts["smallFont"] = font
        glCalls.clear()
    }

    @After
    fun tearDown() {
        QuadBatcher.releaseResources()
        val context = GlesRenderContext.instance
        if (context.isInPass) context.endPass()

        val arrays = HashSet<GlyphArray>()
        for (c in "ABi ") {
            font.getLetter(c).mGlyph?.let { arrays += it.array }
        }
        for (a in arrays) if (a.hasTexture) a.texture.dispose()
    }

    private fun draws() = glCalls.count { it == "glDrawElements" }

    /** The corners of the quads of the last vertex upload: x0, y0, x1, y1 per quad (top-left and bottom-right). */
    private fun lastQuads(): List<FloatArray> {
        val buffer = ByteBuffer.wrap(vertexUploads.last()).order(ByteOrder.nativeOrder())
        val quads = ArrayList<FloatArray>()
        while (buffer.remaining() >= QuadBatcher.GLYPH_VERTEX_SIZE_BYTES * 4) {
            val xs = FloatArray(4)
            val ys = FloatArray(4)
            for (corner in 0 until 4) {
                xs[corner] = buffer.float
                ys[corner] = buffer.float
                buffer.position(buffer.position() + QuadBatcher.GLYPH_VERTEX_SIZE_BYTES - 8)
            }
            quads += floatArrayOf(xs[0], ys[0], xs[2], ys[2])
        }
        return quads
    }

    private fun uploadGlyphs() {
        font.update(null)
        glCalls.clear()
        vertexUploads.clear()
    }

    private fun draw(draw: (GLState, Camera) -> Unit) {
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

        assertEquals(1, draws())
        // The space has no glyph.
        assertEquals(3, lastQuads().size)
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

        val actual = lastQuads()
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
        icon.invalidate(com.reco1l.andengine.component.InvalidationFlag.Content)
        icon.onHandleInvalidations()
        uploadGlyphs()
        icon.width = 30f
        icon.height = 60f

        draw(icon::doDraw)

        val letter = font.getLetter('A')
        val scale = minOf(30f / letter.mAdvance, 60f / font.lineHeight)

        val quad = lastQuads().single()
        assertEquals(15f - letter.mWidth * scale * 0.5f, quad[0], 1e-3f)
        assertEquals(30f - letter.mHeight * scale * 0.5f, quad[1], 1e-3f)
        assertEquals(letter.mWidth * scale, quad[2] - quad[0], 1e-3f)
        assertEquals(letter.mHeight * scale, quad[3] - quad[1], 1e-3f)
    }
}
