package org.andengine.graphics.debug

import android.opengl.GLES30

/**
 * Measures the GPU time of a frame with `GL_EXT_disjoint_timer_query`.
 *
 * Query results arrive a few frames late, so a small ring of queries is used and each result
 * is read once it is available. Results from frames where the GPU reported a disjoint event
 * (e.g. a frequency change) are dropped.
 *
 * All methods must be called on the GL thread. Devices without the extension report
 * [isSupported] = `false` and every call is a no-op.
 */
internal class GpuFrameTimer {

    private val queries = IntArray(RING_SIZE)
    private val pending = BooleanArray(RING_SIZE)
    private val result = IntArray(1)

    private var initialized = false
    private var writeIndex = 0
    private var running = false

    var isSupported = false
        private set

    /** Invalidates all queries, e.g. after the GL context was recreated. Safe on any thread. */
    @Volatile
    private var invalidated = true

    fun invalidate() {
        invalidated = true
    }

    private fun ensureInitialized() {
        if (!invalidated) return
        invalidated = false
        initialized = false
        running = false
        pending.fill(false)

        val extensions = GLES30.glGetString(GLES30.GL_EXTENSIONS) ?: ""
        isSupported = extensions.contains("GL_EXT_disjoint_timer_query")
        if (!isSupported) return

        // Old query names belong to the lost context and are not deleted.
        GLES30.glGenQueries(RING_SIZE, queries, 0)
        clearDisjoint()
        initialized = true
    }

    fun begin() {
        ensureInitialized()
        if (!initialized || running) return

        // Ring is full: the oldest query is still pending, skip this frame instead of stalling.
        if (pending[writeIndex]) return

        GLES30.glBeginQuery(GL_TIME_ELAPSED_EXT, queries[writeIndex])
        running = true
    }

    fun end() {
        if (!running) return
        GLES30.glEndQuery(GL_TIME_ELAPSED_EXT)
        pending[writeIndex] = true
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
        if (!initialized) return 0L

        var sum = 0L
        val disjoint = clearDisjoint()

        for (i in 0 until RING_SIZE) {
            if (!pending[i]) continue

            GLES30.glGetQueryObjectuiv(queries[i], GLES30.GL_QUERY_RESULT_AVAILABLE, result, 0)
            if (result[0] == 0) continue

            GLES30.glGetQueryObjectuiv(queries[i], GLES30.GL_QUERY_RESULT, result, 0)
            pending[i] = false

            val nanos = result[0].toLong() and 0xFFFFFFFFL

            // Some Adreno drivers report ~2^32 ns for queries that did not complete properly.
            // No real frame takes a second on the GPU, so such results are dropped.
            if (!disjoint && nanos < MAX_PLAUSIBLE_NANOS) {
                sum += nanos
                collectedFrames++
                if (nanos > collectedMaxNanos) collectedMaxNanos = nanos
            }
        }

        return sum
    }

    /** Reads and thereby resets the disjoint flag. */
    private fun clearDisjoint(): Boolean {
        GLES30.glGetIntegerv(GL_GPU_DISJOINT_EXT, result, 0)
        return result[0] != 0
    }

    private companion object {
        const val RING_SIZE = 4
        const val MAX_PLAUSIBLE_NANOS = 1_000_000_000L
        const val GL_TIME_ELAPSED_EXT = 0x88BF
        const val GL_GPU_DISJOINT_EXT = 0x8FBB
    }
}
