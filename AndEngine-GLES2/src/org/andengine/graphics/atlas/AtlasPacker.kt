package org.andengine.graphics.atlas

/** Position of an image inside an atlas page. [x]/[y] point at the image content (padding excluded). */
class AtlasSlot(@JvmField val x: Int, @JvmField val y: Int, @JvmField val width: Int, @JvmField val height: Int)

/**
 * Skyline (bottom-left) rectangle packer for one square page. Every image reserves [padding] pixels on all
 * sides, including towards the page border.
 */
class AtlasPacker(val pageSize: Int, val padding: Int = 2) {

    // Skyline segments: start x, height (y of the free surface) and width.
    private var xs = IntArray(32)
    private var ys = IntArray(32)
    private var ws = IntArray(32)
    private var count = 1

    var usedArea: Int = 0
        private set

    init {
        xs[0] = 0
        ys[0] = 0
        ws[0] = pageSize
    }

    fun tryPlace(width: Int, height: Int): AtlasSlot? {
        val rw = width + 2 * padding
        val rh = height + 2 * padding
        if (width <= 0 || height <= 0 || rw > pageSize || rh > pageSize) return null

        var bestIndex = -1
        var bestY = Int.MAX_VALUE
        var bestWidth = Int.MAX_VALUE
        for (i in 0 until count) {
            val y = fitY(i, rw)
            if (y < 0 || y + rh > pageSize) continue
            if (y < bestY || (y == bestY && ws[i] < bestWidth)) {
                bestIndex = i
                bestY = y
                bestWidth = ws[i]
            }
        }
        if (bestIndex < 0) return null

        val rx = xs[bestIndex]
        addLevel(bestIndex, rx, bestY + rh, rw)
        usedArea += rw * rh
        return AtlasSlot(rx + padding, bestY + padding, width, height)
    }

    /** Lowest y at which a rect of [rw] width fits starting at segment [index], or -1 if it overruns the page. */
    private fun fitY(index: Int, rw: Int): Int {
        val x = xs[index]
        if (x + rw > pageSize) return -1
        var remaining = rw
        var i = index
        var y = 0
        while (remaining > 0) {
            if (ys[i] > y) y = ys[i]
            remaining -= ws[i]
            i++
        }
        return y
    }

    private fun addLevel(index: Int, x: Int, y: Int, w: Int) {
        ensureCapacity(count + 1)
        System.arraycopy(xs, index, xs, index + 1, count - index)
        System.arraycopy(ys, index, ys, index + 1, count - index)
        System.arraycopy(ws, index, ws, index + 1, count - index)
        xs[index] = x
        ys[index] = y
        ws[index] = w
        count++

        // Shrink or remove following segments covered by the new one.
        val end = x + w
        val i = index + 1
        while (i < count && xs[i] < end) {
            val overlap = end - xs[i]
            if (overlap < ws[i]) {
                xs[i] += overlap
                ws[i] -= overlap
                break
            }
            removeAt(i)
        }

        // Merge neighbours of equal height.
        var j = 0
        while (j < count - 1) {
            if (ys[j] == ys[j + 1]) {
                ws[j] += ws[j + 1]
                removeAt(j + 1)
            } else {
                j++
            }
        }
    }

    private fun removeAt(index: Int) {
        val tail = count - index - 1
        System.arraycopy(xs, index + 1, xs, index, tail)
        System.arraycopy(ys, index + 1, ys, index, tail)
        System.arraycopy(ws, index + 1, ws, index, tail)
        count--
    }

    private fun ensureCapacity(n: Int) {
        if (n <= xs.size) return
        val size = xs.size * 2
        xs = xs.copyOf(size)
        ys = ys.copyOf(size)
        ws = ws.copyOf(size)
    }
}

/** Builds padded pixel data for atlas uploads by duplicating the outermost pixel row/column outward. */
object AtlasEdgeExtrusion {

    /** [src] is ARGB, row-major, [width]x[height]. Returns (width+2p)x(height+2p) pixels. */
    fun extrude(src: IntArray, width: Int, height: Int, padding: Int): IntArray {
        val outW = width + 2 * padding
        val outH = height + 2 * padding
        val out = IntArray(outW * outH)
        for (oy in 0 until outH) {
            val sy = (oy - padding).coerceIn(0, height - 1)
            val srcRow = sy * width
            val dstRow = oy * outW
            for (ox in 0 until outW) {
                out[dstRow + ox] = src[srcRow + (ox - padding).coerceIn(0, width - 1)]
            }
        }
        return out
    }
}
