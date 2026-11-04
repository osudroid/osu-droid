/*
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga Inc.
 *
 * @author Nicolas Gramlich
 * @since 11:52:26 - 20.08.2010
 */
package org.andengine.graphics.text

import org.andengine.util.adt.map.Library

open class FontLibrary : Library<Font> {

    constructor() : super()

    constructor(pInitialCapacity: Int) : super(pInitialCapacity)

    open fun loadFonts(pFontManager: FontManager) {
        val items = mItems
        for (i in items.size() - 1 downTo 0) {
            val font: Font? = items.valueAt(i)
            if (font != null) {
                pFontManager.loadFont(font)
            }
        }
    }
}
