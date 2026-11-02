package org.andengine.graphics.atlas

import org.andengine.opengl.texture.ITexture
import org.andengine.opengl.texture.TextureManager
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource
import org.andengine.opengl.texture.region.TextureRegion
import java.util.IdentityHashMap
import java.util.WeakHashMap

/**
 * Packs small images into shared [BitmapTextureAtlas] pages so that sprites using them share one
 * texture.
 *
 * Images are only accepted between [beginSession] and [endSession]. Within a session, an image goes
 * onto the open page with the same [TextureOptions] if it has room; otherwise that page is closed and
 * handed to the [TextureManager] right away (so its decoded images do not wait in memory for the end
 * of the session) and a new page is opened. The outermost [endSession] hands over the pages still
 * open. A page is never filled after it was handed over, so it is never uploaded while being filled;
 * later sessions open new pages instead of re-uploading it.
 *
 * Every region holds a reference on its page; [release] drops it and unloads the page once no region
 * is left. All methods are thread-safe (loading usually runs on background threads while the GL
 * thread renders).
 */
class TextureAtlasPages @JvmOverloads constructor(
    private val textureManager: TextureManager,
    val maxPageSize: Int = 2048,
    val maxRegionSize: Int = 512,
    val padding: Int = 2
) {

    private class Page(
        @JvmField val atlas: BitmapTextureAtlas,
        @JvmField val packer: AtlasPacker,
        @JvmField val options: TextureOptions
    ) {
        @JvmField var references = 0
        @JvmField var submitted = false
    }

    private val lock = Any()
    private val pages = ArrayList<Page>()
    private val regionPages = IdentityHashMap<TextureRegion, Page>()

    /** Every page texture this instance created, live or not, so copies and stale aliases are recognized. */
    private val pageTextures = WeakHashMap<ITexture, Boolean>()

    private var sessionDepth = 0

    val pageCount: Int
        get() = synchronized(lock) { pages.size }

    /** Edge length for new pages: [maxPageSize], limited by [deviceMaxTextureSize] once that is known. */
    val pageSize: Int
        get() {
            val device = deviceMaxTextureSize
            return if (device > 0) minOf(maxPageSize, device) else maxPageSize
        }

    fun beginSession() {
        synchronized(lock) { sessionDepth++ }
    }

    /** Ends a session; the outermost one hands all pages filled since then to the [TextureManager]. */
    fun endSession() {
        synchronized(lock) {
            if (sessionDepth == 0) return
            sessionDepth--
            if (sessionDepth > 0) return

            for (i in pages.size - 1 downTo 0) {
                val page = pages[i]
                if (!page.submitted) close(i)
            }
        }
    }

    /** Hands the open page at [index] over for upload, or drops it if no region is left on it. */
    private fun close(index: Int) {
        val page = pages[index]
        if (page.references == 0) {
            pages.removeAt(index)
            return
        }
        page.submitted = true
        textureManager.loadTexture(page.atlas)
    }

    /**
     * Places [source] on a page and returns a region covering exactly its content. Returns `null` when
     * no session is open, the image exceeds [maxRegionSize] in either dimension, it does not fit on an
     * empty page, or [options] repeat (wrapping cannot be expressed inside a shared page).
     */
    // Pages are shared by options with the same filtering and alpha handling; wrapping is always clamped.
    private fun sameOptions(a: TextureOptions, b: TextureOptions): Boolean =
        a === b || a.mMinFilter == b.mMinFilter && a.mMagFilter == b.mMagFilter && a.mPreMultiplyAlpha == b.mPreMultiplyAlpha

    fun add(source: IBitmapTextureAtlasSource, options: TextureOptions): TextureRegion? {
        val width = source.textureWidth
        val height = source.textureHeight
        if (width <= 0 || height <= 0 || width > maxRegionSize || height > maxRegionSize) return null
        if (options.mWrapS != CLAMPED.mWrapS || options.mWrapT != CLAMPED.mWrapT) return null

        synchronized(lock) {
            if (sessionDepth == 0) return null

            // At most one page per options is open at a time (see below).
            var openIndex = -1
            for (i in 0 until pages.size) {
                val candidate = pages[i]
                if (!candidate.submitted && sameOptions(candidate.options, options)) {
                    openIndex = i
                    break
                }
            }

            var page: Page? = if (openIndex >= 0) pages[openIndex] else null
            var slot: AtlasSlot? = page?.packer?.tryPlace(width, height)

            if (page == null || slot == null) {
                val size = pageSize
                val packer = AtlasPacker(size, padding)
                slot = packer.tryPlace(width, height) ?: return null
                // The open page is full for this image: close it now instead of at the end of the session.
                if (openIndex >= 0) close(openIndex)
                val atlas = BitmapTextureAtlas(textureManager, size, size, options)
                page = Page(atlas, packer, options)
                pages.add(page)
                pageTextures[atlas] = true
            }

            page.atlas.addTextureAtlasSource(ExtrudedBitmapSource(source, padding), slot.x - padding, slot.y - padding)
            val region = TextureRegion(page.atlas, slot.x.toFloat(), slot.y.toFloat(), width.toFloat(), height.toFloat())
            page.references++
            regionPages[region] = page
            return region
        }
    }

    /**
     * Returns `true` if [region] lies on a page of this instance; the caller must then not unload its
     * texture itself. The first release of a region drops its page reference, unloading the page when it
     * was the last one; further releases (aliases, copies) change nothing.
     */
    fun release(region: TextureRegion): Boolean {
        synchronized(lock) {
            val page = regionPages.remove(region) ?: return pageTextures.containsKey(region.texture)
            page.references--
            if (page.references == 0) {
                pages.remove(page)
                if (page.submitted) textureManager.unloadTexture(page.atlas)
            }
            return true
        }
    }

    /** Whether [region] lies on a page created by this instance (including copies and already released regions). */
    fun isAtlasRegion(region: TextureRegion): Boolean =
        synchronized(lock) { pageTextures.containsKey(region.texture) }

    companion object {
        private val CLAMPED = TextureOptions.BILINEAR

        /** `GL_MAX_TEXTURE_SIZE` of the current context, published by the renderer; 0 while unknown. */
        @Volatile
        @JvmStatic
        var deviceMaxTextureSize: Int = 0
    }
}
