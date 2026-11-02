package com.osudroid.resources

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Typeface
import org.andengine.engine.Engine
import org.andengine.graphics.text.AndroidGlyphRasterizer
import org.andengine.graphics.text.GlyphAtlas
import org.andengine.opengl.font.Font
import org.andengine.opengl.font.FontManager
import org.andengine.opengl.font.StrokeFont
import ru.nsu.ccfit.zuev.osu.Config
import java.util.concurrent.ConcurrentHashMap

/**
 * A named cache of loaded fonts, safe to read and write from any thread. See [TextureStore]'s
 * documentation for why this matters.
 *
 * Fonts draw into the shared glyph atlas, which never reuses freed space, so loading a font with the
 * parameters of the one already stored under the same name returns that font instead of a new one.
 */
class FontStore {

    /** Everything that makes two fonts of the same name interchangeable. */
    private data class Spec(
        val file: String?,
        val size: Int,
        val color: Int,
        val strokeColor: Int,
        val strokeWidth: Float,
        val stroke: Boolean,
    )

    // Set via init(), mirroring ResourceManager.Init() - see TextureStore's equivalent for why.
    private lateinit var fontManager: FontManager
    private lateinit var assets: AssetManager

    private val fonts = ConcurrentHashMap<String, Font>()
    private val specs = ConcurrentHashMap<String, Spec>()

    fun init(engine: Engine, context: Context) = init(engine.fontManager, context.assets)

    fun init(fontManager: FontManager, assets: AssetManager) {
        this.fontManager = fontManager
        this.assets = assets
    }

    fun get(name: String): Font? = fonts[name]

    fun containsKey(name: String): Boolean = fonts.containsKey(name)

    /** Returns the cached font, lazily loading a default-styled one under [name] if absent. */
    fun getOrLoadDefault(name: String): Font? {
        if (!fonts.containsKey(name)) {
            loadFont(name, null, 35, android.graphics.Color.WHITE)
        }
        return fonts[name]
    }

    fun clear() {
        fonts.clear()
        specs.clear()
    }

    @Synchronized
    fun loadFont(resname: String, file: String?, size: Int, color: Int): Font {
        val scaledSize = size / Config.getTextureQuality()
        val spec = Spec(file, scaledSize, color, 0, 0f, false)

        existing(resname, spec)?.let { return it }

        val rasterizer = AndroidGlyphRasterizer(typeface(file), scaledSize * FontManager.getRasterScale(), true)
        return register(resname, spec, Font(fontManager, rasterizer, GlyphAtlas.shared, color))
    }

    @Synchronized
    fun loadStrokeFont(resname: String, file: String?, size: Int, color1: Int, color2: Int): StrokeFont {
        val quality = Config.getTextureQuality()
        val scaledSize = size / quality
        val strokeWidth = if (file == null && quality != 1) 0.75f else 2f / quality
        val spec = Spec(file, scaledSize, color1, color2, strokeWidth, true)

        (existing(resname, spec) as? StrokeFont)?.let { return it }

        val scale = FontManager.getRasterScale()
        val rasterizer = AndroidGlyphRasterizer(typeface(file), scaledSize * scale, true, strokeWidth * scale)
        return register(resname, spec, StrokeFont(fontManager, rasterizer, GlyphAtlas.shared, color1, color2, false))
    }

    private fun existing(resname: String, spec: Spec): Font? =
        if (specs[resname] == spec) fonts[resname] else null

    private fun typeface(file: String?): Typeface =
        if (file == null) Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        else Typeface.createFromAsset(assets, "fonts/$file")

    private fun <T : Font> register(resname: String, spec: Spec, font: T): T {
        fontManager.loadFont(font)
        val old = fonts.put(resname, font)
        specs[resname] = spec
        if (old != null) fontManager.unloadFont(old)
        return font
    }
}
