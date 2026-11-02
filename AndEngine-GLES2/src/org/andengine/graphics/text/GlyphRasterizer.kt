package org.andengine.graphics.text

import java.nio.ByteBuffer

/** Size and placement of one glyph image, in raster pixels. */
class GlyphMetrics {
    /** Width of the glyph image; 0 for glyphs without pixels. */
    @JvmField var width: Int = 0

    /** Height of the glyph image; 0 for glyphs without pixels. */
    @JvmField var height: Int = 0

    /** Left edge of the image relative to the pen position. */
    @JvmField var offsetX: Float = 0f

    /** Top edge of the image relative to the baseline, usually negative. */
    @JvmField var offsetY: Float = 0f

    /** Horizontal pen advance after this glyph. */
    @JvmField var advance: Float = 0f
}

/**
 * Produces glyph images for a font at a fixed raster size. All values are in raster pixels.
 * Layer 0 is the fill; layer 1 is the stroke, which only stroked fonts provide. Every layer of a
 * glyph has the size and placement reported by [measure].
 */
interface GlyphRasterizer {
    /** Distance from the baseline to the top of the line, negative. */
    val ascent: Float

    /** Distance from the baseline to the bottom of the line, positive. */
    val descent: Float

    fun measure(c: Char, out: GlyphMetrics)

    /**
     * Writes the R8 image of [layer] of [c], width*height bytes row by row without padding, at the
     * current position of [out].
     */
    fun rasterize(c: Char, layer: Int, out: ByteBuffer)
}
