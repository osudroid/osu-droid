package org.andengine.graphics.atlas

import org.andengine.graphics.texture.Pixels
import java.nio.ByteBuffer

/**
 * Composes straight RGBA images into one [width]x[height] block of [Pixels], for example a whole
 * atlas page or a single image with an extruded border. Texels no image covers stay 0, 0, 0, 0.
 *
 * The block lives in one direct buffer that is allocated here and only referenced by this page and
 * the [Pixels] from [toPixels]; it is freed with them, so a page is meant to be built, uploaded and
 * dropped, not kept. After [release], [put] and [toPixels] throw.
 */
class PixelPage(val width: Int, val height: Int) {

    init {
        require(width > 0 && height > 0) { "Invalid page size ${width}x$height" }
    }

    private var buffer: ByteBuffer? = ByteBuffer.allocateDirect(width * height * BYTES_PER_PIXEL)

    /**
     * Copies [pixels] with its top-left corner at ([x], [y]), row by row. With [extrude] > 0 the
     * outermost rows and columns are repeated [extrude] times around the image (corners included),
     * exactly as [AtlasEdgeExtrusion.extrude] does, so the image plus its border must fit the page.
     */
    fun put(pixels: Pixels, x: Int, y: Int, extrude: Int = 0) {
        val page = checkNotNull(buffer) { "PixelPage was released" }
        val imageWidth = pixels.width
        val imageHeight = pixels.height
        require(imageWidth > 0 && imageHeight > 0) { "Empty image ${imageWidth}x$imageHeight" }
        require(extrude >= 0) { "Negative extrusion $extrude" }
        require(x - extrude >= 0 && y - extrude >= 0 && x + imageWidth + extrude <= width && y + imageHeight + extrude <= height) {
            "Image ${imageWidth}x$imageHeight at ($x, $y) with extrusion $extrude does not fit the ${width}x$height page"
        }

        val rowBytes = imageWidth * BYTES_PER_PIXEL
        val pageRowBytes = width * BYTES_PER_PIXEL
        val source = pixels.data.duplicate()
        val base = source.position()
        require(source.remaining() >= rowBytes * imageHeight) { "Pixel data shorter than ${imageWidth}x$imageHeight" }

        val target = page.duplicate()
        for (row in 0 until imageHeight) {
            val start = (y + row) * pageRowBytes + x * BYTES_PER_PIXEL
            source.limit(base + (row + 1) * rowBytes)
            source.position(base + row * rowBytes)
            target.position(start)
            target.put(source)

            if (extrude > 0) {
                // Absolute int access copies the four bytes of a texel as they are, whatever the byte order.
                val last = start + rowBytes - BYTES_PER_PIXEL
                val left = page.getInt(start)
                val right = page.getInt(last)
                for (i in 1..extrude) {
                    page.putInt(start - i * BYTES_PER_PIXEL, left)
                    page.putInt(last + i * BYTES_PER_PIXEL, right)
                }
            }
        }

        if (extrude > 0) {
            // Repeat the first and last extended rows above and below the image.
            val extendedRowBytes = rowBytes + 2 * extrude * BYTES_PER_PIXEL
            val firstRow = y * pageRowBytes + (x - extrude) * BYTES_PER_PIXEL
            val lastRow = firstRow + (imageHeight - 1) * pageRowBytes
            val edge = page.duplicate()
            for (i in 1..extrude) {
                copyRow(edge, firstRow, target, firstRow - i * pageRowBytes, extendedRowBytes)
                copyRow(edge, lastRow, target, lastRow + i * pageRowBytes, extendedRowBytes)
            }
        }
    }

    private fun copyRow(from: ByteBuffer, fromOffset: Int, to: ByteBuffer, toOffset: Int, length: Int) {
        from.limit(fromOffset + length)
        from.position(fromOffset)
        to.position(toOffset)
        to.put(from)
    }

    /** The page as [Pixels], sharing this page's buffer (later [put] calls show in it). */
    fun toPixels(): Pixels {
        val data = checkNotNull(buffer) { "PixelPage was released" }.duplicate()
        data.clear()
        return Pixels(width, height, data)
    }

    /** Drops this page's reference to its buffer; [Pixels] taken before keep it alive until they are dropped. */
    fun release() {
        buffer = null
    }

    private companion object {
        const val BYTES_PER_PIXEL = 4
    }
}
