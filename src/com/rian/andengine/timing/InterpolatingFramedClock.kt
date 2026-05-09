package com.rian.andengine.timing

import com.osudroid.math.Interpolation
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/**
 * An [IClock] which uses an internal stopwatch to interpolate (smooth out) a source.
 */
class InterpolatingFramedClock @JvmOverloads constructor(source: IFrameBasedClock? = null) : IFrameBasedClock,
    ISourceChangeableClock {
    /**
     * The amount of error that is allowed between the source and interpolated time before the interpolated time is
     * ignored and the source time is used. Defaults to two 60 FPS frames (~33.3 ms).
     *
     * This is internally adjusted for the current playback rate (so that the actual precision is constant regardless
     * of the rate applied).
     */
    var allowableErrorSeconds = 2f / 60

    /**
     * Drift recovery half-life in seconds. Defaults to 0.05 seconds.
     *
     * The time error decays exponentially toward the source. Every [driftRecoveryHalfLife] seconds, the remaining error
     * halves.
     *
     * An example, starting at 0.01 s error with a 0.05 s half-life:
     * - at 0 s, error is 0.01 s.
     * - at 0.05 s, error is 0.005 s.
     * - at 0.1 s, error is 0.0025 s.
     * - at 0.15 s, error is 0.00125 s.
     * ...
     *
     * To an observer, it will look like time has a temporary ramp applied to it:
     * - If source is ahead, time will speed up and gradually approach original speed.
     * - If source is behind, time will slow down and gradually approach original speed.
     *
     * Only applies when the error is within [allowableErrorSeconds].
     */
    var driftRecoveryHalfLife = 0.05f

    /**
     * Whether interpolation was applied at the last processed frame.
     *
     * If [drift] becomes too high (as defined by [allowableErrorSeconds]), interpolation will be bypassed in order
     * to provide a more correct time value.
     */
    var isInterpolating = false
        private set

    /**
     * The drift in seconds between the source and interpolation at the last processed frame.
     */
    val drift
        get() = currentTime - framedSourceClock.currentTime

    override val rate
        get() = framedSourceClock.rate

    override var isRunning = false
        private set

    override var elapsedFrameTime = 0f
        private set

    override lateinit var source: IClock
        private set

    private lateinit var framedSourceClock: IFrameBasedClock

    override var currentTime = 0f
        private set

    private val realTimeClock = FramedClock(StopwatchClock(true))

    private var _currentTime = 0f

    private val _timeInfo = FrameTimeInfo()

    override val timeInfo
        get() = _timeInfo.apply {
            current = currentTime
            elapsed = elapsedFrameTime
        }

    init {
        changeSource(source)
    }

    override fun changeSource(source: IClock?) {
        if (source != null && ::source.isInitialized && this.source == source) {
            return
        }

        this.source = source ?: StopwatchClock(true)

        // We need a frame-based source to correctly process interpolation.
        // If the provided source is not already a framed clock, encapsulate it in one.
        framedSourceClock = this.source as? IFrameBasedClock ?: FramedClock(this.source)

        isInterpolating = false
        _currentTime = framedSourceClock.currentTime
    }

    override fun processFrame() {
        val lastTime = _currentTime

        realTimeClock.processFrame()
        framedSourceClock.processFrame()

        val sourceIsRunning = framedSourceClock.isRunning
        val sourceHasElapsed = framedSourceClock.elapsedFrameTime != 0f

        try {
            if (!sourceIsRunning) {
                // While the source isn't running, we remain in the current interpolation mode unless there's a seek.
                // This is to ensure the most consistent playback possible, and avoid fractional differences when
                // stopping/starting the source.
                if (sourceHasElapsed) {
                    isInterpolating = false
                    _currentTime = framedSourceClock.currentTime
                }

                return
            }

            if (isInterpolating) {
                // Apply time increase from interpolation.
                _currentTime += realTimeClock.elapsedFrameTime * rate

                // Then check the post-interpolated time.
                // If we differ from the current time of the source, gradually approach the ground truth.
                //
                // The remaining error halves every half-life ms.
                _currentTime = Interpolation.dampContinuously(
                    _currentTime,
                    framedSourceClock.currentTime,
                    driftRecoveryHalfLife,
                    realTimeClock.elapsedFrameTime
                )

                val withinAllowableError = abs(framedSourceClock.currentTime - _currentTime) <= allowableErrorSeconds * abs(rate)

                if (!withinAllowableError) {
                    // if we've exceeded the allowable error, we should use the source clock's time value.
                    isInterpolating = false
                    _currentTime = framedSourceClock.currentTime
                }
            } else {
                _currentTime = framedSourceClock.currentTime

                // Of importance, only start interpolating from the next frame.
                // The first frame after a clock starts may give very incorrect results, ie. due to a seek in the frame
                // before.
                if (sourceHasElapsed) {
                    isInterpolating = true
                }
            }

            // Seeking backwards should only be allowed if the source is explicitly doing that.
            val elapsedInOpposingDirection = framedSourceClock.elapsedFrameTime != 0f && sign(framedSourceClock.elapsedFrameTime) != sign(rate)

            if (!elapsedInOpposingDirection) {
                _currentTime = if (rate >= 0) max(lastTime, _currentTime) else min(lastTime, _currentTime)
            }
        } finally {
            isRunning = sourceIsRunning
            currentTime = _currentTime
            elapsedFrameTime = currentTime - lastTime
        }
    }

    override val framesPerSecond = 0f
}
