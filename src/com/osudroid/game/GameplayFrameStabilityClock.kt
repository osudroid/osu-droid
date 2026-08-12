package com.osudroid.game

import android.util.Log
import com.rian.andengine.timing.FramedClock
import com.rian.andengine.timing.IClock
import com.rian.andengine.timing.ManualClock
import com.rian.andengine.timing.Stopwatch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * A frame-stable clock that wraps [FramedBeatmapClock] and guarantees two properties:
 * - **Frame stability**: gameplay time advances by at most one 60 fps frame interval (~16.67ms) per update, preventing
 *   hit-object teleportation on frame drops.
 * - **BASS glitch suppression**: time jumps larger than 500ms that are not accompanied by an equivalent wall-clock
 *   advance are silently discarded. See [this](https://www.un4seen.com/forum/?topic=20482.msg145474#msg145474) forum
 *   post for more details.
 *
 * Use [seek] to seek the clock — [currentTime] reflects the new position immediately (before the next
 * [processFrame] call), and the next frame direct-seeks without the per-frame clamp.
 */
class GameplayFrameStabilityClock(
    /**
     * A [FramedBeatmapClock] which will be used as reference for time, rate, and running state.
     */
    private val referenceClock: FramedBeatmapClock
) : IGameplayClock {
    /**
     * A local [ManualClock] which tracks [referenceClock].
     *
     * Values are transferred from [referenceClock] every [processFrame] call.
     */
    private val manualClock = ManualClock()

    /**
     * The main [FramedClock] which has stability applied to it.
     */
    private val framedClock = FramedClock(manualClock, false)

    private var firstConsumption = true
    private var invalidBassTimeLogCount = 0

    private val wallClock = Stopwatch().also { it.start() }
    private var wallElapsedSeconds = 0f

    /**
     * Whether to enable frame-stable playback.
     */
    @get:JvmName("isFrameStablePlayback")
    var frameStablePlayback = true

    /**
     * The time (in seconds) at which frame stability activates. Before this point the clock runs freely,
     * allowing the lead-in to pass without spinning through hundreds of clamped frames.
     */
    var gameplayStartTime = Float.NEGATIVE_INFINITY

    /**
     * Whether the clock is still behind the reference clock after frame stability clamping, meaning the catch-up
     * loop should run an additional step this frame.
     */
    @get:JvmName("requiresCatchUp")
    var requiresCatchUp = false
        private set

    override val currentTime
        get() = if (firstConsumption) referenceClock.currentTime else framedClock.currentTime

    override val elapsedFrameTime by framedClock::elapsedFrameTime
    override val framesPerSecond by framedClock::framesPerSecond
    override val timeInfo by framedClock::timeInfo
    override var rate by referenceClock::rate
    override val isRunning by referenceClock::isRunning

    override var userGlobalOffset by referenceClock::userGlobalOffset
    override var userBeatmapOffset by referenceClock::userBeatmapOffset
    override val totalAppliedOffset by referenceClock::totalAppliedOffset
    override val source by referenceClock::source

    override fun seek(position: Float): Boolean {
        resetState()
        return referenceClock.seek(position)
    }

    override fun reset() {
        resetState()
        referenceClock.reset()
    }

    override fun start() = referenceClock.start()

    override fun stop() = referenceClock.stop()

    override fun resetSpeedAdjustments() = referenceClock.resetSpeedAdjustments()

    override fun changeSource(source: IClock?) = referenceClock.changeSource(source)

    override fun processFrame() {
        wallElapsedSeconds = wallClock.elapsedSeconds
        wallClock.restart()

        referenceClock.processFrame()
        updateClock()
    }

    private fun resetState() {
        firstConsumption = true
        invalidBassTimeLogCount = 0
        wallClock.restart()
        wallElapsedSeconds = 0f
    }

    private fun updateClock() {
        // Handle first consumption before any other checks so a seek done while paused takes effect
        // immediately (i.e. gameplayClock.currentTime reflects the new position on the very next frame).
        if (firstConsumption) {
            val referenceTime = referenceClock.currentTime
            manualClock.currentTime = referenceTime

            // The first processFrame call sets currentTime = referenceTime from a zero baseline, producing a
            // large initial elapsedFrameTime.
            framedClock.processFrame()

            firstConsumption = false
            manualClock.currentTime = referenceTime
            manualClock.rate = referenceClock.rate
            manualClock.isRunning = referenceClock.isRunning

            // The second call resets lastFrameTime so elapsedFrameTime = 0.
            framedClock.processFrame()

            return
        }

        if (!referenceClock.isRunning) {
            requiresCatchUp = false
            // Freeze time but still process the framed clock so elapsedFrameTime = 0.
            manualClock.currentTime = framedClock.currentTime
            manualClock.isRunning = false
            framedClock.processFrame()
            return
        }

        var proposedTime = referenceClock.currentTime

        if (frameStablePlayback) {
            proposedTime = applyFrameStability(proposedTime)
        }

        // This is a hotfix for a BASS issue (https://www.un4seen.com/forum/?topic=20482.msg145474#msg145474).
        // In this case, we consider a jump larger than 500 ms to be a glitch.
        // Double-checking wallElapsedSeconds ensures we do not freeze time during a legitimate long frame.
        val delta = abs(proposedTime - referenceClock.currentTime)

        if (frameStablePlayback && delta > 0.5f && wallElapsedSeconds <= 0.5f) {
            if (invalidBassTimeLogCount < 10) {
                invalidBassTimeLogCount++
                Log.w(TAG, "Ignoring likely invalid time value provided by BASS during gameplay")
                Log.w(TAG, "- provided: ${referenceClock.currentTime}")
                Log.w(TAG, "- expected: $proposedTime")
            }

            requiresCatchUp = false
            // Freeze time for this frame.
            manualClock.currentTime = framedClock.currentTime
            manualClock.rate = referenceClock.rate
            manualClock.isRunning = referenceClock.isRunning
            framedClock.processFrame()

            return
        }

        invalidBassTimeLogCount = 0
        requiresCatchUp = delta > 0f

        manualClock.currentTime = proposedTime
        manualClock.rate = referenceClock.rate
        manualClock.isRunning = referenceClock.isRunning

        framedClock.processFrame()
    }

    private fun applyFrameStability(proposedTime: Float): Float {
        // Scale by the current rate so a normal frame at any speed never triggers the clamp.
        val frameTime = abs(referenceClock.rate) / 60f

        if (manualClock.currentTime < gameplayStartTime) {
            // Before the first approach circle appears, advance freely up to gameplayStartTime.
            val clamped = min(gameplayStartTime, proposedTime)
            manualClock.currentTime = clamped

            return clamped
        }

        if (abs(manualClock.currentTime - proposedTime) > frameTime * 1.2f) {
            return if (proposedTime > manualClock.currentTime) {
                min(proposedTime, manualClock.currentTime + frameTime)
            } else {
                max(proposedTime, manualClock.currentTime - frameTime)
            }
        }

        return proposedTime
    }

    private companion object {
        private const val TAG = "GameplayFrameStabilityClock"
    }
}
