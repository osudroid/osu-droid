package org.andengine.opengl.texture.atlas.bitmap.source

import org.andengine.graphics.texture.Pixels

/**
 * An atlas source that can deliver its image as RGBA bytes. An RGBA_8888 atlas uploads these bytes as
 * they are, without the per-pixel conversion of the bitmap path: straight-alpha bytes for a
 * non-premultiplied atlas, premultiplied ones for a premultiplied atlas. Every load decodes anew, so the
 * pixels are available again after a context loss.
 *
 * [onLoadBitmap] stays the fallback: other formats use it, and so does an atlas when the source cannot
 * deliver its variant ([canLoadPixels]) or [onLoadPixels] returns `null` or the other variant. The
 * [onLoadPixels] variants must not leave any state for a following [onLoadBitmap] call: they may run at
 * different times or on different threads.
 *
 * [onLoadPixels] usually runs on the texture manager's background worker (see
 * [org.andengine.opengl.texture.TextureManager]), possibly while the GL thread loads the same source
 * for another load, so it must be thread-safe. A source that is prepared before it is queued (e.g. a
 * preload) must finish that preparation before its texture is queued for loading, and may only hand
 * the prepared pixels to a load of the same variant.
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
