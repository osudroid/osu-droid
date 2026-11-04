package com.osudroid.resources

import android.content.Context
import com.reco1l.andengine.texture.BlankTextureRegion
import org.andengine.engine.Engine
import org.andengine.graphics.atlas.TextureAtlasPages
import org.andengine.opengl.texture.TextureManager
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource
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
    private lateinit var textureManager: TextureManager
    private lateinit var context: Context
    private var atlasPages: TextureAtlasPages? = null

    private val textures = ConcurrentHashMap<String, TextureRegion>()
    private val frameCounts = ConcurrentHashMap<String, Int>()

    /** [atlasPages], if given, receives small images loaded while one of its sessions is open. */
    @JvmOverloads
    fun init(engine: Engine, context: Context, atlasPages: TextureAtlasPages? = null) {
        this.textureManager = engine.textureManager
        this.context = context
        this.atlasPages = atlasPages
    }

    /** Sets up everything except the asset-backed loads, which need a [Context]. Tests use this. */
    fun init(textureManager: TextureManager, atlasPages: TextureAtlasPages?) {
        this.textureManager = textureManager
        this.atlasPages = atlasPages
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

    /**
     * Unconditionally sets [name] to [region] - a `null` region is stored as [markAbsent] would,
     * matching the original `Map<String, TextureRegion>.put(name, someNullableGet())` pass-through
     * pattern `ResourceManager.loadBackground` relies on (its "::background" fallback stores
     * whatever `get("menu-background")` returned, including `null` if that itself was never loaded).
     */
    fun put(name: String, region: TextureRegion?) {
        textures[name] = region ?: absentTexture
    }

    /**
     * Marks [name] as checked-and-absent: [containsKey] becomes true, [get] keeps returning `null`.
     * Unlike [put], does not override an already-present region (matching the `if
     * (!textures.containsKey("lighting")) textures.put("lighting", null)` guard every original call
     * site used this for).
     */
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

    /**
     * Forgets every entry. Atlas regions give back their page reference; textures of their own are
     * left loaded.
     */
    fun clear() {
        val pages = atlasPages
        if (pages != null) {
            for (region in textures.values) {
                if (region !== absentTexture) pages.release(region)
            }
        }
        textures.clear()
        frameCounts.clear()
    }

    /** Whether [region] shares an atlas page with other images, so its texture must not be unloaded directly. */
    fun isAtlasRegion(region: TextureRegion): Boolean = atlasPages?.isAtlasRegion(region) == true

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
            if (source.width == 0 || source.height == 0 || !source.preload(opt.mPreMultiplyAlpha)) {
                return null
            }

            region = createRegion(source, source.width, source.height, opt)
        } else {
            val source = try {
                QualityAssetBitmapSource(context, file)
            } catch (e: NullPointerException) {
                return BlankTextureRegion()
            }

            if (source.width == 0 || source.height == 0 || !source.preload(opt.mPreMultiplyAlpha)) {
                return null
            }

            region = createRegion(source, source.width, source.height, opt)
        }

        replace(resname, region)
        return region
    }

    fun loadHighQualityAsset(resname: String, file: String): TextureRegion? {
        val source = QualityAssetBitmapSource(context, file)
        if (source.width == 0 || source.height == 0) {
            return null
        }

        val region = createRegion(source, source.width, source.height, TextureOptions.BILINEAR)

        replace(resname, region)
        return region
    }

    fun loadHighQualityFile(resname: String, file: File): TextureRegion? {
        val source = QualityFileBitmapSource(file)
        if (source.width == 0 || source.height == 0 || !source.preload()) {
            return null
        }

        val region = createRegion(source, source.width, source.height, TextureOptions.BILINEAR)

        replace(resname, region)
        return region
    }

    /**
     * Returns a region for [source] ([width]x[height]): on an atlas page while a session of the
     * configured [TextureAtlasPages] is open and the image qualifies, otherwise on a texture of its own
     * that is queued for upload right away.
     */
    fun createRegion(source: IBitmapTextureAtlasSource, width: Int, height: Int, opt: TextureOptions): TextureRegion {
        atlasPages?.add(source, opt)?.let { return it }

        val tex = BitmapTextureAtlas(textureManager, width, height, opt)
        val region = TextureRegionFactory.createFromSource(tex, source, 0, 0, false)
        textureManager.loadTexture(tex)
        return region
    }

    /**
     * Stores [region] under [name] like a load does. The region previously stored there is released
     * (its texture unloaded, or its atlas page reference dropped) unless another name still maps to it -
     * otherwise every reload under the same name, e.g. on a skin change, would keep the old texture
     * or atlas page alive forever.
     */
    fun replace(name: String, region: TextureRegion) {
        val previous = textures.put(name, region) ?: return
        if (previous === absentTexture || previous === region || textures.containsValue(previous)) return
        releaseTexture(previous)
    }

    fun unload(name: String) {
        val region = get(name) ?: return
        releaseTexture(region)
        textures.remove(name)
    }

    fun unload(region: TextureRegion) {
        releaseTexture(region)
        textures.entries.removeAll { it.value === region }
    }

    /** Unloads [region]'s texture unless it lies on an atlas page, which is released by reference instead. */
    fun releaseTexture(region: TextureRegion) {
        if (atlasPages?.release(region) != true) {
            textureManager.unloadTexture(region.texture)
        }
    }

    //endregion
}
