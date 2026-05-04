package com.rian.andengine.timing

import kotlin.math.max

/**
 * A [FramedClock] which will limit the number of frames processed by adding [Thread.sleep] calls on each [processFrame].
 */
class ThrottledFrameClock : FramedClock() {
    /**
     * The target number of updates per second. Only used when [throttling] is true.
     *
     * A value of 0 is treated the same as "unlimited" or [Double.MAX_VALUE].
     */
    var maximumUpdateHz = 1000.0

    /**
     * Whether throttling should be enabled. Defaults to `true`.
     */
    var throttling = true

    /**
     * The time spent in a [Thread.sleep] state during the last frame.
     */
    var timeSlept = 0.0
        private set

    private var accumulatedSleepError = 0.0

    override fun processFrame() {
        super.processFrame()

        if (throttling) {
            if (maximumUpdateHz > 0 && maximumUpdateHz < Double.MAX_VALUE) {
                throttle()
            } else {
                // Even when running at unlimited frame-rate, we should call the scheduler to give lower-priority
                // background processes a chance to do work.
                timeSlept = sleepAndUpdateCurrent(0.0)
            }
        } else {
            timeSlept = 0.0
        }
    }

    private fun throttle() {
        val excessFrameTime = 1000.0 / maximumUpdateHz - elapsedFrameTime

        timeSlept = sleepAndUpdateCurrent(max(0.0, excessFrameTime + accumulatedSleepError))

        accumulatedSleepError += excessFrameTime - timeSlept

        // Never allow the sleep error to become too negative and induce too many catch-up frames.
        accumulatedSleepError = max(-1000 / 30.0, accumulatedSleepError)
    }

    private fun sleepAndUpdateCurrent(milliseconds: Double): Double {
        // By returning here, in cases where the game is not keeping up, we don't yield.
        // Not 100% sure if we want to do this, but let's give it a try.
        if (milliseconds <= 0) {
            return 0.0
        }

        val before = currentTime

        Thread.sleep(milliseconds.toLong())

        currentTime = sourceTime

        return currentTime - before
    }
}