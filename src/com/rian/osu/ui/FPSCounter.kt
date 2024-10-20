package com.rian.osu.ui

import com.reco1l.framework.ColorARGB
import com.rian.osu.math.Interpolation.dampContinuously
import kotlin.math.max
import kotlin.math.roundToInt
import org.anddev.andengine.engine.handler.IUpdateHandler
import org.anddev.andengine.entity.text.ChangeableText

/**
 * A class that counts frames per second.
 *
 * Unlike [org.anddev.andengine.entity.util.FPSCounter], this class uses a dampened average to smooth out the calculated
 * frame rate, while accounting for spike frames.
 *
 * All time units are in seconds.
 *
 * @param displayText A [ChangeableText] that will be used to display the current frame rate.
 */
open class FPSCounter(private val displayText: ChangeableText) : IUpdateHandler {
    private var fps = 0f
    private var frameTime = 0f
    private var maximumFps = 0f

    private var elapsedTime = 0f
    private var frameCount = 0
    private val averageFps
        get() = if (elapsedTime > 0) frameCount / elapsedTime else 0f

    private var lastDisplayUpdateTime = 0f
    private var lastDisplayedFps = 0

    private val spikeTime = 0.02f
    private val dampTime = 0.1f
    private val minTimeBetweenUpdates = 0.01f

    private val minimumTextColor = ColorARGB(15536417)
    private val middleTextColor = ColorARGB(15450695)
    private val maximumTextColor = ColorARGB(13434777)

    override fun onUpdate(deltaTime: Float) {
        elapsedTime += deltaTime
        ++frameCount

        val hasSpike = frameTime < spikeTime && deltaTime > spikeTime

        frameTime = dampContinuously(frameTime, deltaTime, if (hasSpike) 0f else dampTime, deltaTime)

        // Show spike time using raw delta time to account for average frame rate not showing spike frames.
        fps = if (hasSpike) 1 / deltaTime else dampContinuously(fps, averageFps, dampTime, deltaTime)
        maximumFps = max(maximumFps, fps)

        if (elapsedTime - lastDisplayUpdateTime > minTimeBetweenUpdates) {
            updateDisplayText()
            lastDisplayUpdateTime = elapsedTime
        }
    }

    private fun updateDisplayText() {
        val displayedFps = fps.roundToInt()

        if (displayedFps == lastDisplayedFps) {
            return
        }

        lastDisplayedFps = displayedFps
        displayText.text = "$displayedFps FPS"

        val performanceRatio = if (maximumFps > 0) fps / maximumFps else 0f

        val red: Float
        val green: Float
        val blue: Float

        if (performanceRatio < 0.5f) {
            val t = performanceRatio / 0.5f

            red = minimumTextColor.red + t * (middleTextColor.red - minimumTextColor.red)
            green = minimumTextColor.green + t * (middleTextColor.green - minimumTextColor.green)
            blue = minimumTextColor.blue + t * (middleTextColor.blue - minimumTextColor.blue)
        } else {
            val t = (performanceRatio - 0.5f) / 0.4f

            red = middleTextColor.red + t * (maximumTextColor.red - middleTextColor.red)
            green = middleTextColor.green + t * (maximumTextColor.green - middleTextColor.green)
            blue = middleTextColor.blue + t * (maximumTextColor.blue - middleTextColor.blue)
        }

        displayText.setColor(red, green, blue)
    }

    override fun reset() {
        elapsedTime = 0f
        frameCount = 0
        frameTime = 0f
        fps = 0f
        maximumFps = 0f
        lastDisplayUpdateTime = 0f
        lastDisplayedFps = 0
    }
}