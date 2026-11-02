package org.andengine.opengl.texture.atlas.bitmap.source

import org.andengine.graphics.texture.Pixels

/**
 * An atlas source that can deliver its image as RGBA bytes, which an RGBA_8888 atlas uploads as they
 * are: straight-alpha bytes for a non-premultiplied atlas, premultiplied ones for a premultiplied atlas.
 * [onLoadBitmap] stays the fallback for other formats and whenever [onLoadPixels] cannot deliver the
 * variant. [onLoadPixels] usually runs on the texture manager's background worker, possibly while the
 * GL thread loads the same source, so it must be thread-safe and leave no state for [onLoadBitmap].
 */
interface IPixelTextureAtlasSource : IBitmapTextureAtlasSource {
    /** The image as straight RGBA bytes, or `null` if they are not available. */
    fun onLoadPixels(): Pixels?

    /**
     * The image as RGBA bytes, premultiplied by alpha if [premultiplied] (then [Pixels.premultiplied] is
     * set), or `null` if that variant is not available. By default only straight bytes are offered.
     */
    fun onLoadPixels(premultiplied: Boolean): Pixels? = if (premultiplied) null else onLoadPixels()

    /**
     * Whether [onLoadPixels] can deliver the given variant at all; if not, an atlas of that variant loads
     * this source through [onLoadBitmap] right away. Must not depend on state that changes between loads.
     */
    fun canLoadPixels(premultiplied: Boolean): Boolean = !premultiplied
}
