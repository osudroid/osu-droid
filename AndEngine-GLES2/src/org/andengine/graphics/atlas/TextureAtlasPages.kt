package org.andengine.graphics.atlas

import java.util.IdentityHashMap
import java.util.WeakHashMap
import org.andengine.opengl.texture.ITexture
import org.andengine.opengl.texture.TextureManager
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource
import org.andengine.opengl.texture.region.ITiledTextureRegion
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.opengl.texture.region.TiledTextureRegion

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
 *
 * With [allowRotation], an image that does not fit upright on the open page is tried turned by 90°
 * before a new page is opened; its region is then [TextureRegion.isRotated]. Only entities that honour
 * rotated regions (such as [org.andengine.entity.sprite.Sprite]) draw those correctly, so it is off by
 * default.
 */
class TextureAtlasPages @JvmOverloads constructor(
    private val textureManager: TextureManager,
    val maxPageSize: Int = 2048,
    val maxRegionSize: Int = 512,
    val padding: Int = 2,
    val maxRegionLength: Int = maxRegionSize * 2,
    val allowRotation: Boolean = false
) {

    /**
     * Largest accepted image area: [maxRegionSize] squared. Long, thin images (list item backgrounds,
     * bars) may be up to [maxRegionLength] on one side as long as they stay within this area.
     */
    val maxRegionArea: Int = maxRegionSize * maxRegionSize

    init {
        require(maxRegionLength >= maxRegionSize) { "maxRegionLength $maxRegionLength is below maxRegionSize $maxRegionSize" }
    }

    private class Page(
        @JvmField val atlas: BitmapTextureAtlas,
        @JvmField var packer: AtlasPacker,
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

    // Pages are shared by options with the same filtering and alpha handling; wrapping is always clamped.
    private fun sameOptions(a: TextureOptions, b: TextureOptions): Boolean =
        a === b || a.mMinFilter == b.mMinFilter && a.mMagFilter == b.mMagFilter && a.mPreMultiplyAlpha == b.mPreMultiplyAlpha

    /**
     * Places [source] on a page and returns a region covering exactly its content. Returns `null` when
     * no session is open, the image is larger than [maxRegionArea] or longer than [maxRegionLength] on a
     * side, it does not fit on an empty page, or [options] repeat (wrapping cannot be expressed inside a
     * shared page).
     */
    fun add(source: IBitmapTextureAtlasSource, options: TextureOptions): TextureRegion? =
        synchronized(lock) { place(listOf(source), options)?.get(0) }

    /**
     * Places all of [sources] on one page, in order, and returns them as the tiles of a
     * [TiledTextureRegion] on that page. The set is never split: if it does not fit on the open page, all
     * tiles go onto a new one. Returns `null` for an empty list, outside a session, if any image is
     * rejected the way [add] rejects it, or if the set does not fit on an empty page. Release it with
     * [release] for tiled regions.
     */
    fun addTiled(sources: List<IBitmapTextureAtlasSource>, options: TextureOptions): TiledTextureRegion? {
        val regions = synchronized(lock) { place(sources, options) } ?: return null
        return TiledTextureRegion(regions[0].texture, *regions.toTypedArray())
    }

    private fun accepts(source: IBitmapTextureAtlasSource): Boolean {
        val width = source.textureWidth
        val height = source.textureHeight
        if (width <= 0 || height <= 0 || width > maxRegionLength || height > maxRegionLength) return false
        return width.toLong() * height <= maxRegionArea
    }

    /** Places every image on [packer], upright or, if allowed, turned; `null` if one does not fit. */
    private fun placeAll(packer: AtlasPacker, sources: List<IBitmapTextureAtlasSource>): Array<AtlasSlot>? {
        val slots = arrayOfNulls<AtlasSlot>(sources.size)
        for (i in sources.indices) {
            val width = sources[i].textureWidth
            val height = sources[i].textureHeight
            slots[i] = packer.tryPlace(width, height)
                ?: (if (allowRotation) packer.tryPlace(height, width) else null)
                ?: return null
        }
        @Suppress("UNCHECKED_CAST")
        return slots as Array<AtlasSlot>
    }

    /** Puts all of [sources] on one page and returns their regions; `null` if they cannot be placed. Holds [lock]. */
    private fun place(sources: List<IBitmapTextureAtlasSource>, options: TextureOptions): List<TextureRegion>? {
        if (sessionDepth == 0 || sources.isEmpty()) return null
        if (options.mWrapS != CLAMPED.mWrapS || options.mWrapT != CLAMPED.mWrapT) return null
        for (i in sources.indices) if (!accepts(sources[i])) return null

        // At most one page per options is open at a time: a full one is closed before a new one opens.
        var openIndex = -1
        for (i in 0 until pages.size) {
            val candidate = pages[i]
            if (!candidate.submitted && sameOptions(candidate.options, options)) {
                openIndex = i
                break
            }
        }

        // Placements are tried on a copy of the packer, so a set that does not fit leaves the page as it was.
        var page: Page? = if (openIndex >= 0) pages[openIndex] else null
        var packer = page?.packer?.copy()
        var slots = packer?.let { placeAll(it, sources) }

        if (page == null || slots == null) {
            packer = AtlasPacker(pageSize, padding)
            slots = placeAll(packer, sources) ?: return null
            // The open page is full for these images: close it now instead of at the end of the session.
            if (openIndex >= 0) close(openIndex)
            val atlas = BitmapTextureAtlas(textureManager, packer.pageSize, packer.pageSize, options)
            page = Page(atlas, packer, options)
            pages.add(page)
            pageTextures[atlas] = true
        } else {
            page.packer = packer!!
        }

        val regions = ArrayList<TextureRegion>(sources.size)
        for (i in sources.indices) {
            val source = sources[i]
            val slot = slots[i]
            // A turned image lies on the page with its sides swapped.
            val rotated = slot.width != source.textureWidth || slot.height != source.textureHeight
            page.atlas.addTextureAtlasSource(ExtrudedBitmapSource(source, padding, rotated), slot.x - padding, slot.y - padding)
            // A rotated region takes the image's own size and swaps it for its extent on the page.
            val region = TextureRegion(
                page.atlas, slot.x.toFloat(), slot.y.toFloat(),
                source.textureWidth.toFloat(), source.textureHeight.toFloat(), rotated
            )
            page.references++
            regionPages[region] = page
            regions.add(region)
        }
        return regions
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

    /** Releases every tile of [tiled] like [release]; `true` if they lie on pages of this instance. */
    fun release(tiled: ITiledTextureRegion): Boolean {
        var ours = false
        for (i in 0 until tiled.tileCount) {
            val tile = tiled.getTextureRegion(i)
            if (tile is TextureRegion && release(tile)) ours = true
        }
        return ours
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
