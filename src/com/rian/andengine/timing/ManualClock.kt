package com.rian.andengine.timing

/**
 * A completely manual [IClock] implementation. Everything is settable.
 */
open class ManualClock : IClock {
    override var currentTime = 0f
    override val rate = 1f
    override val isRunning = false
}