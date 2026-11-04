package org.andengine.graphics

import java.nio.ByteBuffer

enum class TextureFormat {
    /** 8 bits per channel, red/green/blue/alpha. 4 bytes per pixel. */
    RGBA8,

    /** One 8-bit channel. 1 byte per pixel. */
    R8;

    val bytesPerPixel: Int get() = if (this == RGBA8) 4 else 1
}

enum class TextureFilter { Nearest, Linear, LinearMipmapLinear }

enum class TextureWrap { Clamp, Repeat, MirroredRepeat }

/**
 * Pixels for an upload: straight (not premultiplied) alpha, rows tightly packed, read from the
 * buffer's current position.
 */
class TextureData(val width: Int, val height: Int, val format: TextureFormat, val pixels: ByteBuffer)

/**
 * Supplies the content of a texture, at creation and again after the GPU context was lost.
 * Returning `null` means the content is not available; the backend then shows a placeholder.
 */
fun interface TextureSource {
    fun load(): TextureData?
}

interface Texture : GpuResource {
    val width: Int
    val height: Int

    /** 1 for a plain 2D texture, more than 1 for a texture array. */
    val layers: Int
    val format: TextureFormat

    /**
     * Writes [data] into [layer] at ([x], [y]); [data] must have this texture's [format]. A
     * successful upload also ends the placeholder a failed [TextureSource] put in place, so the
     * texture shows its own content again (the parts not written stay as they were).
     */
    fun upload(layer: Int, x: Int, y: Int, data: TextureData)
}
