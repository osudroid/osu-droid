package com.osudroid.game

import com.rian.andengine.timing.DecouplingFramedClock
import com.rian.andengine.timing.FramedOffsetClock
import com.rian.andengine.timing.IClock
import com.rian.andengine.timing.IFrameBasedClock
import com.rian.andengine.timing.InterpolatingFramedClock
import com.rian.andengine.timing.OffsetCorrectionClock

/**
 * A clock intended to be the single source-of-truth for beatmap timing inside gameplay.
 *
 * It provides some functionality:
 *  - Optionally applies beatmap and user offsets.
 *  - Adjusts seek operations to account for any applied offsets (seeking in raw beatmap time values).
 */
class FramedBeatmapClock @JvmOverloads constructor(
    applyOffsets: Boolean,
    requireDecoupling: Boolean = false,
    source: IClock? = null
) : IGameplayClock {
    private val decoupledTrack = DecouplingFramedClock(source).apply { allowDecoupling = requireDecoupling }
    private val interpolatedTrack = InterpolatingFramedClock(decoupledTrack).apply { driftRecoveryHalfLife = 0.08f }

    private val userGlobalOffsetClock: OffsetCorrectionClock?
    private val userBeatmapOffsetClock: FramedOffsetClock?

    private val finalClockSource: IFrameBasedClock

    init {
        if (applyOffsets) {
            val global = OffsetCorrectionClock(interpolatedTrack)
            val beatmap = FramedOffsetClock(global)

            userGlobalOffsetClock = global
            userBeatmapOffsetClock = beatmap
            finalClockSource = beatmap
        } else {
            userGlobalOffsetClock = null
            userBeatmapOffsetClock = null
            finalClockSource = interpolatedTrack
        }
    }

    override var userGlobalOffset
        get() = userGlobalOffsetClock?.offset ?: 0f
        set(value) {
            userGlobalOffsetClock?.offset = value
        }

    override var userBeatmapOffset
        get() = userBeatmapOffsetClock?.offset ?: 0f
        set(value) {
            userBeatmapOffsetClock?.offset = value
        }

    override val totalAppliedOffset
        get() = (userGlobalOffsetClock?.rateAdjustedOffset ?: 0f) + (userBeatmapOffsetClock?.offset ?: 0f)

    // IFrameBasedClock delegations to the final clock with offsets applied
    override val timeInfo by finalClockSource::timeInfo
    override val currentTime by finalClockSource::currentTime
    override val elapsedFrameTime by finalClockSource::elapsedFrameTime
    override val framesPerSecond by finalClockSource::framesPerSecond
    override val isRunning by finalClockSource::isRunning

    override fun processFrame() {
        finalClockSource.processFrame()
    }

    // IAdjustable / ISourceChangeable delegation to decoupledTrack
    override fun reset() {
        decoupledTrack.reset()
        finalClockSource.processFrame()
    }

    override fun start() {
        decoupledTrack.start()
        finalClockSource.processFrame()
    }

    override fun stop() {
        decoupledTrack.stop()
        finalClockSource.processFrame()
    }

    override fun seek(position: Float): Boolean {
        val success = decoupledTrack.seek(position - totalAppliedOffset)
        finalClockSource.processFrame()

        return success
    }

    override fun resetSpeedAdjustments() = decoupledTrack.resetSpeedAdjustments()

    override var rate by decoupledTrack::rate
    override val source by decoupledTrack::source

    override fun changeSource(source: IClock?) = decoupledTrack.changeSource(source)
}