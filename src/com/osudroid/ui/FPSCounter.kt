package com.osudroid.ui

import com.reco1l.framework.Color4
import com.osudroid.math.Interpolation
import com.reco1l.andengine.UIEngine
import java.util.Formatter
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.opengl.font.Font
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal

/**
 * Represents an FPS counter.
 *
 * All time units are in seconds.
 *
 * @param font The [Font] that will be used to display the current frame rate.
 */
class FPSCounter(font: Font) : ChangeableText(
    0f,
    0f,
    font,
    "",
    24
) {
    private val drawClock = UIEngine.current.drawClock
    private val updateClock = UIEngine.current.updateClock

    //region Counting logic

    /**
     * The current frame rate.
     */
    var displayedFpsCount = 0f
        private set

    /**
     * The average frame time in seconds.
     */
    var displayedFrameTime = 0f
        private set

    private var aimDrawFPS = 0f
    private var aimUpdateFPS = 0f

    private val spikeTime = 0.02f
    private val dampTime = 0.1f

    private val stringBuilder = StringBuilder(24)
    private val formatter = Formatter(stringBuilder, Locale.US)

    //endregion

    //region Display logic

    private var lastDisplayedFps = 0
    private var lastDisplayedFrameTime = 0f

    private var timeSinceLastUpdate = 0f
    private val updateInterval = 0.1f

    private val minimumTextColor = Color4(0xed1121)
    private val middleTextColor = Color4(0xebc247)
    private val maximumTextColor = Color4(0xccff99)

    override fun onManagedUpdate(pSecondsElapsed: Float) {
        super.onManagedUpdate(pSecondsElapsed)

        val elapsedDrawFrameTime = drawClock.elapsedFrameTime
        val elapsedUpdateFrameTime = updateClock.elapsedFrameTime

        // If the game goes into a suspended state (i.e., debugger attached), we want to ignore really long periods of
        // no processing.
        if (elapsedUpdateFrameTime > 10) {
            return
        }

        // Handle the case where the window has become inactive (we want to show the FPS as it is changing, even if it
        // is not an outlier).
        val aimRatesChanged = updateAimFPS()

        val hasUpdateSpike = displayedFrameTime < spikeTime && elapsedUpdateFrameTime > spikeTime
        // Use elapsed frame time rather than framesPerSecond to better catch stutter frames.
        val hasDrawSpike = displayedFpsCount > 1f / spikeTime && elapsedDrawFrameTime > spikeTime

        displayedFrameTime = Interpolation.dampContinuously(
            displayedFrameTime,
            elapsedUpdateFrameTime,
            if (hasUpdateSpike) 0f else dampTime,
            elapsedUpdateFrameTime
        )

        displayedFpsCount = if (hasDrawSpike) {
            // Show spike time using raw elapsed value, to account for `framesPerSecond` being so averaged spike frames
            // do not show.
            1f / elapsedDrawFrameTime
        } else {
            Interpolation.dampContinuously(
                displayedFpsCount,
                drawClock.framesPerSecond,
                dampTime,
                updateClock.timeInfo.elapsed
            )
        }

        val hasSignificantChanges = aimRatesChanged ||
                hasDrawSpike ||
                hasUpdateSpike ||
                displayedFpsCount < aimDrawFPS * 0.8f ||
                1 / displayedFrameTime < aimUpdateFPS * 0.8f

        timeSinceLastUpdate += updateClock.timeInfo.elapsed

        if (!hasSignificantChanges && timeSinceLastUpdate < updateInterval) {
            return
        }

        timeSinceLastUpdate = 0f

        val displayedFps = displayedFpsCount.roundToInt()
        val isHighPrecision = displayedFrameTime < 0.01f
        val displayedMs = displayedFrameTime * 1000

        val roundedFrameTime = if (isHighPrecision) {
            (displayedMs * 10).roundToInt() / 10f
        } else {
            displayedMs.roundToInt().toFloat()
        }

        if (!hasSignificantChanges && displayedFps == lastDisplayedFps && roundedFrameTime == lastDisplayedFrameTime) {
            return
        }

        lastDisplayedFps = displayedFps
        lastDisplayedFrameTime = roundedFrameTime

        stringBuilder.setLength(0)

        formatter.format(
            "%.${if (isHighPrecision) "1" else "0"}f ms | %d FPS",
            displayedMs,
            displayedFps
        )

        text = stringBuilder.toString()

        updateColor()
        setPosition(Config.getRES_WIDTH() - widthScaled - 5, Config.getRES_HEIGHT() - heightScaled - 10)
    }

    private fun updateAimFPS(): Boolean {
        val newAimDrawFPS = getGlobal().mainActivity.refreshRate

        val newAimUpdateFPS = if (updateClock.throttling && updateClock.maximumUpdateHz > 0) {
            min(updateClock.maximumUpdateHz, newAimDrawFPS)
        } else {
            newAimDrawFPS
        }

        if (aimDrawFPS != newAimDrawFPS || aimUpdateFPS != newAimUpdateFPS) {
            aimDrawFPS = newAimDrawFPS
            aimUpdateFPS = newAimUpdateFPS
            return true
        }

        return false
    }

    private fun updateColor() {
        val performanceRatio = displayedFpsCount / aimDrawFPS
        val red: Float
        val green: Float
        val blue: Float

        if (performanceRatio < 0.5f) {
            val t = (performanceRatio / 0.5f).coerceIn(0f, 1f)

            red = Interpolation.linear(minimumTextColor.red, middleTextColor.red, t)
            green = Interpolation.linear(minimumTextColor.green, middleTextColor.green, t)
            blue = Interpolation.linear(minimumTextColor.blue, middleTextColor.blue, t)
        } else {
            val t = ((performanceRatio - 0.5f) / 0.4f).coerceIn(0f, 1f)

            red = Interpolation.linear(middleTextColor.red, maximumTextColor.red, t)
            green = Interpolation.linear(middleTextColor.green, maximumTextColor.green, t)
            blue = Interpolation.linear(middleTextColor.blue, maximumTextColor.blue, t)
        }

        setColor(red, green, blue)
    }

    //endregion

    //region Reset

    override fun reset() {
        super.reset()

        displayedFpsCount = 0f
        displayedFrameTime = 0f
        lastDisplayedFps = 0
        lastDisplayedFrameTime = 0f
        timeSinceLastUpdate = 0f
    }

    //endregion
}