package com.rian.andengine.timing

import kotlin.math.truncate

class StopwatchClock @JvmOverloads constructor(start: Boolean = false) : Stopwatch(), IAdjustableClock {
    private var seekOffset = 0.0

    /**
     * Keep track of how much stopwatch time we have used at previous rates.
     */
    private var rateChangeUsed: Double = 0.0

    /**
     * Keep track of the resultant time that was accumulated at previous rates.
     */
    private var rateChangeAccumulated: Double = 0.0

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
        get() = (stopwatchMilliseconds - rateChangeUsed) * rate + rateChangeAccumulated

    private val stopwatchMilliseconds
        get() = elapsedMilliseconds

    override var rate = 1.0
        set(value) {
            if (field == value) {
                return
            }

            rateChangeAccumulated += (stopwatchMilliseconds - rateChangeUsed) * field
            rateChangeUsed = stopwatchMilliseconds

            field = value
        }

    override fun reset() {
        resetAccumulatedRate()

        super.reset()
    }

    override fun restart() {
        resetAccumulatedRate()

        super.restart()
    }

    override fun resetSpeedAdjustments() {
        rate = 1.0
    }

    override fun seek(position: Double): Boolean {
        // Determine the offset that when added to stopwatchCurrentTime; results in the requested time value
        seekOffset = position - stopwatchCurrentTime
        return true
    }

    override fun toString(): String {
        return "${this::class.simpleName ?: "StopwatchClock"} (${truncate(currentTime)}ms)"
    }

    private fun resetAccumulatedRate() {
        rateChangeAccumulated = 0.0
        rateChangeUsed = 0.0
    }
}