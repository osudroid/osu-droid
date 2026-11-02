package org.andengine.graphics.shape

import android.util.LongSparseArray
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.GlesRestorable
import org.andengine.graphics.gles.RestorePhase
import org.andengine.graphics.text.GlyphAtlas
import org.andengine.graphics.text.GlyphSlot

/**
 * Small R8 coverage masks for UI shapes (solid area, rounded corners, ring corners), kept in the
 * [GlyphAtlas] so shapes draw with the same batch and vertex format as glyphs. Sizes are in
 * physical pixels; converting from virtual units is the caller's job. GL thread only.
 *
 * The first request for a mask rasterizes it on the CPU, allocates a slot and uploads it; later
 * requests return the same slot. After a context loss all known masks are uploaded again into their
 * existing slots (restore phase [RestorePhase.Legacy], i.e. after the atlas textures exist again).
 *
 * The atlas writes a one texel zero border around every image, so a UV on the content edge blends
 * with transparent texels under linear filtering. For the [solid] area use [centerU] / [centerV],
 * which point into the middle of the 4x4 coverage-255 area and never touch the border.
 */
class ShapeMasks(private val atlas: GlyphAtlas) : GlesRestorable {

    private class Entry(val slot: GlyphSlot, val kind: Int, val a: Int, val b: Int)

    private val entries = LongSparseArray<Entry>()

    init {
        GlesBackend.registry.register(this)
    }

    /** A [SOLID_SIZE] x [SOLID_SIZE] area of full coverage. Sample it at [centerU]/[centerV]. */
    fun solid(): GlyphSlot = get(KIND_SOLID, 0, 0)

    /**
     * Quarter circle of [radiusPx] covering the bottom right of a [radiusPx] x [radiusPx] area: the
     * circle center lies at (radiusPx, radiusPx) in texel space, so coverage is 1 at the bottom right
     * and 0 at the top left. Other corners are made by flipping the UVs. The image has one extra
     * column and row at the right and bottom that repeat the last real ones, so sampling right at
     * the center edge does not blend with the atlas border; map the area from [slot].u to
     * [innerU] and [slot].v to [innerV], not to u2/v2.
     */
    fun corner(radiusPx: Int): GlyphSlot = get(KIND_CORNER, clampSize(radiusPx), 0)

    /** Like [corner] but only the band of [widthPx] inside the outer radius [radiusPx]. */
    fun ring(radiusPx: Int, widthPx: Int): GlyphSlot {
        val r = clampSize(radiusPx)
        return get(KIND_RING, r, min(r, max(1, widthPx)))
    }

    override val restorePhase: RestorePhase get() = RestorePhase.Legacy

    override fun onContextLost() {
        synchronized(entries) { for (i in 0 until entries.size()) entries.valueAt(i).slot.uploaded = false }
    }

    override fun restore(backend: GlesBackend) {
        synchronized(entries) { for (i in 0 until entries.size()) {
                val e = entries.valueAt(i)
                atlas.upload(e.slot, raster(e.kind, e.a, e.b))
            } }
    }

    /** Removes this instance from the registry; for owners other than [shared]. */
    fun dispose() {
        GlesBackend.registry.unregister(this)
    }

    /** Radii beyond what the atlas can hold degrade to the largest possible mask. */
    private fun clampSize(px: Int): Int = min(max(1, px), atlas.maxImageSize - 1)

    private fun get(kind: Int, a: Int, b: Int): GlyphSlot {
        val key = (kind.toLong() shl 56) or (a.toLong() shl 28) or b.toLong()
        synchronized(entries) {
            val existing = entries.get(key)
            if (existing != null) {
                if (!existing.slot.uploaded) atlas.upload(existing.slot, raster(kind, a, b))
                return existing.slot
            }
            val size = if (kind == KIND_SOLID) SOLID_SIZE else a + 1
            val slot = atlas.allocate(size, size)
                ?: throw IllegalStateException("Glyph atlas cannot hold a ${size}x$size shape mask")
            entries.put(key, Entry(slot, kind, a, b))
            atlas.upload(slot, raster(kind, a, b))
            return slot
        }
    }

    private fun raster(kind: Int, a: Int, b: Int): ByteBuffer {
        val size = if (kind == KIND_SOLID) SOLID_SIZE else a + 1
        val buf = ByteBuffer.allocateDirect(size * size)
        for (y in 0 until size) for (x in 0 until size) {
            val coverage = when (kind) {
                KIND_SOLID -> 1f
                KIND_CORNER -> coverage(a.toFloat(), min(x, a - 1), min(y, a - 1), a)
                else -> {
                    val cx = min(x, a - 1)
                    val cy = min(y, a - 1)
                    max(0f, coverage(a.toFloat(), cx, cy, a) - coverage((a - b).toFloat(), cx, cy, a))
                }
            }
            buf.put(y * size + x, (coverage * 255f + 0.5f).toInt().toByte())
        }
        return buf
    }

    private fun coverage(radius: Float, x: Int, y: Int, center: Int): Float {
        if (radius <= 0f) return 0f
        val dx = x + 0.5f - center
        val dy = y + 0.5f - center
        val d = sqrt(dx * dx + dy * dy)
        return min(1f, max(0f, radius - d + 0.5f))
    }

    companion object {
        const val SOLID_SIZE = 4

        /** Largest mask radius; bigger shapes magnify the mask. */
        const val MAX_MASK_PX = 512

        /**
         * Rounds a pixel size so continuously changing sizes share a bounded set of masks: exact up to
         * 32, then up to multiples of 4 up to 128, multiples of 16 up to 512, capped at 512. The
         * result is never below 1 and never smaller than [px] unless capped.
         */
        @JvmStatic
        fun quantize(px: Int): Int = when {
            px <= 32 -> max(1, px)
            px <= 128 -> (px + 3) / 4 * 4
            px <= MAX_MASK_PX -> (px + 15) / 16 * 16
            else -> MAX_MASK_PX
        }

        private const val KIND_SOLID = 0
        private const val KIND_CORNER = 1
        private const val KIND_RING = 2

        @JvmStatic
        val shared: ShapeMasks by lazy { ShapeMasks(GlyphAtlas.shared) }

        /** U in the middle of [slot]; for the solid area this stays clear of the zero border. */
        @JvmStatic
        fun centerU(slot: GlyphSlot): Float = (slot.u + slot.u2) * 0.5f

        /** U of the circle-center edge of a corner or ring mask (one texel before the right end). */
        @JvmStatic
        fun innerU(slot: GlyphSlot): Float = slot.u + (slot.u2 - slot.u) * (slot.width - 1) / slot.width

        /** V of the circle-center edge of a corner or ring mask. */
        @JvmStatic
        fun innerV(slot: GlyphSlot): Float = slot.v + (slot.v2 - slot.v) * (slot.height - 1) / slot.height

        /** V in the middle of [slot]. */
        @JvmStatic
        fun centerV(slot: GlyphSlot): Float = (slot.v + slot.v2) * 0.5f
    }
}
