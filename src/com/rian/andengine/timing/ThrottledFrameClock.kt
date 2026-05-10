package com.rian.andengine.timing

import kotlin.math.max

/**
 * A [FramedClock] which will limit the number of frames processed by adding [Thread.sleep] calls on each [processFrame].
 */
class ThrottledFrameClock : FramedClock() {
    /**
     * The target number of updates per second. Only used when [throttling] is true.
     *
     * A value of 0 is treated the same as "unlimited" or [Float.MAX_VALUE].
     */
    var maximumUpdateHz = 1000f

    /**
     * Whether throttling should be enabled. Defaults to `true`.
     */
    var throttling = true

    /**
     * The time spent in a [Thread.sleep] state during the last frame.
     */
    var timeSlept = 0f
        private set

    private var accumulatedSleepError = 0f

    override fun processFrame() {
        super.processFrame()

        if (throttling && maximumUpdateHz > 0 && maximumUpdateHz < Float.MAX_VALUE) {
            throttle()
        } else {
            timeSlept = 0f
        }
    }

    private fun throttle() {
        val excessFrameTime = 1f / maximumUpdateHz - elapsedFrameTime

        timeSlept = sleepAndUpdateCurrent(max(0f, excessFrameTime + accumulatedSleepError))

        accumulatedSleepError += excessFrameTime - timeSlept

        // Never allow the sleep error to become too negative and induce too many catch-up frames.
        accumulatedSleepError = max(-1 / 30f, accumulatedSleepError)
    }

    private fun sleepAndUpdateCurrent(seconds: Float): Float {
        // By returning here, in cases where the game is not keeping up, we don't sleep.
        // Not 100% sure if we want to do this, but let's give it a try.
        if (seconds <= 0) {
            return 0f
        }

        val before = currentTime

        Thread.sleep((seconds * 1e3).toLong())

        currentTime = sourceTime

        return currentTime - before
    }
}