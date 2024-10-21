package com.rian.osu.ui

import com.rian.osu.math.Interpolation.dampContinuously
import org.anddev.andengine.entity.text.ChangeableText

/**
 * A class that counts a draw thread's frames per second.
 *
 * All time units are in seconds.
 *
 * @param displayText The [ChangeableText] that will be used to display the current frame rate.
 */
class DrawFPSCounter(displayText: ChangeableText) : FPSCounter(displayText) {
    override val tag = "Draw"
    private var lastDeltaTime = 0f

    override fun calculateUpdatedFps(deltaTime: Float): Float {
        val hasSpike = currentFps > 1 / spikeTime && deltaTime > spikeTime

        // Show spike time using raw delta time to account for average frame rate not showing spike frames.
        val newFps = if (hasSpike) 1 / deltaTime else dampContinuously(currentFps, averageFps, dampTime, lastDeltaTime)

        lastDeltaTime = deltaTime

        return newFps
    }

    override fun reset() {
        super.reset()

        lastDeltaTime = 0f
    }
}