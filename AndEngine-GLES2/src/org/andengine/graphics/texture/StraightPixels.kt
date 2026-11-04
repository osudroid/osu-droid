package org.andengine.graphics.texture

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * An image as RGBA bytes: R, G, B, A per pixel, rows tightly packed, read from [data]'s position to
 * its limit. [data] is a direct buffer, ready for a GL upload. The colors are straight (not
 * premultiplied) unless [premultiplied], in which case they hold the bytes of a premultiplied bitmap.
 */
class Pixels @JvmOverloads constructor(
    val width: Int,
    val height: Int,
    val data: ByteBuffer,
    val premultiplied: Boolean = false
)

/**
 * Decodes images into [Pixels]. By default alpha is not premultiplied, so colors at partial alpha
 * stay as they are in the file; the premultiplied variant keeps the bytes of the platform's
 * premultiplied decoding as they are. The intermediate bitmap is recycled before returning; only the
 * direct buffer of the result remains.
 */
object StraightPixels {

    /**
     * Decodes the image file at [path], or returns `null` if it cannot be read or decoded. With
     * [premultiplied] the colors are premultiplied by alpha.
     */
    @JvmStatic
    @JvmOverloads
    fun decodeFile(path: String, inSampleSize: Int = 1, premultiplied: Boolean = false): Pixels? =
        try {
            FileInputStream(path).use { decode(it, inSampleSize, premultiplied) }
        } catch (e: IOException) {
            null
        }

    /**
     * Decodes [stream] (left open), or returns `null` if it holds no decodable image. With
     * [premultiplied] the colors are premultiplied by alpha, byte for byte as a premultiplied
     * ARGB_8888 bitmap holds them in memory.
     */
    @JvmStatic
    @JvmOverloads
    fun decode(stream: InputStream, inSampleSize: Int = 1, premultiplied: Boolean = false): Pixels? {
        val options = BitmapFactory.Options()
        options.inPremultiplied = premultiplied
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        options.inSampleSize = inSampleSize
        var bitmap = BitmapFactory.decodeStream(stream, null, options) ?: return null
        if (bitmap.config != Bitmap.Config.ARGB_8888) {
            // Decoders may ignore the preferred config (e.g. hardware or wide-gamut images).
            val converted = bitmap.copy(Bitmap.Config.ARGB_8888, false)
            bitmap.recycle()
            bitmap = converted ?: return null
        }
        if (premultiplied) {
            // A decoder that kept alpha straight: no premultiplied bytes, the caller falls back.
            if (!bitmap.isPremultiplied && bitmap.hasAlpha()) {
                bitmap.recycle()
                return null
            }
            return fromPremultipliedBitmap(bitmap)
        }
        // A decoder that ignored inPremultiplied = false: no straight bytes, the caller falls back.
        if (bitmap.isPremultiplied && bitmap.hasAlpha()) {
            bitmap.recycle()
            return null
        }
        return fromBitmap(bitmap)
    }

    /**
     * Copies [bitmap] into [Pixels] and recycles it, also when this throws. [bitmap] must be
     * ARGB_8888 and either not premultiplied or without alpha, since its memory is copied as it is
     * (ARGB_8888 is laid out as the bytes R, G, B, A). Pixels with alpha 0 get RGB 0, as the
     * premultiplied decoding gave them.
     */
    @JvmStatic
    fun fromBitmap(bitmap: Bitmap): Pixels {
        try {
            require(bitmap.config == Bitmap.Config.ARGB_8888) { "Expected ARGB_8888, got ${bitmap.config}" }
            require(!bitmap.isPremultiplied || !bitmap.hasAlpha()) { "Bitmap must hold straight alpha" }

            val data = copyMemory(bitmap)
            val swap = blueFirstInMemory
            // Without alpha and in R, G, B, A order there is nothing to change.
            if (bitmap.hasAlpha() || swap) normalize(data, swap, clearTransparent = true)
            return Pixels(bitmap.width, bitmap.height, data)
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Copies [bitmap] into premultiplied [Pixels] and recycles it, also when this throws. [bitmap]
     * must be ARGB_8888 and premultiplied (or without alpha). Its memory is copied as it is, so the
     * bytes are exactly those a bitmap upload of it would send.
     */
    @JvmStatic
    fun fromPremultipliedBitmap(bitmap: Bitmap): Pixels {
        try {
            require(bitmap.config == Bitmap.Config.ARGB_8888) { "Expected ARGB_8888, got ${bitmap.config}" }
            require(bitmap.isPremultiplied || !bitmap.hasAlpha()) { "Bitmap must hold premultiplied alpha" }

            val data = copyMemory(bitmap)
            val swap = blueFirstInMemory
            if (swap) normalize(data, swap = true, clearTransparent = false)
            return Pixels(bitmap.width, bitmap.height, data, premultiplied = true)
        } finally {
            bitmap.recycle()
        }
    }

    /** The memory of the ARGB_8888 [bitmap] in a direct buffer, without row padding. */
    private fun copyMemory(bitmap: Bitmap): ByteBuffer {
        val width = bitmap.width
        val height = bitmap.height
        val data = ByteBuffer.allocateDirect(width * height * 4)
        if (bitmap.rowBytes == width * 4) {
            bitmap.copyPixelsToBuffer(data)
        } else {
            // Padded rows: copy the whole bitmap and drop the padding of each row.
            val padded = ByteBuffer.allocateDirect(bitmap.rowBytes * height)
            bitmap.copyPixelsToBuffer(padded)
            for (y in 0 until height) {
                padded.limit(y * bitmap.rowBytes + width * 4).position(y * bitmap.rowBytes)
                data.put(padded)
            }
        }
        data.flip()
        return data
    }

    /**
     * Android stores ARGB_8888 as R, G, B, A. Desktop builds of the graphics library (as used by
     * host-side tests) store B, G, R, A instead; this is detected once and corrected in place.
     */
    private val blueFirstInMemory: Boolean by lazy {
        val probe = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        probe.setPixel(0, 0, 0xFFFF0000.toInt())
        val bytes = ByteBuffer.allocate(4)
        probe.copyPixelsToBuffer(bytes)
        probe.recycle()
        bytes.get(0).toInt() == 0 && bytes.get(2).toInt() != 0
    }

    /** Pixels per bulk transfer in [normalize]: a 64 KiB scratch array per call. */
    private const val CHUNK_PIXELS = 16384

    /**
     * Clears RGB of fully transparent pixels if [clearTransparent] and, if [swap], turns B, G, R, A
     * into R, G, B, A. Works on ints in bulk chunks, since per-byte buffer access is far too slow for
     * large images.
     */
    private fun normalize(data: ByteBuffer, swap: Boolean, clearTransparent: Boolean) {
        val ints = data.duplicate().order(ByteOrder.nativeOrder()).asIntBuffer()
        val littleEndian = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN
        // Byte 3 is alpha; bytes 0 and 2 are the channels to swap.
        val alphaMask = if (littleEndian) -0x1000000 else 0xFF
        val byte0Shift = if (littleEndian) 0 else 24
        val byte2Shift = if (littleEndian) 16 else 8
        val keepMask = ((0xFF shl byte0Shift) or (0xFF shl byte2Shift)).inv()

        val total = ints.remaining()
        val scratch = IntArray(minOf(CHUNK_PIXELS, total))
        var offset = 0
        while (offset < total) {
            val count = minOf(scratch.size, total - offset)
            ints.position(offset)
            ints.get(scratch, 0, count)
            for (i in 0 until count) {
                val pixel = scratch[i]
                scratch[i] = when {
                    clearTransparent && pixel and alphaMask == 0 -> 0
                    swap -> (pixel and keepMask) or
                        (((pixel ushr byte0Shift) and 0xFF) shl byte2Shift) or
                        (((pixel ushr byte2Shift) and 0xFF) shl byte0Shift)
                    else -> pixel
                }
            }
            ints.position(offset)
            ints.put(scratch, 0, count)
            offset += count
        }
    }
}
