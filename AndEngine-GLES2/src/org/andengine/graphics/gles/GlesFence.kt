package org.andengine.graphics.gles

import android.opengl.GLES30
import org.andengine.graphics.GpuFence

/**
 * GL sync object. A context loss drops the fence; it then counts as signaled since the commands
 * it waited for are gone. Not labelled: sync objects need `glObjectPtrLabel`. GL thread only.
 */
class GlesFence(override val label: String) : GpuFence, GlesRestorable {

    private var sync = 0L

    override var isDisposed = false
        private set

    override val restorePhase: RestorePhase get() = RestorePhase.Queries

    init {
        GlesBackend.registry.register(this)
    }

    override fun insert() {
        check(!isDisposed) { "Fence '$label' is disposed" }
        val gl = GlesBackend.gl
        if (sync != 0L) gl.glDeleteSync(sync)
        sync = gl.glFenceSync(GLES30.GL_SYNC_GPU_COMMANDS_COMPLETE, 0)
    }

    override fun isSignaled(): Boolean {
        if (sync == 0L) return true
        val result = GlesBackend.gl.glClientWaitSync(sync, 0, 0L)
        if (result == GLES30.GL_ALREADY_SIGNALED || result == GLES30.GL_CONDITION_SATISFIED) {
            GlesBackend.gl.glDeleteSync(sync)
            sync = 0L
            return true
        }
        return false
    }

    override fun onContextLost() {
        sync = 0L
    }

    override fun restore() {}

    override fun dispose() {
        if (isDisposed) return
        isDisposed = true
        GlesBackend.registry.unregister(this)
        if (sync != 0L) GlesBackend.gl.glDeleteSync(sync)
        sync = 0L
    }

    override fun toString() = "GlesFence($label)"
}
