package com.osudroid.resources

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.osudroid.BuildSettings
import com.reco1l.framework.paintBitmap
import java.io.IOException
import java.io.InputStream
import org.andengine.graphics.texture.Pixels
import org.andengine.graphics.texture.RawPixelSource
import org.andengine.graphics.texture.StraightPixels
import org.andengine.opengl.texture.atlas.source.BaseTextureAtlasSource
import org.andengine.util.StreamUtils
import org.andengine.util.debug.Debug

/**
 * Loads an image from a file or an asset (whatever [openInputStream] opens) for a texture atlas.
 * A subclass can shrink it while decoding through [sampleSize]. [preload] decodes the image ahead
 * of the upload, and the next load hands out that result.
 */
abstract class StreamBitmapSource(
    textureX: Int,
    textureY: Int,
    textureWidth: Int,
    textureHeight: Int
) : BaseTextureAtlasSource(textureX, textureY, textureWidth, textureHeight), RawPixelSource {

    private var bitmap: Bitmap? = null

    /**
     * Pixels decoded by [preload], handed out once by the next [onLoadPixels] of the same variant
     * ([Pixels.premultiplied]); a load of the other variant drops them.
     */
    private var preloadedPixels: Pixels? = null

    /** Opens the encoded image; every decode opens it anew. */
    @Throws(IOException::class)
    abstract fun openInputStream(): InputStream

    /** The `inSampleSize` every decode uses. */
    protected abstract fun sampleSize(): Int

    /** Names the image in log messages, e.g. "File: <path>". */
    protected abstract fun sourceLabel(): String

    /** The config the bitmap path decodes with when [onLoadBitmap] asks for [requested]. */
    protected open fun bitmapConfig(requested: Bitmap.Config): Bitmap.Config = requested

    /** Same as `preload(false)`: for an atlas with straight alpha. */
    fun preload(): Boolean = preload(false)

    /**
     * Decodes the image ahead of the upload and reports whether it can be decoded. The next load
     * uses the result: as pixels of the given variant (for an atlas that premultiplies alpha if
     * [premultiplied]) when possible, otherwise as a bitmap.
     */
    fun preload(premultiplied: Boolean): Boolean {
        if (!isNoTexturesMode()) {
            val pixels = decodePixels(premultiplied)
            if (pixels != null) {
                synchronized(this) {
                    preloadedPixels = pixels
                }
                return true
            }
        }
        val bmp = onLoadBitmap(Bitmap.Config.ARGB_8888)
        synchronized(this) {
            bitmap = bmp
        }
        return bmp != null
    }

    /**
     * Whether textures are replaced by placeholders ([BuildSettings.NO_TEXTURES_MODE]), which only
     * the bitmap path paints. Tests override it to run the placeholder path.
     */
    protected open fun isNoTexturesMode(): Boolean = BuildSettings.NO_TEXTURES_MODE

    override fun canLoadPixels(premultiplied: Boolean): Boolean = true

    override fun onLoadPixels(): Pixels? = onLoadPixels(false)

    override fun onLoadPixels(premultiplied: Boolean): Pixels? {
        synchronized(this) {
            val pixels = preloadedPixels
            if (pixels != null) {
                preloadedPixels = null
                if (pixels.premultiplied == premultiplied) {
                    return pixels
                }
                // Preloaded for the other variant: dropped, this load decodes anew.
            }
            if (bitmap != null) {
                // Preloaded as a bitmap: the bitmap path uses it.
                return null
            }
        }
        if (isNoTexturesMode()) {
            return null
        }
        return decodePixels(premultiplied)
    }

    private fun decodePixels(premultiplied: Boolean): Pixels? {
        var input: InputStream? = null
        return try {
            input = openInputStream()
            StraightPixels.decode(input, sampleSize(), premultiplied)
        } catch (e: IOException) {
            Debug.e("Failed loading pixels in " + javaClass.simpleName + ". " + sourceLabel(), e)
            null
        } finally {
            StreamUtils.close(input)
        }
    }

    override fun onLoadBitmap(pBitmapConfig: Bitmap.Config): Bitmap? {
        synchronized(this) {
            // Pixels preloaded for the pixel path are of no use here.
            preloadedPixels = null
            val bmp = bitmap
            if (bmp != null) {
                bitmap = null
                return bmp
            }
        }
        val decodeOptions = BitmapFactory.Options()
        decodeOptions.inPreferredConfig = bitmapConfig(pBitmapConfig)
        decodeOptions.inSampleSize = sampleSize()
        decodeOptions.inMutable = isNoTexturesMode()

        var input: InputStream? = null
        return try {
            input = openInputStream()
            var decoded = BitmapFactory.decodeStream(input, null, decodeOptions)

            if (isNoTexturesMode()) {
                decoded = decoded.paintBitmap()
            }

            decoded
        } catch (e: IOException) {
            Debug.e("Failed loading Bitmap in " + javaClass.simpleName + ". " + sourceLabel(), e)
            null
        } finally {
            StreamUtils.close(input)
        }
    }
}
