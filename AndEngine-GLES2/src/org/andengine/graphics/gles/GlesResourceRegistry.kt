package org.andengine.graphics.gles

import android.util.Log
import org.andengine.graphics.GpuResource

/** Order in which [GlesResourceRegistry.restoreAll] restores entries after a new context. */
enum class RestorePhase { Programs, Buffers, Textures, Samplers, VertexArrays, Framebuffers, Queries, Legacy }

/** Something that owns GL objects and must be rebuilt when the GL context is recreated. */
interface GlesRestorable {

    val restorePhase: RestorePhase

    /** Forget all GL ids. Must not call GL: the old context is already gone. */
    fun onContextLost()

    /** Recreate the GL objects in the current (new) context. */
    fun restore()
}

/**
 * Strongly holds every [GlesRestorable] and replays them on a new context. Register/unregister may
 * happen on any thread; [restoreAll] runs on the GL thread.
 */
class GlesResourceRegistry {

    private val entries = ArrayList<GlesRestorable>()

    /** The number of registered resources, read by tests. */
    val size: Int get() = synchronized(entries) { entries.size }

    /** Adds [r]; registering the same object again is ignored, so it is restored only once. */
    fun register(r: GlesRestorable) {
        synchronized(entries) {
            for (i in entries.indices) {
                if (entries[i] === r) return
            }
            entries += r
        }
    }

    fun unregister(r: GlesRestorable) {
        synchronized(entries) { entries -= r }
    }

    /**
     * Calls [GlesRestorable.onContextLost] on every entry, then [GlesRestorable.restore] sorted by
     * [RestorePhase], in registration order within a phase. Disposed [GpuResource]s are skipped.
     * Each call is isolated: an entry that throws is logged (tag [TAG]) and the remaining entries
     * are still processed, so one broken resource cannot leave the rest of the context unrestored.
     */
    fun restoreAll() {
        // Snapshot so restorers may register or unregister without breaking the iteration.
        val snapshot = synchronized(entries) { entries.toTypedArray() }

        for (i in snapshot.indices) {
            val r = snapshot[i]
            if (isDisposed(r)) continue
            try {
                r.onContextLost()
            } catch (e: RuntimeException) {
                Log.e(TAG, "onContextLost failed for ${describe(r)}", e)
            }
        }

        val phases = RestorePhase.entries
        for (p in phases.indices) {
            val phase = phases[p]
            for (i in snapshot.indices) {
                val r = snapshot[i]
                if (r.restorePhase != phase || isDisposed(r)) continue
                try {
                    r.restore()
                } catch (e: RuntimeException) {
                    Log.e(TAG, "restore ($phase) failed for ${describe(r)}", e)
                }
            }
        }
    }

    private fun isDisposed(r: GlesRestorable) = r is GpuResource && r.isDisposed

    private fun describe(r: GlesRestorable): String = if (r is GpuResource) "'${r.label}' ($r)" else r.toString()

    companion object {
        const val TAG = "GlesRegistry"
    }
}
