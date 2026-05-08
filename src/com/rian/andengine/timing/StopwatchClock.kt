package com.rian.andengine.timing

import kotlin.math.truncate

/**
 * A wrapper around [Stopwatch] which implements [IAdjustableClock], allowing it to be used as an [IClock] with
 * adjustable rate and seek functionality.
 *
 * @param start Whether to start this [StopwatchClock] immediately.
 */
open class StopwatchClock @JvmOverloads constructor(start: Boolean = false) : Stopwatch(), IAdjustableClock {
    private var seekOffset = 0f

    /**
     * Keep track of how much stopwatch time we have used at previous rates.
     */
    private var rateChangeUsed = 0f

    /**
     * Keep track of the resultant time that was accumulated at previous rates.
     */
    private var rateChangeAccumulated = 0f

    init {
        if (start) {
            start()
        }
    }

    override val currentTime
        get() = stopwatchCurrentTime + seekOffset

    /**
     * The current time, represented solely by the accumulated [Stopwatch] time.
     */
    private val stopwatchCurrentTime
        get() = (stopwatchSeconds - rateChangeUsed) * rate + rateChangeAccumulated

    private val stopwatchSeconds
        get() = elapsedSeconds

    override var rate = 1f
        set(value) {
            if (field == value) {
                return
            }

            rateChangeAccumulated += (stopwatchSeconds - rateChangeUsed) * field
            rateChangeUsed = stopwatchSeconds

            field = value
        }

    override fun reset() {
        resetAccumulatedRate()
        seekOffset = 0f

        super.reset()
    }

    override fun restart() {
        resetAccumulatedRate()
        seekOffset = 0f

        super.restart()
    }

    override fun resetSpeedAdjustments() {
        rate = 1f
    }

    override fun seek(position: Float): Boolean {
        // Determine the offset that when added to stopwatchCurrentTime; results in the requested time value
        seekOffset = position - stopwatchCurrentTime
        return true
    }

    override fun toString() = "${this::class.simpleName ?: "StopwatchClock"} (${truncate(currentTime * 1e3)}ms)"

    private fun resetAccumulatedRate() {
        rateChangeAccumulated = 0f
        rateChangeUsed = 0f
    }
}