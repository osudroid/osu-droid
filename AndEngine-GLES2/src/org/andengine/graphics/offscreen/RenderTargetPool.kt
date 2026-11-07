package org.andengine.graphics.offscreen

import org.andengine.graphics.RenderTarget
import org.andengine.graphics.gles.GlesGraphicsDevice

/**
 * A render target lent by [RenderTargetPool]. The holder calls [markUsed] whenever it draws the
 * target's content, a lease not used for [RenderTargetPool.UNUSED_FRAMES] frames is taken back
 * ([isValid] becomes false), so the targets of entities that are dropped without being detached
 * (e.g. with their whole scene) return to the pool. The holder must check [isValid] before using
 * [target] and acquire a new lease once it is false. GL thread only.
 */
class RenderTargetLease internal constructor(val target: RenderTarget) {

    var isValid = true
        internal set

    internal var lastUsedFrame = RenderTargetPool.frame

    /** Keeps the lease for another [RenderTargetPool.UNUSED_FRAMES] frames. */
    fun markUsed() {
        lastUsedFrame = RenderTargetPool.frame
    }
}

/**
 * Lends render targets with a depth buffer, so that offscreen content of short-lived entities does
 * not create a framebuffer each time. Sizes are rounded up to [SIZE_STEP], so a target fits many
 * requests, at most [MAX_FREE] returned targets are kept. GL thread only.
 */
object RenderTargetPool {

    /** Sizes are rounded up to a multiple of this. */
    const val SIZE_STEP = 256

    /** Returned targets kept for reuse; more are disposed. */
    const val MAX_FREE = 6

    /** Frames after which an unused lease is taken back. */
    const val UNUSED_FRAMES = 120

    private val free = ArrayList<RenderTarget>()
    private val leases = ArrayList<RenderTargetLease>()

    /** Counts the frames, see [onFrame]. */
    internal var frame = 0L
        private set

    /** The largest width and height a target can have. */
    @JvmStatic
    val maxSize: Int get() = GlesGraphicsDevice.instance.capabilities.maxTextureSize

    /** Lends a target of at least [width] x [height] pixels (at most [maxSize]), with a depth buffer. */
    @JvmStatic
    fun acquire(width: Int, height: Int): RenderTargetLease {
        var best = -1
        for (i in free.indices) {
            val t = free[i]
            if (t.width < width || t.height < height) continue
            if (best < 0 || t.width * t.height < free[best].width * free[best].height) best = i
        }
        val target = if (best >= 0) {
            free.removeAt(best)
        } else {
            val max = maxSize
            GlesGraphicsDevice.instance.createRenderTarget(
                roundUp(width).coerceAtMost(max), roundUp(height).coerceAtMost(max), true, "offscreen"
            )
        }
        return RenderTargetLease(target).also { leases.add(it) }
    }

    /** Gives the target of [lease] back for reuse; does nothing for a lease no longer valid. */
    @JvmStatic
    fun release(lease: RenderTargetLease) {
        if (!lease.isValid) return
        lease.isValid = false
        leases.remove(lease)
        free.add(lease.target)
        while (free.size > MAX_FREE) {
            var largest = 0
            for (i in free.indices) {
                if (free[i].width * free[i].height > free[largest].width * free[largest].height) largest = i
            }
            free.removeAt(largest).dispose()
        }
    }

    /** Called once per frame by [OffscreenRenderer]: takes back the leases unused for [UNUSED_FRAMES] frames. */
    internal fun onFrame() {
        frame++
        for (i in leases.indices.reversed()) {
            val lease = leases[i]
            if (frame - lease.lastUsedFrame > UNUSED_FRAMES) release(lease)
        }
    }

    /** Takes back every lease and disposes all targets, e.g. for shutdown and tests. */
    @JvmStatic
    fun clear() {
        for (lease in leases.toList()) release(lease)
        for (t in free) t.dispose()
        free.clear()
    }

    private fun roundUp(size: Int) = (size + SIZE_STEP - 1) / SIZE_STEP * SIZE_STEP
}
