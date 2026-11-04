package org.andengine.graphics.debug

import org.andengine.graphics.GpuQuery
import org.andengine.graphics.GraphicsDevice

/**
 * Measures the GPU time of a frame with [GpuQuery] timer queries.
 *
 * Query results arrive a few frames late, so a small ring of queries is used and each result
 * is read once it is available. Results invalidated by the GPU (disjoint event, e.g. a frequency
 * change) are dropped by the query. The queries are created on the first [begin] and restore
 * themselves after a context loss.
 *
 * All methods must be called on the GL thread, except [invalidate]. Devices without timer queries
 * report [isSupported] = `false`; nothing is created and every call is a no-op.
 */
class GpuFrameTimer(private val device: GraphicsDevice) {

    private val queries = arrayOfNulls<GpuQuery>(RING_SIZE)

    /** Pending results to drop instead of count, see [invalidate]. */
    private val stale = BooleanArray(RING_SIZE)

    private var initialized = false
    private var writeIndex = 0
    private var running = false

    var isSupported = false
        private set

    @Volatile
    private var invalidated = false

    /** Drops all pending results, e.g. after the collected data was reset. Safe on any thread. */
    fun invalidate() {
        invalidated = true
    }

    private fun ensureInitialized() {
        if (invalidated) {
            invalidated = false
            for (i in 0 until RING_SIZE) stale[i] = queries[i]?.isPending == true
        }
        if (initialized) return
        initialized = true

        isSupported = device.capabilities.supportsTimerQuery
        if (!isSupported) return

        for (i in 0 until RING_SIZE) queries[i] = device.createQuery("GpuFrameTimer[$i]")
    }

    fun begin() {
        ensureInitialized()
        if (!isSupported || running) return

        val query = queries[writeIndex]!!
        // Ring is full: the oldest query is still pending, skip this frame instead of stalling.
        if (query.isPending) return

        query.begin()
        running = true
    }

    fun end() {
        if (!running) return
        queries[writeIndex]!!.end()
        writeIndex = (writeIndex + 1) % RING_SIZE
        running = false
    }

    /** Number of frames read by the last [collect] call. */
    var collectedFrames = 0
        private set

    /** Maximum GPU time of a single frame read by the last [collect] call, in nanoseconds. */
    var collectedMaxNanos = 0L
        private set

    /**
     * Reads every finished query and returns the sum of their GPU times in nanoseconds.
     * See [collectedFrames] and [collectedMaxNanos] for the details of the last call.
     */
    fun collect(): Long {
        collectedFrames = 0
        collectedMaxNanos = 0L
        if (!isSupported) return 0L
        ensureInitialized()

        var sum = 0L
        for (i in 0 until RING_SIZE) {
            val query = queries[i]!!
            if (!query.isPending) {
                stale[i] = false
                continue
            }

            val nanos = query.resultNanos()
            // Still waiting for the GPU.
            if (query.isPending) continue

            val drop = stale[i]
            stale[i] = false

            // Some Adreno drivers report ~2^32 ns for queries that did not complete properly.
            // No real frame takes a second on the GPU, so such results are dropped.
            if (nanos >= 0L && !drop && nanos < MAX_PLAUSIBLE_NANOS) {
                sum += nanos
                collectedFrames++
                if (nanos > collectedMaxNanos) collectedMaxNanos = nanos
            }
        }

        return sum
    }

    /** Releases the queries; the timer must not be used afterwards. */
    fun dispose() {
        for (i in 0 until RING_SIZE) {
            queries[i]?.dispose()
            queries[i] = null
        }
        isSupported = false
        running = false
    }

    private companion object {
        const val RING_SIZE = 4
        const val MAX_PLAUSIBLE_NANOS = 1_000_000_000L
    }
}
