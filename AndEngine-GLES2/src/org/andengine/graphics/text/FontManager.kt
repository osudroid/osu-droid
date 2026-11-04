/*
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 *
 * @author Nicolas Gramlich
 * @since 17:48:46 - 08.03.2010
 */
package org.andengine.graphics.text

open class FontManager {

    private val mFontsManaged = ArrayList<Font>()
    private var mReloading = false

    open fun onCreate() {
    }

    @Synchronized
    open fun onDestroy() {
        val managedFonts = mFontsManaged
        for (i in managedFonts.size - 1 downTo 0) {
            managedFonts[i].invalidateLetters()
        }

        mFontsManaged.clear()
    }

    /** Registers the font once; loading an already managed font again changes nothing. */
    @Synchronized
    open fun loadFont(pFont: Font) {
        if (!mFontsManaged.contains(pFont)) {
            mFontsManaged.add(pFont)
        }
    }

    @Synchronized
    open fun loadFonts(vararg pFonts: Font) {
        for (i in pFonts.indices) {
            loadFont(pFonts[i])
        }
    }

    @Synchronized
    open fun unloadFont(pFont: Font) {
        mFontsManaged.remove(pFont)
    }

    @Synchronized
    open fun unloadFonts(vararg pFonts: Font) {
        for (i in pFonts.indices) {
            unloadFont(pFonts[i])
        }
    }

    @Synchronized
    open fun updateFonts(): Boolean {
        val fonts = mFontsManaged
        val fontCount = fonts.size
        if (fontCount > 0) {
            for (i in fontCount - 1 downTo 0) {
                fonts[i].update()
            }
        }

        if (mReloading) {
            mReloading = false
            return true
        } else {
            return false
        }
    }

    /**
     * Uploads every existing glyph again into its atlas slot, e.g. after a context loss left the atlas empty.
     */
    @Synchronized
    open fun onReload() {
        mReloading = true
        val managedFonts = mFontsManaged
        for (i in managedFonts.size - 1 downTo 0) {
            managedFonts[i].invalidateLetters()
        }
    }

    companion object {

        @Volatile
        private var sRasterScale = 1f

        /**
         * Sets the physical pixels per virtual unit that fonts created afterwards rasterize their glyphs with.
         */
        @JvmStatic
        fun setRasterScale(pScale: Float) {
            if (!(pScale > 0)) {
                throw IllegalArgumentException("pScale must be positive: $pScale")
            }
            sRasterScale = pScale
        }

        @JvmStatic
        fun getRasterScale(): Float {
            return sRasterScale
        }
    }
}
