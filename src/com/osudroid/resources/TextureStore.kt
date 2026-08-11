package com.osudroid.resources

import android.content.Context
import com.reco1l.andengine.texture.BlankTextureRegion
import org.andengine.engine.Engine
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.opengl.texture.region.TextureRegionFactory
import ru.nsu.ccfit.zuev.osu.helper.QualityAssetBitmapSource
import ru.nsu.ccfit.zuev.osu.helper.QualityFileBitmapSource
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * A named cache of loaded textures plus their animation frame counts, safe to read and write from
 * any thread. Historically (in `ResourceManager`) this was a plain `HashMap` written from
 * background skin-loading threads (see `BeatmapSkinManager`) while the update/GL thread read it
 * concurrently during rendering - corrupting under that access pattern. Backed by
 * [ConcurrentHashMap] here instead, so every operation below is safe regardless of calling thread.
 *
 * One [TextureStore] instance is one flat cache; the default-skin/user-skin/beatmap-skin layering
 * ([BeatmapSkinManager]'s override behavior) is composed by the caller out of two instances, not
 * built into this class.
 */
class TextureStore {

    // Set via init(), mirroring ResourceManager.Init() - not constructor parameters, so that
    // bookkeeping-only tests can construct a store without needing a real Engine/Context (those
    // fields are only touched by the GL-touching load methods below).
    private lateinit var engine: Engine
    private lateinit var context: Context

    private val textures = ConcurrentHashMap<String, TextureRegion>()
    private val frameCounts = ConcurrentHashMap<String, Int>()

    fun init(engine: Engine, context: Context) {
        this.engine = engine
        this.context = context
    }

    /**
     * Sentinel standing in for a `null` cache entry - e.g. the "lighting" texture, which
     * `ResourceManager.loadSkin` intentionally marks as "checked, absent" to suppress repeated
     * lazy-load attempts. [ConcurrentHashMap] forbids real `null` values, so this is substituted
     * internally and translated back to `null` by [get]/[getOrNull], preserving the exact external
     * contract callers (e.g. `GameScene`'s `getTexture("lighting") != null` check) rely on.
     */
    private val absentTexture: TextureRegion = BlankTextureRegion()

    //region Bookkeeping

    /** Returns the cached region for [name], or `null` if absent or explicitly marked absent via [markAbsent]. */
    fun get(name: String): TextureRegion? = textures[name]?.takeIf { it !== absentTexture }

    fun containsKey(name: String): Boolean = textures.containsKey(name)

    fun put(name: String, region: TextureRegion) {
        textures[name] = region
    }

    /** Marks [name] as checked-and-absent: [containsKey] becomes true, [get] keeps returning `null`. */
    fun markAbsent(name: String) {
        textures.putIfAbsent(name, absentTexture)
    }

    fun remove(name: String): TextureRegion? = textures.remove(name)?.takeIf { it !== absentTexture }

    val keys: Set<String> get() = textures.keys

    fun getFrameCount(name: String): Int = frameCounts[name] ?: -1

    /** Mirrors `ResourceManager.parseFrameIndex`'s "only grow, never shrink" update rule. */
    fun growFrameCount(name: String, atLeast: Int) {
        frameCounts.merge(name, atLeast) { existing, candidate -> maxOf(existing, candidate) }
    }

    fun removeFrameCount(name: String) {
        frameCounts.remove(name)
    }

    fun clearFrameCounts() = frameCounts.clear()

    fun clear() {
        textures.clear()
        frameCounts.clear()
    }

    //endregion

    //region GL-touching loads

    /**
     * Loads a texture either from an external file (with `@2x` HD-variant fallback when the exact
     * path doesn't exist) or from the app's bundled assets, registers it under [resname], and
     * returns it. Returns `null` on decode failure, or a [BlankTextureRegion] when an external file
     * (and its `@2x` variant) doesn't exist at all - matching `ResourceManager.loadTexture`'s
     * original, slightly inconsistent failure contract exactly (callers historically rely on both).
     */
    fun load(resname: String, file: String, external: Boolean, opt: TextureOptions = TextureOptions.BILINEAR): TextureRegion? {
        val region: TextureRegion

        if (external) {
            var texFile = File(file)
            var isHDTexture = false

            if (!texFile.exists()) {
                val dotIndex = file.lastIndexOf('.')
                texFile = File(file.substring(0, dotIndex) + "@2x" + file.substring(dotIndex))
                isHDTexture = texFile.exists()

                if (!isHDTexture) {
                    return BlankTextureRegion()
                }
            }

            val source = QualityFileBitmapSource(texFile, if (isHDTexture) 2 else 1)
            if (source.width == 0 || source.height == 0 || !source.preload()) {
                return null
            }

            val tex = BitmapTextureAtlas(engine.textureManager, source.width, source.height, opt)
            region = TextureRegionFactory.createFromSource(tex, source, 0, 0, false)
            engine.textureManager.loadTexture(tex)
        } else {
            val source = try {
                QualityAssetBitmapSource(context, file)
            } catch (e: NullPointerException) {
                return BlankTextureRegion()
            }

            if (source.width == 0 || source.height == 0 || !source.preload()) {
                return null
            }

            val tex = BitmapTextureAtlas(engine.textureManager, source.width, source.height, opt)
            region = TextureRegionFactory.createFromSource(tex, source, 0, 0, false)
            engine.textureManager.loadTexture(tex)
        }

        textures[resname] = region
        return region
    }

    fun loadHighQualityAsset(resname: String, file: String): TextureRegion? {
        val source = QualityAssetBitmapSource(context, file)
        if (source.width == 0 || source.height == 0) {
            return null
        }

        val tex = BitmapTextureAtlas(engine.textureManager, source.width, source.height, TextureOptions.BILINEAR)
        val region = TextureRegionFactory.createFromSource(tex, source, 0, 0, false)
        engine.textureManager.loadTexture(tex)

        textures[resname] = region
        return region
    }

    fun loadHighQualityFile(resname: String, file: File): TextureRegion? {
        val source = QualityFileBitmapSource(file)
        if (source.width == 0 || source.height == 0) {
            return null
        }

        val tex = BitmapTextureAtlas(engine.textureManager, source.width, source.height, TextureOptions.BILINEAR)
        val region = TextureRegionFactory.createFromSource(tex, source, 0, 0, false)
        engine.textureManager.loadTexture(tex)

        textures[resname] = region
        return region
    }

    fun unload(name: String) {
        val region = get(name) ?: return
        engine.textureManager.unloadTexture(region.texture)
        textures.remove(name)
    }

    fun unload(region: TextureRegion) {
        engine.textureManager.unloadTexture(region.texture)
        textures.entries.removeAll { it.value === region }
    }

    //endregion
}
