package org.andengine.graphics.text

import java.nio.ByteBuffer
import org.andengine.graphics.GraphicsDevice
import org.andengine.graphics.Texture
import org.andengine.graphics.TextureData
import org.andengine.graphics.TextureFilter
import org.andengine.graphics.TextureFormat
import org.andengine.graphics.TextureWrap
import org.andengine.graphics.atlas.AtlasPacker
import org.andengine.graphics.gles.GlesGraphicsDevice

/**
 * One R8 texture array of the [GlyphAtlas]. The GPU texture is created on first access of [texture],
 * which must happen on the GL thread; allocating slots never touches the GPU. The texture comes back
 * empty after a context loss (it has no source to restore from); fonts upload their glyphs again.
 */
class GlyphArray(
    private val device: GraphicsDevice,
    @JvmField val index: Int,
    private val layerSize: Int,
    private val layers: Int
) {
    private var created: Texture? = null

    /** The texture array, created on first access. GL thread only. */
    val texture: Texture
        get() {
            var t = created
            if (t == null) {
                t = device.createTexture(
                    layerSize,
                    layerSize,
                    TextureFormat.R8,
                    layers,
                    TextureFilter.Linear,
                    TextureWrap.Clamp,
                    null,
                    "GlyphAtlas[$index]"
                )
                created = t
            }
            return t
        }

    /** True once the GPU texture exists. */
    val hasTexture: Boolean get() = created != null
}

/** Position of one glyph image in the atlas. UVs sit exactly on the texel edges of the content. */
class GlyphSlot(
    @JvmField val array: GlyphArray,
    @JvmField val layer: Int,
    @JvmField val x: Int,
    @JvmField val y: Int,
    @JvmField val width: Int,
    @JvmField val height: Int,
    @JvmField val u: Float,
    @JvmField val v: Float,
    @JvmField val u2: Float,
    @JvmField val v2: Float
) {
    /**
     * True once the image of this slot was written to the current GPU texture by [GlyphAtlas.upload].
     * Cleared by [Font.invalidateLetters][org.andengine.opengl.font.Font.invalidateLetters] and by
     * [ShapeMasks.onContextLost][org.andengine.graphics.shape.ShapeMasks.onContextLost] when
     * the atlas textures were lost; drawing skips slots that are not uploaded, because the texels
     * of a fresh texture are undefined.
     */
    @Volatile
    @JvmField
    var uploaded = false
}

/**
 * Shared atlas for glyph images: R8 layers of [layerSize], [layersPerArray] layers per texture array,
 * [padding] empty pixels around every glyph. Layers fill one after another; freed space is not reused.
 * [allocate] is CPU-only and thread-safe; [upload] needs the GL thread.
 */
class GlyphAtlas(
    private val device: GraphicsDevice,
    val layerSize: Int = 1024,
    val layersPerArray: Int = 4,
    private val padding: Int = 2
) {
    init {
        require(padding >= 1) { "Glyph padding must be at least 1 for the upload border" }
    }

    private val arrays = ArrayList<GlyphArray>()
    private var packer: AtlasPacker? = null
    private var currentLayer = 0

    /** Largest width or height [allocate] accepts. */
    val maxImageSize: Int get() = layerSize - 2 * padding

    val arrayCount: Int
        @Synchronized get() = arrays.size

    @Synchronized
    fun allocate(width: Int, height: Int): GlyphSlot? {
        val limit = maxImageSize
        if (width <= 0 || height <= 0 || width > limit || height > limit) return null

        if (arrays.isEmpty()) {
            arrays.add(GlyphArray(device, 0, layerSize, layersPerArray))
            packer = AtlasPacker(layerSize, padding)
            currentLayer = 0
        }
        var place = packer!!.tryPlace(width, height)
        while (place == null) {
            if (currentLayer + 1 < layersPerArray) {
                currentLayer++
            } else {
                arrays.add(GlyphArray(device, arrays.size, layerSize, layersPerArray))
                currentLayer = 0
            }
            packer = AtlasPacker(layerSize, padding)
            place = packer!!.tryPlace(width, height)
        }

        val size = layerSize.toFloat()
        return GlyphSlot(
            arrays[arrays.size - 1], currentLayer, place.x, place.y, width, height,
            place.x / size, place.y / size, (place.x + width) / size, (place.y + height) / size
        )
    }

    private var scratch: ByteBuffer? = null

    /**
     * Writes tightly packed R8 [pixels] (width*height bytes, from its position) into [slot] and marks
     * the slot uploaded. The image is written with a 1 texel border of zeros around it, i.e. as
     * (width+2) x (height+2) texels at (x-1, y-1), so bilinear sampling at the content edge never
     * reads undefined storage. Needs a [padding] of at least 1; the padding rings of different
     * slots never overlap. GL thread only; does not allocate once the scratch buffer has grown.
     */
    fun upload(slot: GlyphSlot, pixels: ByteBuffer) {
        val w = slot.width
        val h = slot.height
        val bw = w + 2
        val size = bw * (h + 2)
        var buf = scratch
        if (buf == null || buf.capacity() < size) {
            buf = ByteBuffer.allocateDirect(size)
            scratch = buf
        }
        buf.clear()
        for (i in 0 until size) buf.put(i, 0)
        val start = pixels.position()
        for (row in 0 until h) {
            val dst = (row + 1) * bw + 1
            for (col in 0 until w) buf.put(dst + col, pixels.get(start + row * w + col))
        }
        buf.position(0)
        buf.limit(size)
        slot.array.texture.upload(slot.layer, slot.x - 1, slot.y - 1, TextureData(bw, h + 2, TextureFormat.R8, buf))
        slot.uploaded = true
    }

    companion object {
        @JvmStatic
        val shared: GlyphAtlas by lazy { GlyphAtlas(GlesGraphicsDevice.instance) }
    }
}
