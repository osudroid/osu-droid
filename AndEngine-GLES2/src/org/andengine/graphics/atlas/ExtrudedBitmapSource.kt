package org.andengine.graphics.atlas

import android.graphics.Bitmap
import org.andengine.graphics.texture.Pixels
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource
import org.andengine.opengl.texture.atlas.bitmap.source.IPixelTextureAtlasSource
import org.andengine.opengl.texture.atlas.source.BaseTextureAtlasSource

/**
 * Wraps [source] for an atlas page: reports the size including [padding] on every side and loads a
 * bitmap whose border repeats the outermost pixels of the original, so linear filtering at the
 * region edge never samples a neighbouring image. The texture position of this source is the
 * padded corner; the image content starts [padding] pixels further in.
 *
 * On an RGBA page the image is delivered as bytes ([onLoadPixels]) with the border extruded in the
 * byte buffer, if [source] is an [IPixelTextureAtlasSource] that delivers pixels of the page's variant
 * (straight or premultiplied) and of its declared size. The extrusion copies texels as they are, so it
 * fits either variant. Any other source keeps the bitmap path ([onLoadBitmap]). The source holds no
 * state between loads.
 *
 * With [rotated], the image is turned 90° clockwise on the page (its width and height swap), the way a
 * rotated [org.andengine.opengl.texture.region.TextureRegion] expects it. Rotated images always take
 * the bitmap path.
 */
class ExtrudedBitmapSource @JvmOverloads constructor(
    val source: IBitmapTextureAtlasSource,
    val padding: Int,
    val rotated: Boolean = false
) : BaseTextureAtlasSource(
    0,
    0,
    (if (rotated) source.textureHeight else source.textureWidth) + 2 * padding,
    (if (rotated) source.textureWidth else source.textureHeight) + 2 * padding
), IPixelTextureAtlasSource {

    override fun deepCopy(): ExtrudedBitmapSource {
        val copy = ExtrudedBitmapSource(source.deepCopy(), padding, rotated)
        copy.setTextureX(mTextureX)
        copy.setTextureY(mTextureY)
        return copy
    }

    override fun canLoadPixels(premultiplied: Boolean): Boolean =
        !rotated && source is IPixelTextureAtlasSource && source.canLoadPixels(premultiplied)

    override fun onLoadPixels(): Pixels? = onLoadPixels(false)

    override fun onLoadPixels(premultiplied: Boolean): Pixels? {
        if (rotated || source !is IPixelTextureAtlasSource) return null
        // A null here makes the atlas decode again through onLoadBitmap: a rare fallback, accepted on purpose.
        val image = source.onLoadPixels(premultiplied) ?: return null
        if (image.premultiplied != premultiplied) return null
        if (image.width != source.textureWidth || image.height != source.textureHeight) return null
        if (padding == 0) return image

        val page = PixelPage(image.width + 2 * padding, image.height + 2 * padding)
        page.put(image, padding, padding, padding)
        val composed = page.toPixels()
        page.release()
        return if (premultiplied) Pixels(composed.width, composed.height, composed.data, true) else composed
    }

    override fun onLoadBitmap(pBitmapConfig: Bitmap.Config): Bitmap? {
        val original = source.onLoadBitmap(pBitmapConfig) ?: return null
        var width = original.width
        var height = original.height
        var pixels = IntArray(width * height)
        original.getPixels(pixels, 0, width, 0, 0, width, height)
        val premultiplied = original.isPremultiplied
        val config = original.config ?: Bitmap.Config.ARGB_8888
        original.recycle()

        if (rotated) {
            pixels = rotateClockwise(pixels, width, height)
            width = height.also { height = width }
        }

        val outWidth = width + 2 * padding
        val outHeight = height + 2 * padding
        val extruded = AtlasEdgeExtrusion.extrude(pixels, width, height, padding)

        val out = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        // getPixels() returned unpremultiplied colors; store them back the way the original held them.
        out.isPremultiplied = premultiplied
        out.setPixels(extruded, 0, outWidth, 0, 0, outWidth, outHeight)

        if (config == Bitmap.Config.ARGB_8888) return out
        val converted = out.copy(config, false)
        out.recycle()
        return converted
    }

    override fun toString(): String = "${javaClass.simpleName}($source, padding=$padding, rotated=$rotated)"

    private companion object {
        /** [src] is [width]x[height]; returns it turned 90° clockwise ([height]x[width]). */
        fun rotateClockwise(src: IntArray, width: Int, height: Int): IntArray {
            val out = IntArray(src.size)
            for (y in 0 until height) {
                val row = y * width
                val outX = height - 1 - y
                for (x in 0 until width) {
                    out[x * height + outX] = src[row + x]
                }
            }
            return out
        }
    }
}
