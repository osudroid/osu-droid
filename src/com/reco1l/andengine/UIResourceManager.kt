@file:Suppress("ConstPropertyName")

package com.reco1l.andengine

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import com.reco1l.andengine.component.UIComponent
import org.andengine.graphics.text.AndroidGlyphRasterizer
import org.andengine.graphics.text.GlyphAtlas
import org.andengine.opengl.font.Font
import org.andengine.opengl.font.FontManager
import java.lang.ref.WeakReference

class UIResourceManager(
    private val context: Context,
    private val fontManager: () -> FontManager = { UIEngine.current.fontManager }
) {

    private val fonts = mutableMapOf<String, Font>()
    private val fontSubscribers = mutableMapOf<Font, MutableList<WeakReference<UIComponent>>>()


    fun getOrStoreFont(size: Float, family: String): Font {

        val fontIdentifier = "${family}-${size}"

        val fetchedFont = fonts[fontIdentifier]
        if (fetchedFont != null) {
            return fetchedFont
        }

        val manager = fontManager()
        val typeface = Typeface.createFromAsset(context.assets, "fonts/${family}")
        val rasterizer = AndroidGlyphRasterizer(typeface, size * FontManager.getRasterScale(), true)
        val font = Font(manager, rasterizer, GlyphAtlas.shared, Color.WHITE)

        Log.i("UIResourceManager", "Loading font: $fontIdentifier")

        manager.loadFont(font)
        fonts[fontIdentifier] = font

        return font
    }

    fun subscribeToFont(font: Font, component: UIComponent) {
        val subscribers = fontSubscribers.getOrPut(font) { mutableListOf() }
        if (subscribers.none { it.get() === component }) {
            subscribers.add(WeakReference(component))
        }
    }

    fun unsubscribeFromFont(font: Font, component: UIComponent) {

        val subscribers = fontSubscribers[font] ?: return
        subscribers.removeAll { it.get() === component || it.get() == null }

        // The font stays loaded for the process lifetime: fonts own no GPU pages, and unloading would
        // create new atlas slots (the atlas never reuses space) the next time the font is needed.
    }
}
