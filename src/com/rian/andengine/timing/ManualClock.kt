package com.rian.andengine.timing

/**
 * A completely manual [IClock] implementation. Everything is settable.
 */
open class ManualClock : IClock {
    override var currentTime = 0.0
    override val rate = 1.0
    override val isRunning = false
}