package org.andengine.entity.text

import org.andengine.entity.sprite.SpriteBatching
import org.andengine.graphics.BlendMode
import org.andengine.graphics.DrawContext
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.opengl.font.Font
import org.andengine.opengl.font.Letter
import org.andengine.opengl.font.StrokeFont

/**
 * Hands an engine [Text] to the [QuadBatcher]: the glyph rectangles and letters are taken from the
 * text's own layout, the color from the text when it is drawn, the blend mode from its blend
 * function. GL thread only.
 */
object TextBatching {

    /**
     * Adds the first [count] letters of [text] to the batch, transformed by the current model-view
     * matrix of [glState] (see [addLetter]). The color is the font color times the text color.
     */
    @JvmStatic
    fun add(text: Text, glState: DrawContext, count: Int) {
        val font = text.font as? Font ?: return
        val letters = text.letters
        val quads = text.letterQuads
        val n = minOf(count, text.letterCount)
        if (n <= 0) return

        val blend = SpriteBatching.batchBlendOf(text)
        val matrix = glState.modelViewGLMatrix
        QuadBatcher.setProjection(glState.projectionGLMatrix)
        val fillColor = fillColor(font, text.red, text.green, text.blue, text.alpha)
        val strokeColor = strokeColor(font, text.red, text.green, text.blue, text.alpha)

        for (i in 0 until n) {
            val letter = letters[i]
            val o = i * Text.LETTER_QUAD_SIZE
            addLetter(font, letter, blend, matrix, quads[o], quads[o + 1], quads[o + 2], quads[o + 3], fillColor, strokeColor)
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
     * transformed by [matrix]. A [StrokeFont] letter adds its fill first and its outline on top of
     * it, so the outline stays fully visible over the fill. [QuadBatcher.setProjection] is up to
     * the caller.
     */
    @JvmStatic
    fun addLetter(
        font: Font, letter: Letter, blend: BlendMode, matrix: FloatArray,
        x0: Float, y0: Float, x1: Float, y1: Float,
        fillColor: Int, strokeColor: Int
    ) {
        val stroke = font as? StrokeFont
        val glyph = letter.mGlyph
        if (glyph != null && (stroke == null || !stroke.isStrokeOnly)) {
            QuadBatcher.addGlyph(glyph, blend, matrix, x0, y0, x1, y1, fillColor)
        }
        if (stroke != null) {
            val outline = letter.mStrokeGlyph
            if (outline != null) QuadBatcher.addGlyph(outline, blend, matrix, x0, y0, x1, y1, strokeColor)
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
