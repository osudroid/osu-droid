package org.andengine.engine

import org.andengine.engine.Engine.EngineLock

/**
 * An [EngineLock] that counts how often the update is released. It lives in the engine's package because the
 * lock's hand-over methods are package-private.
 */
class CountingEngineLock : EngineLock(false) {
    var released = 0

    override fun notifyCanUpdate() {
        released++
        super.notifyCanUpdate()
    }

    /** Whether the update thread would currently be allowed to run. */
    fun updateMayRun(): Boolean {
        lock()
        try {
            return !mDrawing.get()
        } finally {
            unlock()
        }
    }

    /** Hands the frame to the draw side, the way a finished update tick does. */
    fun handOverToDraw() {
        lock()
        try {
            notifyCanDraw()
        } finally {
            unlock()
        }
    }
}
