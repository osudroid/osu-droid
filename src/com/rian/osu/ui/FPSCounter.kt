package com.rian.osu.ui

import android.content.Context
import android.view.WindowManager
import com.reco1l.framework.ColorARGB
import kotlin.math.roundToInt
import org.anddev.andengine.engine.handler.IUpdateHandler
import org.anddev.andengine.entity.text.ChangeableText
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal

/**
 * Base class for FPS counters.
 *
 * All time units are in seconds.
 *
 * @param displayText The [ChangeableText] that will be used to display the current frame rate.
 */
abstract class FPSCounter(@JvmField val displayText: ChangeableText) : IUpdateHandler {
    /**
     * The current frames per second.
     */
    protected var currentFps = 0f
        private set

    /**
     * The average frames per second.
     */
    protected val averageFps
        get() = if (elapsedTime > 0) frameCount / elapsedTime else 0f

    /**
     * The default display of the device.
     */
    @Suppress("DEPRECATION")
    private val display = (getGlobal().mainActivity.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay

    /**
     * The tag used to identify the counter.
     */
    protected abstract val tag: String

    protected val spikeTime = 0.02f
    protected val dampTime = 0.1f

    private var elapsedTime = 0f
    private var frameCount = 0
    private var lastDisplayedFps = 0
    private var lastDisplayUpdateTime = 0f
    private val minTimeBetweenUpdates = 0.01f

    private val minimumTextColor = ColorARGB(0xed1121)
    private val middleTextColor = ColorARGB(0xebc247)
    private val maximumTextColor = ColorARGB(0xccff99)

    /**
     * Updates the FPS of this [FPSCounter].
     *
     * Unlike [onUpdate], this can be called in any thread, as it does not update [displayText].
     *
     * @param deltaTime The time in seconds since the last update.
     */
    fun updateFps(deltaTime: Float) {
        // If the game goes into a suspended state (i.e., debugger attached), we want to ignore really long periods of
        // no processing.
        if (deltaTime > 10) {
            return
        }

        elapsedTime += deltaTime
        ++frameCount

        currentFps = calculateUpdatedFps(deltaTime)
    }

    override fun onUpdate(pSecondsElapsed: Float) {
        updateDisplayText()
    }

    protected fun updateDisplayText() {
        if (elapsedTime - lastDisplayUpdateTime <= minTimeBetweenUpdates) {
            return
        }

        lastDisplayUpdateTime = elapsedTime
        val displayedFps = currentFps.roundToInt()

        if (displayedFps == lastDisplayedFps) {
            return
        }

        lastDisplayedFps = displayedFps
        displayText.text = "$tag: $displayedFps FPS"

        val performanceRatio = currentFps / display.refreshRate

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

        displayText.setColor(red, green, blue)
    }

    override fun reset() {
        currentFps = 0f
        elapsedTime = 0f
        frameCount = 0
        lastDisplayedFps = 0
        lastDisplayUpdateTime = 0f
    }

    /**
     * Obtains the updated frames per second after [deltaTime] has passed.
     *
     * @param deltaTime The time in seconds since the last update.
     */
    protected abstract fun calculateUpdatedFps(deltaTime: Float): Float
}