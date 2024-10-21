package com.rian.osu.ui

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
}