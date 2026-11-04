@file:Suppress("ConstPropertyName")

package com.reco1l.andengine

import android.content.Context
import android.graphics.Color
import android.util.Log
import org.andengine.graphics.text.Font
import org.andengine.graphics.text.FontFactory
import org.andengine.graphics.text.FontManager

/**
 * @param fontManager supplies the font manager the fonts are loaded with. Tests pass their own.
 */
class UIResourceManager(
    private val context: Context,
    private val fontManager: () -> FontManager = { UIEngine.current.fontManager }
) {

    // Fonts stay loaded for the process lifetime: they own no GPU pages, and unloading would create
    // new atlas slots (the atlas never reuses space) the next time the font is needed.
    private val fonts = mutableMapOf<String, Font>()


    fun getOrStoreFont(size: Float, family: String): Font {

        val fontIdentifier = "${family}-${size}"

        val fetchedFont = fonts[fontIdentifier]
        if (fetchedFont != null) {
            return fetchedFont
        }

        Log.i("UIResourceManager", "Loading font: $fontIdentifier")

        val font = FontFactory.createFromAsset(fontManager(), context.assets, "fonts/${family}", size, true, Color.WHITE)
        fonts[fontIdentifier] = font

        return font
    }
}
