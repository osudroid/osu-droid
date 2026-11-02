package org.andengine.graphics.batch

import java.lang.ref.WeakReference
import java.util.WeakHashMap
import org.andengine.graphics.Texture
import org.andengine.graphics.gles.GlesLegacyTextureView
import org.andengine.graphics.gles.LegacyTextureIdProvider
import org.andengine.opengl.texture.ITexture

/**
 * Hands out one [Texture] view per legacy [ITexture], so legacy textures can be drawn through the
 * [QuadBatcher]. Views are created once and cached weakly by texture; repeated lookups of the same
 * texture (the common case while a batch runs) are answered without a map lookup. Legacy textures
 * whose id is an external (`GL_TEXTURE_EXTERNAL_OES`) texture get their own view from [externalOf].
 * GL thread only.
 */
object BatchTexture {

    private class Entry(legacy: ITexture, external: Boolean) {
        val provider = WeakIdProvider(legacy)
        val view = if (external) {
            GlesLegacyTextureView(provider, legacy.width, legacy.height, "legacy:$legacy", GlesLegacyTextureView.TEXTURE_EXTERNAL_OES)
        } else {
            GlesLegacyTextureView(provider, legacy.width, legacy.height, "legacy:$legacy")
        }
    }

    private val entries = WeakHashMap<ITexture, Entry>()
    private val externalEntries = WeakHashMap<ITexture, Entry>()

    // One-entry cache in front of the map. Its key is held strongly, so a hit costs no weak
    // reference lookup; [endFrame] drops it, so it keeps a texture alive for one frame at most.
    private var lastLegacy: ITexture? = null
    private var last: Entry? = null

    /** The view of [legacy]; the same instance for as long as [legacy] lives. */
    @JvmStatic
    fun of(legacy: ITexture): Texture {
        val cached = last
        if (cached != null && lastLegacy === legacy) return cached.view

        var entry = entries[legacy]
        if (entry == null) {
            entry = Entry(legacy, external = false)
            entries[legacy] = entry
        }
        last = entry
        lastLegacy = legacy
        return entry.view
    }

    /**
     * The view of [legacy] as an external texture: bound to `GL_TEXTURE_EXTERNAL_OES`, for legacy
     * textures whose hardware id is such a texture (video). Draw it only with an external
     * [MeshProgram]. The same instance for as long as [legacy] lives; not the view [of] returns.
     */
    @JvmStatic
    fun externalOf(legacy: ITexture): Texture {
        var entry = externalEntries[legacy]
        if (entry == null) {
            entry = Entry(legacy, external = true)
            externalEntries[legacy] = entry
        }
        return entry.view
    }

    /** Drops the strong reference of the one-entry cache; called at the end of every frame. */
    @JvmStatic
    fun endFrame() {
        lastLegacy = null
        last = null
    }

    /**
     * False for a legacy texture view whose texture is not on the GPU (or no longer alive); such
     * quads are not drawn. Engine textures are always drawable.
     */
    @JvmStatic
    fun isLoaded(texture: Texture): Boolean = texture !is GlesLegacyTextureView || texture.isLoaded

    /**
     * Holds the texture weakly: the map's values must not keep their keys alive. A collected
     * texture reads as "not loaded"; it cannot be drawn anyway once nothing references it.
     */
    private class WeakIdProvider(legacy: ITexture) : LegacyTextureIdProvider {
        private val ref = WeakReference(legacy)

        val texture: ITexture? get() = ref.get()

        override fun hardwareTextureId(): Int = ref.get()?.hardwareTextureID ?: 0
    }
}
