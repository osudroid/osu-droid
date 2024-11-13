package com.rian.osu.ui

import android.content.Context
import android.view.WindowManager
import com.reco1l.framework.ColorARGB
import com.rian.osu.math.Interpolation.dampContinuously
import kotlin.math.min
import kotlin.math.roundToInt
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.opengl.font.Font
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal

/**
 * Represents an FPS counter.
 *
 * All time units are in seconds.
 *
 * @param font The [Font] that will be used to display the current frame rate.
 */
class FPSCounter(font: Font) : ChangeableText(0f, 0f, font, "999/999 FPS") {
    //region Counting logic

    /**
     * The current frame rate.
     */
    var fps = 0f
        private set

    /**
     * The maximum frame rate that can be displayed.
     */
    var maximumFps = 0f
        private set(value) {
            if (field == value) {
                return
            }

            field = value

            // Force update so that the new maximum frame rate is displayed.
            forceUpdate = true
        }

    /**
     * The average frame rate.
     */
    var averageFps = 0f
        private set

    @Suppress("DEPRECATION")
    private val deviceDisplay = (getGlobal().mainActivity.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay

    private val spikeTime = 0.02f
    private val dampTime = 0.1f

    // Average FPS calculation
    private var timeUntilNextAverageFpsCalculation = 0f
    private var timeSinceLastAverageFpsCalculation = 0f
    private var framesSinceLastAverageFpsCalculation = 0
    private val averageFpsCalculationInterval = 0.25f

    /**
     * Updates the FPS of this [FPSCounter].
     *
     * @param deltaTime The time in seconds since the last update.
     */
    fun updateFps(deltaTime: Float) {
        // Update average FPS
        if (timeUntilNextAverageFpsCalculation <= 0) {
            timeUntilNextAverageFpsCalculation += averageFpsCalculationInterval

            averageFps =
                if (framesSinceLastAverageFpsCalculation == 0) 0f
                else framesSinceLastAverageFpsCalculation / timeSinceLastAverageFpsCalculation

            timeSinceLastAverageFpsCalculation = 0f
            framesSinceLastAverageFpsCalculation = 0

            // This should not belong here, but let's do it anyway to prevent excessive locks.
            maximumFps = deviceDisplay.refreshRate
        }

        framesSinceLastAverageFpsCalculation++
        timeUntilNextAverageFpsCalculation -= deltaTime
        timeSinceLastAverageFpsCalculation += deltaTime

        // Use elapsed frame time rather than FPS to better catch stutter frames.
        val hasSpike = fps > 1 / spikeTime && deltaTime > spikeTime

        // Show spike time using raw elapsed value to account for average FPS being so averaged spike frames don't show.
        fps = min(maximumFps, if (hasSpike) 1 / deltaTime else dampContinuously(fps, averageFps, dampTime, deltaTime))
    }

    //endregion

    //region Display logic

    private var lastDisplayedFps = 0

    private var forceUpdate = false

    private var timeSinceLastUpdate = 0f
    private val updateInterval = 0.01f

    private val minimumTextColor = ColorARGB(0xed1121)
    private val middleTextColor = ColorARGB(0xebc247)
    private val maximumTextColor = ColorARGB(0xccff99)

    override fun onManagedUpdate(pSecondsElapsed: Float) {
        super.onManagedUpdate(pSecondsElapsed)

        // If the game goes into a suspended state (i.e., debugger attached), we want to ignore really long periods of
        // no processing.
        if (pSecondsElapsed > 10) {
            return
        }

        timeSinceLastUpdate += pSecondsElapsed

        if (!forceUpdate && timeSinceLastUpdate < updateInterval) {
            return
        }

        forceUpdate = false
        timeSinceLastUpdate = 0f

        val displayedFps = fps.roundToInt()

        if (!forceUpdate && displayedFps == lastDisplayedFps) {
            return
        }

        lastDisplayedFps = displayedFps

        text = "$displayedFps/${maximumFps.roundToInt()} FPS"

        updateColor()
    }

    private fun updateColor() {
        val performanceRatio = fps / maximumFps
        val red: Float
        val green: Float
        val blue: Float

        if (performanceRatio < 0.5f) {
            val t = performanceRatio / 0.5f

            red = minimumTextColor.red + t * (middleTextColor.red - minimumTextColor.red)
            green = minimumTextColor.green + t * (middleTextColor.green - minimumTextColor.green)
            blue = minimumTextColor.blue + t * (middleTextColor.blue - minimumTextColor.blue)
        } else {
            val t = ((performanceRatio - 0.5f) / 0.4f).coerceIn(0f, 1f)

            red = middleTextColor.red + t * (maximumTextColor.red - middleTextColor.red)
            green = middleTextColor.green + t * (maximumTextColor.green - middleTextColor.green)
            blue = middleTextColor.blue + t * (maximumTextColor.blue - middleTextColor.blue)
        }

        setColor(red, green, blue)
    }

    //endregion

    //region Reset

    override fun reset() {
        super.reset()

        timeUntilNextAverageFpsCalculation = 0f
        timeSinceLastAverageFpsCalculation = 0f
        framesSinceLastAverageFpsCalculation = 0

        fps = 0f
        maximumFps = 0f
        lastDisplayedFps = 0
        forceUpdate = false
        timeSinceLastUpdate = 0f
    }

    //endregion
}