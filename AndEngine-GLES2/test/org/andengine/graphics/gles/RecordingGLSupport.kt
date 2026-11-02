package org.andengine.graphics.gles

import android.opengl.GLES20
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.andengine.graphics.shape.ShapeBatching
import org.andengine.graphics.shape.ShapeMasks
import org.andengine.graphics.text.GlyphAtlas
import org.andengine.graphics.text.GlyphMetrics
import org.andengine.graphics.text.GlyphRasterizer
import org.andengine.graphics.texture.Pixels
import org.andengine.opengl.texture.ITexture
import org.andengine.opengl.texture.TextureManager
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.junit.Assert.assertTrue

/** The 4x4 identity matrix, column-major. Never written to. */
val IDENTITY = floatArrayOf(
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    0f, 0f, 0f, 1f
)

/**
 * Makes this fake the current context, with 16 texture units and [maxTextureSize] px textures,
 * and clears the calls the context setup made.
 */
fun RecordingGL.install(maxTextureSize: Int = 4096): RecordingGL {
    intValues[GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS] = 16
    intValues[GLES20.GL_MAX_TEXTURE_SIZE] = maxTextureSize
    GlesBackend.onContextCreated(this)
    calls.clear()
    return this
}

/** A new [RecordingGL] installed as the current context, see [install]. */
fun installRecordingGL(maxTextureSize: Int = 4096): RecordingGL = RecordingGL().install(maxTextureSize)

/** The number of indexed draws, the way the quad batcher draws. */
fun RecordingGL.draws(): Int = calls.count { it.startsWith("glDrawElements") }

/** The number of non-indexed draws, the way the render context draws a mesh. */
fun RecordingGL.arrayDraws(): Int = calls.count { it.startsWith("glDrawArrays") }

/** The bytes of the last vertex upload, in the device's byte order. */
fun RecordingGL.lastVertices(): ByteBuffer = ByteBuffer.wrap(vertexUploads.last()).order(ByteOrder.nativeOrder())

/** The `glTexSubImage2D` calls that uploaded bytes. */
fun RecordingGL.subImageCalls(): List<String> = calls.filter { it.startsWith("glTexSubImage2D(") && it.endsWith(",buf)") }

/** The bitmap uploads (converted by getPixels) as `bitmap(x,y,width,height)`. */
fun RecordingGL.bitmapUploads(): List<String> = calls.filter { it.startsWith("glTexSubImage2D(") && it.endsWith(",ints)") }.map { call ->
    val args = call.removePrefix("glTexSubImage2D(").removeSuffix(")").split(",")
    "bitmap(${args[2]},${args[3]},${args[4]},${args[5]})"
}

/** The index of [call] in the recorded calls; fails if it was not made. */
fun RecordingGL.indexOfCall(call: String): Int {
    val index = calls.indexOf(call)
    assertTrue("$call missing in $calls", index >= 0)
    return index
}

/** The last call starting with [prefix] before the call at [index], or null. */
fun RecordingGL.lastCallBefore(index: Int, prefix: String): String? =
    calls.subList(0, index).lastOrNull { it.startsWith(prefix) }

/** The bytes of [pixels], from its data's position to its limit. */
fun bytesOf(pixels: Pixels): ByteArray {
    val copy = pixels.data.duplicate()
    val bytes = ByteArray(copy.remaining())
    copy.get(bytes)
    return bytes
}

/** An [ITexture] of [width] x [height] whose hardware texture id is read from [id] on every call. */
fun proxyTexture(width: Int, height: Int, id: () -> Int): ITexture {
    lateinit var self: ITexture
    self = Proxy.newProxyInstance(ITexture::class.java.classLoader, arrayOf(ITexture::class.java)) { _, method, args ->
        when (method.name) {
            "getHardwareTextureID" -> id()
            "getWidth" -> width
            "getHeight" -> height
            "hashCode" -> System.identityHashCode(self)
            "equals" -> self === args!![0]
            "toString" -> "ProxyTexture"
            else -> throw UnsupportedOperationException(method.name)
        }
    } as ITexture
    return self
}

/** Shape masks on a fresh 256 px glyph atlas, installed for [ShapeBatching]. */
fun installShapeMasks(atlas: GlyphAtlas = GlyphAtlas(GlesGraphicsDevice.instance, 256, 2, 2)): ShapeMasks {
    val masks = ShapeMasks(atlas)
    ShapeBatching.masks = masks
    return masks
}

/** Takes [masks] out of [ShapeBatching] and disposes them together with their texture. */
fun disposeShapeMasks(masks: ShapeMasks) {
    ShapeBatching.masks = null
    val array = masks.solid().array
    masks.dispose()
    if (array.hasTexture) array.texture.dispose()
}

/** A [width] x [height] page that reports itself as uploaded under the texture name [id]. */
class Page(id: Int, width: Int = 16, height: Int = 16) :
    BitmapTextureAtlas(TextureManager(), width, height, TextureOptions.BILINEAR) {
    init {
        mHardwareTextureID = id
    }
}

/**
 * Glyphs without a font file: every visible glyph is 20x30 raster pixels with an advance of 22, the
 * space has no pixels and an advance of 12. Rasterizes up to [layers] layers, each byte set to the
 * layer index, and records every rasterized glyph as char plus layer in [rasterized].
 */
class FakeRasterizer(private val layers: Int = 1) : GlyphRasterizer {
    val rasterized = ArrayList<String>()

    override val ascent: Float get() = -40f
    override val descent: Float get() = 10f

    override fun measure(c: Char, out: GlyphMetrics) {
        if (c == ' ') {
            out.width = 0
            out.height = 0
            out.offsetX = 0f
            out.offsetY = 0f
            out.advance = 12f
            return
        }
        out.width = 20
        out.height = 30
        out.offsetX = 2f
        out.offsetY = -30f
        out.advance = 22f
    }

    override fun rasterize(c: Char, layer: Int, out: ByteBuffer) {
        require(layer < layers)
        rasterized += "$c$layer"
        for (i in 0 until 20 * 30) out.put(layer.toByte())
    }
}

/** Ends the render pass a test left open, so the next test starts outside a pass. */
fun endOpenPass() {
    val context = GlesRenderContext.instance
    if (context.isInPass) context.endPass()
}
