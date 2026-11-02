package org.andengine.graphics.gles

import org.andengine.graphics.Texture
import org.andengine.graphics.TextureData
import org.andengine.graphics.TextureFormat

/** Supplies the current GL id of a texture owned by legacy code; read at every bind. */
fun interface LegacyTextureIdProvider {

    /** The GL texture id, or 0 or less while the texture is not on the GPU. */
    fun hardwareTextureId(): Int
}

/**
 * A [Texture] that only views a 2D texture owned by legacy code (`ITexture`), so it can be bound
 * through the render context. It owns no GL object and is not in the resource registry: the legacy
 * texture loads and restores itself, and its id is read from [idProvider] at every bind, so a
 * reload is picked up without telling the view. While the legacy texture is not on the GPU the
 * [GlesPlaceholderTexture] is bound instead. [format] is nominal (legacy textures have several);
 * [upload] is not supported. GL thread only.
 */
class GlesLegacyTextureView(
    private val idProvider: LegacyTextureIdProvider,
    override val width: Int,
    override val height: Int,
    override val label: String
) : Texture {

    override val layers: Int get() = 1

    override val format: TextureFormat get() = TextureFormat.RGBA8

    /** True while the legacy texture is on the GPU (and still alive). */
    val isLoaded: Boolean get() = idProvider.hardwareTextureId() > 0

    /** The id to bind: the legacy texture's, or the placeholder while it is not loaded. */
    val textureId: Int
        get() {
            val id = idProvider.hardwareTextureId()
            return if (id > 0) id else GlesPlaceholderTexture.id
        }

    /** Only marks the view as unusable; the legacy texture is not affected. */
    override var isDisposed = false
        private set

    override fun upload(layer: Int, x: Int, y: Int, data: TextureData) {
        throw UnsupportedOperationException("Texture '$label' is owned by legacy code and uploads through it")
    }

    override fun dispose() {
        isDisposed = true
    }

    override fun toString() = "GlesLegacyTextureView($label, ${width}x$height)"
}
