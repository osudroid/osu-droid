package org.andengine.entity.text

import org.andengine.entity.sprite.SpriteBatching
import org.andengine.entity.text.vbo.HighPerformanceTextVertexBufferObject
import org.andengine.graphics.BlendMode
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.opengl.font.Font
import org.andengine.opengl.font.Letter
import org.andengine.opengl.font.StrokeFont
import org.andengine.opengl.shader.PositionColorTextureCoordinatesShaderProgram
import org.andengine.opengl.util.GLState

/**
 * Decides whether an engine [Text] is drawn through the [QuadBatcher] and hands it over: the
 * glyph rectangles are taken from the text's own vertex data, the slots from its letters, the
 * blend mode from its blend function. GL thread only.
 */
object TextBatching {

    /**
     * True if [text] can be drawn through the batch: its font has glyphs in the shared atlas (no
     * page texture), the default shader, the engine's own vertex buffer and a blend function the
     * batch knows.
     */
    @JvmStatic
    fun canBatch(text: Text): Boolean =
        text.font.texture == null &&
            text.font is Font &&
            text.shaderProgram === PositionColorTextureCoordinatesShaderProgram.getInstance() &&
            text.vertexBufferObject is HighPerformanceTextVertexBufferObject &&
            SpriteBatching.blendOf(text) != null

    /**
     * Adds the first [count] of [letters] (in the order of the text's vertex data) to the batch,
     * transformed by the current model-view matrix of [glState]. A [StrokeFont] letter adds its
     * outline first and its fill on top of it. The color is the font color times the text color.
     */
    @JvmStatic
    fun add(text: Text, glState: GLState, letters: Array<Letter?>, count: Int) {
        val blend = SpriteBatching.blendOf(text) ?: return
        val data = (text.vertexBufferObject as HighPerformanceTextVertexBufferObject).bufferData
        val font = text.font as Font
        val matrix = glState.modelViewGLMatrix
        val n = minOf(count, letters.size)
        if (n <= 0) return

        QuadBatcher.setProjection(glState.projectionGLMatrix)
        val fillColor = fillColor(font, text.red, text.green, text.blue, text.alpha)
        val strokeColor = strokeColor(font, text.red, text.green, text.blue, text.alpha)

        for (i in 0 until n) {
            val letter = letters[i] ?: continue
            val o = i * Text.LETTER_SIZE
            // Vertex 0 is the top-left, vertex 2 the bottom-right corner of the glyph.
            val x0 = data[o + Text.VERTEX_INDEX_X]
            val y0 = data[o + Text.VERTEX_INDEX_Y]
            val x1 = data[o + 2 * Text.VERTEX_SIZE + Text.VERTEX_INDEX_X]
            val y1 = data[o + 2 * Text.VERTEX_SIZE + Text.VERTEX_INDEX_Y]
            addLetter(font, letter, blend, matrix, x0, y0, x1, y1, fillColor, strokeColor)
        }
    }

    /** The fill color of [font] times the given color, packed for [addLetter]. */
    @JvmStatic
    fun fillColor(font: Font, red: Float, green: Float, blue: Float, alpha: Float): Int =
        tint(font.color, red, green, blue, alpha)

    /** The outline color of [font] (0 for a font without outline) times the given color, packed for [addLetter]. */
    @JvmStatic
    fun strokeColor(font: Font, red: Float, green: Float, blue: Float, alpha: Float): Int =
        if (font is StrokeFont) tint(font.strokeColor, red, green, blue, alpha) else 0

    /**
     * Adds one glyph of [letter] covering the local rectangle (x0, y0)-(x1, y1) to the batch,
     * transformed by [matrix]. A [StrokeFont] letter adds its outline first and its fill on top of
     * it. [QuadBatcher.setProjection] is up to the caller.
     */
    @JvmStatic
    fun addLetter(
        font: Font, letter: Letter, blend: BlendMode, matrix: FloatArray,
        x0: Float, y0: Float, x1: Float, y1: Float,
        fillColor: Int, strokeColor: Int
    ) {
        val stroke = font as? StrokeFont
        if (stroke != null) {
            val outline = letter.mStrokeGlyph
            if (outline != null) QuadBatcher.addGlyph(outline, blend, matrix, x0, y0, x1, y1, strokeColor)
        }
        val glyph = letter.mGlyph
        if (glyph != null && (stroke == null || !stroke.isStrokeOnly)) {
            QuadBatcher.addGlyph(glyph, blend, matrix, x0, y0, x1, y1, fillColor)
        }
    }

    /** The ARGB [fontColor] times the given color, packed for the batch. */
    private fun tint(fontColor: Int, red: Float, green: Float, blue: Float, alpha: Float): Int {
        val a = (fontColor ushr 24 and 0xFF) / 255f
        val r = (fontColor ushr 16 and 0xFF) / 255f
        val g = (fontColor ushr 8 and 0xFF) / 255f
        val b = (fontColor and 0xFF) / 255f
        return QuadBatcher.packColor(r * red, g * green, b * blue, a * alpha)
    }
}
