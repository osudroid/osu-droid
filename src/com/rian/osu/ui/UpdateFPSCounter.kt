package com.rian.osu.ui

import com.rian.osu.math.Interpolation.dampContinuously
import org.anddev.andengine.entity.text.ChangeableText

/**
 * A class that counts an update thread's frames per second.
 *
 * Unlike [org.anddev.andengine.entity.util.FPSCounter], this class uses a dampened average to smooth out the calculated
 * frame rate, while accounting for spike frames.
 *
 * All time units are in seconds.
 *
 * @param displayText The [ChangeableText] that will be used to display the current frame rate.
 * @param speedMultiplier The speed multiplier of the game. Used to cancel the effect of speed multiplier on frame time.
 */
class UpdateFPSCounter(displayText: ChangeableText, private val speedMultiplier: Float) : FPSCounter(displayText) {
    override val tag = "Update"
    private var frameTime = 0f

    override fun onUpdate(pSecondsElapsed: Float) {
        // Cancel the effect of speed multiplier on frame time.
        updateFps(pSecondsElapsed / speedMultiplier)
        updateDisplayText()
    }

    override fun calculateUpdatedFps(deltaTime: Float): Float {
        val hasSpike = frameTime < spikeTime && deltaTime > spikeTime

        frameTime = dampContinuously(frameTime, deltaTime, if (hasSpike) 0f else dampTime, deltaTime)

        return 1 / frameTime
    }

    override fun reset() {
        super.reset()

        frameTime = 0f
    }
}