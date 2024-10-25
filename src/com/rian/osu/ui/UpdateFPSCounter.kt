package com.rian.osu.ui

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
 */
class UpdateFPSCounter(displayText: ChangeableText) : FPSCounter(displayText) {
    override val tag = "Update"

    override fun onUpdate(deltaTime: Float) {
        // Cancel the effect of speed multiplier on frame time.
        updateFps(deltaTime)

        super.onUpdate(deltaTime)
    }
}