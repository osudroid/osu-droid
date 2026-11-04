package org.andengine.graphics.gles

import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLES32
import android.util.Log
import org.andengine.graphics.Texture
import org.andengine.graphics.TextureData
import org.andengine.graphics.TextureFilter
import org.andengine.graphics.TextureFormat
import org.andengine.graphics.TextureSource
import org.andengine.graphics.TextureWrap
import org.andengine.graphics.debug.RenderStats

/**
 * Immutable-storage texture (`glTexStorage2D`, or `glTexStorage3D` with `GL_TEXTURE_2D_ARRAY` when
 * [layers] > 1). With a [source], the content is loaded at creation and after every context loss
 * (into layer 0); if the source fails, [textureId] points to [GlesPlaceholderTexture]. Without a
 * source, the texture comes back empty after a context loss. GL thread only.
 */
class GlesTexture(
    override val width: Int,
    override val height: Int,
    override val format: TextureFormat,
    override val layers: Int,
    val filter: TextureFilter,
    val wrap: TextureWrap,
    private val source: TextureSource?,
    override val label: String
) : Texture, GlesRestorable {

    /** `GL_TEXTURE_2D` or `GL_TEXTURE_2D_ARRAY`. */
    @JvmField
    val glTarget: Int = if (layers > 1) GLES30.GL_TEXTURE_2D_ARRAY else GLES20.GL_TEXTURE_2D

    private val levels = if (filter == TextureFilter.LinearMipmapLinear) mipLevels(width, height) else 1

    /** The GL texture owned by this object; 0 while there is none. */
    var ownTextureId = 0
        private set

    /** True when the [source] failed and [textureId] is the placeholder. */
    var usesPlaceholder = false
        private set

    /** The GL texture to bind: the own one, or the placeholder if the source failed. */
    val textureId: Int
        get() = if (usesPlaceholder) GlesPlaceholderTexture.id else ownTextureId

    override var isDisposed = false
        private set

    override val restorePhase: RestorePhase get() = RestorePhase.Textures

    init {
        create()
        GlesBackend.registry.register(this)
    }

    private fun create() {
        val gl = GlesBackend.gl
        val ids = IntArray(1)
        gl.glGenTextures(1, ids, 0)
        ownTextureId = ids[0]

        GlesBackend.state.bindTexture(glTarget, ownTextureId)
        val internal = GlesResources.internalFormat(format)
        if (layers > 1) {
            gl.glTexStorage3D(glTarget, levels, internal, width, height, layers)
        } else {
            gl.glTexStorage2D(glTarget, levels, internal, width, height)
        }
        gl.glTexParameteri(glTarget, GLES20.GL_TEXTURE_MIN_FILTER, GlesResources.minFilter(filter))
        gl.glTexParameteri(glTarget, GLES20.GL_TEXTURE_MAG_FILTER, GlesResources.magFilter(filter))
        gl.glTexParameteri(glTarget, GLES20.GL_TEXTURE_WRAP_S, GlesResources.wrap(wrap))
        gl.glTexParameteri(glTarget, GLES20.GL_TEXTURE_WRAP_T, GlesResources.wrap(wrap))
        GlesResources.label(GLES32.GL_TEXTURE, ownTextureId, label)

        usesPlaceholder = false
        if (source != null) loadFromSource(source)
    }

    /**
     * Uploads the source content, or switches to the placeholder. Never throws: this runs inside
     * [GlesResourceRegistry.restoreAll], where an exception would leave every later resource
     * without GL objects.
     */
    private fun loadFromSource(source: TextureSource) {
        val data = try {
            source.load()
        } catch (e: Exception) {
            usePlaceholder("source failed", e)
            return
        }

        val problem = when {
            data == null -> "source returned no data"
            data.format != format -> "source returned ${data.format}, texture is $format"
            data.width > width || data.height > height ->
                "source returned ${data.width}x${data.height}, texture is ${width}x$height"
            data.pixels.remaining() < data.width * data.height * format.bytesPerPixel ->
                "source returned ${data.pixels.remaining()} bytes, " +
                    "${data.width}x${data.height} $format needs ${data.width * data.height * format.bytesPerPixel}"
            else -> null
        }
        if (problem != null) {
            usePlaceholder(problem, null)
            return
        }

        upload(0, 0, 0, data!!)
    }

    private fun usePlaceholder(reason: String, error: Throwable?) {
        usesPlaceholder = true
        Log.w(TAG, "Texture '$label': $reason, using the placeholder", error)
    }

    override fun upload(layer: Int, x: Int, y: Int, data: TextureData) {
        check(!isDisposed) { "Texture '$label' is disposed" }
        require(data.format == format) { "Texture '$label' is $format, data is ${data.format}" }
        require(layer in 0 until layers) { "Layer $layer outside 0 until $layers of '$label'" }
        require(x >= 0 && y >= 0 && x + data.width <= width && y + data.height <= height) {
            "Upload ${data.width}x${data.height} at ($x, $y) outside ${width}x$height of '$label'"
        }
        val byteCount = data.width * data.height * format.bytesPerPixel
        require(data.pixels.remaining() >= byteCount) {
            "Upload to '$label' needs $byteCount bytes, buffer has ${data.pixels.remaining()}"
        }

        val gl = GlesBackend.gl
        val state = GlesBackend.state
        state.bindTexture(glTarget, ownTextureId)
        // A bound unpack buffer would turn the client pointer into an offset.
        state.bindPixelUnpackBuffer(0)

        val singleByte = format == TextureFormat.R8
        if (singleByte) gl.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
        val pixelFormat = GlesResources.pixelFormat(format)
        if (layers > 1) {
            gl.glTexSubImage3D(glTarget, 0, x, y, layer, data.width, data.height, 1, pixelFormat, GLES20.GL_UNSIGNED_BYTE, data.pixels)
        } else {
            gl.glTexSubImage2D(glTarget, 0, x, y, data.width, data.height, pixelFormat, GLES20.GL_UNSIGNED_BYTE, data.pixels)
        }
        if (singleByte) gl.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, GlesTextures.UNPACK_ALIGNMENT_DEFAULT)
        if (levels > 1) gl.glGenerateMipmap(glTarget)
        // The own texture has content again; [textureId] stops pointing at the placeholder, so
        // the next bind picks up the real texture.
        usesPlaceholder = false

        RenderStats.upload(byteCount)
    }

    override fun onContextLost() {
        ownTextureId = 0
    }

    override fun restore() = create()

    override fun dispose() {
        if (isDisposed) return
        val id = ownTextureId
        // Before the texture counts as disposed: queued drawing that uses it is drawn here.
        if (id != 0) GlesBackend.state.onTextureDeleted(id)
        isDisposed = true
        GlesBackend.registry.unregister(this)
        ownTextureId = 0
        if (id != 0) GlesBackend.gl.glDeleteTextures(1, intArrayOf(id), 0)
    }

    override fun toString() = "GlesTexture($label, ${width}x$height x$layers $format)"

    companion object {
        private const val TAG = "GlesTexture"

        /** Length of the full mip chain of a [width] x [height] texture. */
        private fun mipLevels(width: Int, height: Int): Int = 32 - Integer.numberOfLeadingZeros(maxOf(width, height))
    }
}
