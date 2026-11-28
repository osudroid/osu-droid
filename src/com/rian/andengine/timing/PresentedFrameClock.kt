package com.rian.andengine.timing

import kotlin.math.ceil
import kotlin.math.max
import org.anddev.andengine.util.annotation.GLThread
import org.anddev.andengine.util.annotation.UpdateThread

/**
 * An [IFrameBasedClock] that measures the rate at which frames are presented to the display.
 */
class PresentedFrameClock : IFrameBasedClock {
    /**
     * Whether present timestamps are being fed to this clock. Not changed by [reset].
     *
     * Only written during the draw phase.
     */
    @set:GLThread
    var isAvailable = false

    /**
     * The longest gap between presented frames made readable by the last [processFrame] call, in seconds, or 0 if
     * there were none.
     */
    var longestFrameTime = 0f
        private set

    /**
     * The time covered by presented frames since the last [reset], in seconds. Informational only.
     */
    override var currentTime = 0f
        private set

    /**
     * The gap between the last two presented frames made readable by the last [processFrame] call, in seconds, or 0
     * if there were none.
     */
    override var elapsedFrameTime = 0f
        private set

    override var framesPerSecond = 0f
        private set

    override val rate = 1f

    override val isRunning
        get() = isAvailable

    private val _timeInfo = FrameTimeInfo()

    override val timeInfo
        get() = _timeInfo.apply {
            current = currentTime
            elapsed = elapsedFrameTime
        }

    // Pending state, accumulated by onFramePresented and consumed by processFrame. Everything is in nanoseconds.
    private var lastPresentTimeNanos = Long.MIN_VALUE
    private var pendingGapCount = 0
    private var pendingGapSumNanos = 0L
    private var pendingLastGapNanos = 0L
    private var pendingLongestGapNanos = 0L
    private var isResetPending = false

    // Published state.
    private var totalGapNanos = 0L

    // Frame rate bookkeeping. This mirrors FramedClock.
    private var timeUntilNextCalculation = FPS_CALCULATION_INTERVAL
    private var timeSinceLastCalculation = 0f
    private var framesSinceLastCalculation = 0

    /**
     * Records a frame that has been presented to the display. It becomes readable on the next [processFrame] call.
     *
     * Only called during the draw phase.
     *
     * @param presentTimeNanos The time at which the frame was presented, in nanoseconds.
     */
    @GLThread
    fun onFramePresented(presentTimeNanos: Long) {
        val previous = lastPresentTimeNanos

        if (previous == Long.MIN_VALUE) {
            // First frame: there is no gap to count yet.
            lastPresentTimeNanos = presentTimeNanos
            return
        }

        val gap = presentTimeNanos - previous

        if (gap <= 0L) {
            // Out-of-order or duplicate timestamp.
            return
        }

        lastPresentTimeNanos = presentTimeNanos

        if (gap > MAX_GAP_NANOS) {
            // The gap is too long to be meaningful, so this frame resets the measurement.
            return
        }

        ++pendingGapCount
        pendingGapSumNanos += gap
        pendingLastGapNanos = gap
        pendingLongestGapNanos = max(pendingLongestGapNanos, gap)
    }

    /**
     * Resets the measurement. The readable values are cleared on the next [processFrame] call.
     *
     * Only called during the draw phase.
     */
    @GLThread
    fun reset() {
        lastPresentTimeNanos = Long.MIN_VALUE
        clearPending()
        isResetPending = true
    }

    /**
     * Makes the frames recorded since the last call readable.
     */
    @UpdateThread
    override fun processFrame() {
        if (isResetPending) {
            isResetPending = false

            totalGapNanos = 0L
            framesPerSecond = 0f
            timeUntilNextCalculation = FPS_CALCULATION_INTERVAL
            timeSinceLastCalculation = 0f
            framesSinceLastCalculation = 0
        }

        val count = pendingGapCount
        val sumSeconds = nanosToSeconds(pendingGapSumNanos)

        elapsedFrameTime = if (count > 0) nanosToSeconds(pendingLastGapNanos) else 0f
        longestFrameTime = if (count > 0) nanosToSeconds(pendingLongestGapNanos) else 0f

        totalGapNanos += pendingGapSumNanos
        currentTime = nanosToSeconds(totalGapNanos)

        clearPending()
        performBookkeeping(count, sumSeconds)
    }

    private fun performBookkeeping(frames: Int, time: Float) {
        framesSinceLastCalculation += frames
        timeUntilNextCalculation -= time
        timeSinceLastCalculation += time

        if (timeUntilNextCalculation <= 0f) {
            timeUntilNextCalculation += FPS_CALCULATION_INTERVAL

            framesPerSecond =
                if (framesSinceLastCalculation == 0 || timeSinceLastCalculation <= 0f) 0f
                else ceil(framesSinceLastCalculation / timeSinceLastCalculation)

            timeSinceLastCalculation = 0f
            framesSinceLastCalculation = 0
        }
    }

    private fun clearPending() {
        pendingGapCount = 0
        pendingGapSumNanos = 0L
        pendingLastGapNanos = 0L
        pendingLongestGapNanos = 0L
    }

    private fun nanosToSeconds(nanos: Long) = (nanos / 1e9).toFloat()

    override fun toString() = "${this::class.simpleName} (${currentTime}s, $framesPerSecond FPS)"

    companion object {
        /**
         * The longest gap between two presented frames, in nanoseconds, that is still counted.
         *
         * A longer gap restarts the measurement from the frame that ended it.
         */
        const val MAX_GAP_NANOS = 1_000_000_000L

        private const val FPS_CALCULATION_INTERVAL = 0.25f
    }
}
