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
 * A frame-stable clock that wraps an [IGameplayClock] (in production a [FramedBeatmapClock]) and guarantees three
 * properties:
 * - **Frame stability**: gameplay time advances by at most one 60 fps frame interval (~16.67ms) per update, preventing
 *   hit-object teleportation on frame drops.
 * - **BASS glitch suppression**: a BASS reading more than 500ms away from the time expected from real elapsed time
 *   (previous time + wall-clock elapsed time * rate) is discarded for that frame, unless BASS itself advanced
 *   consistently with real time since its last trusted reading (then this clock is the one lagging behind). See
 *   [this](https://www.un4seen.com/forum/?topic=20482.msg145474#msg145474) forum post for more details.
 * - **Bounded freezing**: while the reference clock is running and BASS is ahead, time is held for at most 5
 *   consecutive [processFrame] calls (`MAX_IGNORED_FRAMES`). Whenever the clock is more than 500ms behind a BASS time
 *   that is not (or no longer) considered a glitch, e.g. after a long stall, it re-syncs forward to BASS directly
 *   (seek-like) instead of catching up frame by frame. During the lead-in such a re-sync stops at
 *   [gameplayStartTime]; the next frame re-syncs the rest. Under a persistent glitch (BASS readings that stay
 *   implausible) the clock alternates between at most 5 held frames and a re-sync, so the deviation stays bounded
 *   instead of growing.
 * - **No backward jumps**: GameScene has no seek bookkeeping for a silent backward jump, so the clock never re-syncs
 *   backward. When BASS is more than 500ms behind, the clock holds (as above) and then walks back by at most one frame
 *   per [processFrame] call until it meets BASS again. That case is bounded by the time BASS needs to catch up, not
 *   by a fixed number of frames.
 *
 * Use [seek] to seek the clock — [currentTime] reflects the new position immediately (before the next
 * [processFrame] call), and the next frame direct-seeks without the per-frame clamp.
 */
class GameplayFrameStabilityClock @JvmOverloads constructor(
    /**
     * An [IGameplayClock] (in production a [FramedBeatmapClock]) which will be used as reference for time, rate, and
     * running state.
     */
    private val referenceClock: IGameplayClock,

    /**
     * The real-time source (in nanoseconds) used to measure wall-clock time between frames. Defaults to
     * [System.nanoTime].
     */
    wallTimeSource: () -> Long = System::nanoTime
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
    private var resyncLogCount = 0

    /**
     * The last [referenceClock] time that was trusted (projected forward by real elapsed time while readings are
     * being ignored).
     */
    private var lastReferenceTime = 0f

    /**
     * The number of consecutive frames in which the [referenceClock] time was ignored as a likely BASS glitch.
     */
    private var ignoredFrameCount = 0

    private val wallClock = Stopwatch(wallTimeSource).also { it.start() }
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
        resyncLogCount = 0
        ignoredFrameCount = 0
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
            lastReferenceTime = referenceTime
            manualClock.currentTime = referenceTime
            manualClock.rate = referenceClock.rate
            manualClock.isRunning = referenceClock.isRunning

            // The second call resets lastFrameTime so elapsedFrameTime = 0.
            framedClock.processFrame()

            return
        }

        if (!referenceClock.isRunning) {
            requiresCatchUp = false
            ignoredFrameCount = 0
            lastReferenceTime = referenceClock.currentTime
            // Freeze time but still process the framed clock so elapsedFrameTime = 0.
            manualClock.currentTime = framedClock.currentTime
            manualClock.isRunning = false
            framedClock.processFrame()
            return
        }

        val referenceTime = referenceClock.currentTime

        if (!frameStablePlayback) {
            ignoredFrameCount = 0
            invalidBassTimeLogCount = 0
            lastReferenceTime = referenceTime
            applyTime(referenceTime, false)
            return
        }

        val previousTime = framedClock.currentTime
        val wallAdvance = wallElapsedSeconds * referenceClock.rate

        // This is a hotfix for a BASS issue (https://www.un4seen.com/forum/?topic=20482.msg145474#msg145474), where
        // BASS reports a single bogus position followed by normal values. A reading further than MAX_PLAUSIBLE_JUMP
        // from the time expected from real elapsed time is a glitch candidate. Double-checking wallElapsedSeconds
        // ensures we do not freeze time during a legitimate long frame.
        val expectedTime = previousTime + wallAdvance

        if (wallElapsedSeconds <= MAX_PLAUSIBLE_JUMP && abs(referenceTime - expectedTime) > MAX_PLAUSIBLE_JUMP) {
            // If BASS advanced consistently with real time since its last trusted reading, the reading is not a
            // glitch: this clock is the one lagging behind (e.g. after a stall). Otherwise, ignore the reading, but
            // only for a bounded number of frames so that a persistent offset can never freeze gameplay.
            val referenceIsConsistent = abs(referenceTime - (lastReferenceTime + wallAdvance)) <= MAX_PLAUSIBLE_JUMP

            if (!referenceIsConsistent && ignoredFrameCount < MAX_IGNORED_FRAMES) {
                ignoredFrameCount++

                if (invalidBassTimeLogCount < 10) {
                    invalidBassTimeLogCount++
                    Log.w(TAG, "Ignoring likely invalid time value provided by BASS during gameplay")
                    Log.w(TAG, "- provided: $referenceTime")
                    Log.w(TAG, "- expected: $expectedTime")
                }

                // Project the last trusted reading forward, so the next frame is compared against real elapsed time.
                lastReferenceTime += wallAdvance

                // Freeze time for this frame.
                applyTime(previousTime, false)
                return
            }

            if (referenceTime > previousTime) {
                resync(referenceTime, previousTime)
            } else {
                followBackward(referenceTime)
            }
            return
        }

        ignoredFrameCount = 0
        invalidBassTimeLogCount = 0
        lastReferenceTime = referenceTime

        if (referenceTime - previousTime > MAX_PLAUSIBLE_JUMP) {
            // A legitimate long frame (e.g. a stall) left this clock too far behind to catch up frame by frame.
            resync(referenceTime, previousTime)
            return
        }

        if (previousTime - referenceTime > MAX_PLAUSIBLE_JUMP) {
            followBackward(referenceTime)
            return
        }

        val proposedTime = applyFrameStability(referenceTime)

        applyTime(proposedTime, abs(proposedTime - referenceTime) > 0f)
    }

    /**
     * Jumps forward directly to [referenceTime] (seek-like, without the per-frame clamp), which must be ahead of
     * [previousTime]. During the lead-in the jump stops at [gameplayStartTime], so gameplay never starts in the middle
     * of a jump; the next frame re-syncs the rest. The jump shows up in this frame's [elapsedFrameTime];
     * [requiresCatchUp] is `false` afterwards.
     */
    private fun resync(referenceTime: Float, previousTime: Float) {
        ignoredFrameCount = 0
        lastReferenceTime = referenceTime

        val target = if (previousTime < gameplayStartTime) min(gameplayStartTime, referenceTime) else referenceTime

        if (resyncLogCount < 10) {
            resyncLogCount++
            Log.w(TAG, "Gameplay clock too far away from BASS, re-syncing")
            Log.w(TAG, "- clock: $previousTime")
            Log.w(TAG, "- BASS: $referenceTime")
        }

        applyTime(target, false)
    }

    /**
     * BASS is far behind this clock and trusted: walk towards it by at most one frame instead of jumping back (also
     * during the lead-in), without extra catch-up steps in the same frame.
     */
    private fun followBackward(referenceTime: Float) {
        ignoredFrameCount = 0
        lastReferenceTime = referenceTime
        val frameTime = abs(referenceClock.rate) / 60f
        applyTime(max(referenceTime, framedClock.currentTime - frameTime), false)
    }

    private fun applyTime(time: Float, catchUp: Boolean) {
        requiresCatchUp = catchUp

        manualClock.currentTime = time
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

        /**
         * The largest time difference (in seconds) accepted without being treated as a glitch or requiring a re-sync.
         */
        private const val MAX_PLAUSIBLE_JUMP = 0.5f

        /**
         * The maximum number of consecutive frames for which a likely invalid BASS reading is ignored.
         */
        private const val MAX_IGNORED_FRAMES = 5
    }
}
