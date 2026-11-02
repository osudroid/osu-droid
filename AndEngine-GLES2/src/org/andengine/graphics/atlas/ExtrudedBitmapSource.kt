package org.andengine.graphics.atlas

import android.graphics.Bitmap
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource
import org.andengine.opengl.texture.atlas.source.BaseTextureAtlasSource

/**
 * Wraps [source] for an atlas page: reports the size including [padding] on every side and loads a
 * bitmap whose border repeats the outermost pixels of the original, so linear filtering at the
 * region edge never samples a neighbouring image. The texture position of this source is the
 * padded corner; the image content starts [padding] pixels further in.
 */
class ExtrudedBitmapSource(
    val source: IBitmapTextureAtlasSource,
    val padding: Int
) : BaseTextureAtlasSource(0, 0, source.textureWidth + 2 * padding, source.textureHeight + 2 * padding),
    IBitmapTextureAtlasSource {

    override fun deepCopy(): ExtrudedBitmapSource {
        val copy = ExtrudedBitmapSource(source.deepCopy(), padding)
        copy.setTextureX(mTextureX)
        copy.setTextureY(mTextureY)
        return copy
    }

    override fun onLoadBitmap(pBitmapConfig: Bitmap.Config): Bitmap? {
        val original = source.onLoadBitmap(pBitmapConfig) ?: return null
        val width = original.width
        val height = original.height
        val pixels = IntArray(width * height)
        original.getPixels(pixels, 0, width, 0, 0, width, height)
        val premultiplied = original.isPremultiplied
        val config = original.config ?: Bitmap.Config.ARGB_8888
        original.recycle()

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

    override fun toString(): String = "${javaClass.simpleName}($source, padding=$padding)"
}
