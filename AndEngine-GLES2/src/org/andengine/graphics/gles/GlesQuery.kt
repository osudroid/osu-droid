package org.andengine.graphics.gles

import android.opengl.GLES30
import android.opengl.GLES32
import org.andengine.graphics.GpuQuery

/**
 * GPU timer query (`GL_TIME_ELAPSED_EXT` from `GL_EXT_disjoint_timer_query`); only creatable when
 * [GlesCapabilities.hasTimerQuery]. A result is dropped when the GPU reports a disjoint event
 * (e.g. a frequency change) at the time it is read. A context loss drops a running or pending
 * measurement. GL thread only.
 */
class GlesQuery(override val label: String) : GpuQuery, GlesRestorable {

    /** The GL query; 0 while there is none. */
    var queryId = 0
        private set

    private var active = false
    private var labelled = false
    private val scratch = IntArray(1)

    override var isPending = false
        private set

    override var isDisposed = false
        private set

    override val restorePhase: RestorePhase get() = RestorePhase.Queries

    init {
        check(GlesBackend.caps.hasTimerQuery) { "Query '$label': GL_EXT_disjoint_timer_query is not supported" }
        create()
        GlesBackend.registry.register(this)
    }

    private fun create() {
        GlesBackend.gl.glGenQueries(1, scratch, 0)
        queryId = scratch[0]
        active = false
        isPending = false
        // A query object only exists (and can be labelled) after its first begin.
        labelled = false
    }

    override fun begin() {
        if (active || isDisposed) return
        GlesBackend.gl.glBeginQuery(GL_TIME_ELAPSED_EXT, queryId)
        active = true
        isPending = false
        if (!labelled) {
            labelled = true
            GlesResources.label(GLES32.GL_QUERY, queryId, label)
        }
    }

    override fun end() {
        if (!active) return
        GlesBackend.gl.glEndQuery(GL_TIME_ELAPSED_EXT)
        active = false
        isPending = true
    }

    override fun resultNanos(): Long {
        if (!isPending) return NO_RESULT
        val gl = GlesBackend.gl
        gl.glGetQueryObjectuiv(queryId, GLES30.GL_QUERY_RESULT_AVAILABLE, scratch, 0)
        if (scratch[0] == 0) return NO_RESULT

        gl.glGetQueryObjectuiv(queryId, GLES30.GL_QUERY_RESULT, scratch, 0)
        isPending = false
        val nanos = scratch[0].toLong() and 0xFFFFFFFFL

        // Reading the flag also resets it.
        gl.glGetIntegerv(GL_GPU_DISJOINT_EXT, scratch, 0)
        return if (scratch[0] != 0) NO_RESULT else nanos
    }

    override fun onContextLost() {
        queryId = 0
        active = false
        isPending = false
    }

    override fun restore(backend: GlesBackend) = create()

    override fun dispose() {
        if (isDisposed) return
        isDisposed = true
        GlesBackend.registry.unregister(this)
        val id = queryId
        if (active) GlesBackend.gl.glEndQuery(GL_TIME_ELAPSED_EXT)
        queryId = 0
        active = false
        isPending = false
        if (id != 0) GlesBackend.gl.glDeleteQueries(1, intArrayOf(id), 0)
    }

    override fun toString() = "GlesQuery($label)"

    companion object {
        const val GL_TIME_ELAPSED_EXT = 0x88BF
        const val GL_GPU_DISJOINT_EXT = 0x8FBB
        const val NO_RESULT = -1L
    }
}
