package com.osudroid.ui

import com.reco1l.framework.Color4
import com.osudroid.math.Interpolation
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.UIEngine
import com.reco1l.andengine.shape.UIBox
import com.reco1l.andengine.text.UIText
import com.reco1l.framework.math.Vec4
import java.util.Formatter
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal

/**
 * Represents an FPS counter.
 */
class FPSCounter : UIText() {
    private val updateClock
        get() = UIEngine.current.clock

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

    private var aimFPS = 0f

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

    init {
        font = ResourceManager.getInstance().getFont("smallFont")
        padding = Vec4(4f)
        anchor = Anchor.BottomRight
        origin = Anchor.BottomRight
        alignment = Anchor.Center

        background = UIBox().apply {
            cornerRadius = 8f
            applyTheme = {
                color = it.accentColor * 0.1f
                alpha = 0.8f
            }
        }
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        try {
            isVisible = Config.isShowFPS()

            if (isVisible) {
                updateFPS()
            }
        } finally {
            super.onManagedUpdate(deltaTimeSec)
        }
    }

    private fun updateFPS() {
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

        displayedFrameTime = Interpolation.dampContinuously(
            displayedFrameTime,
            elapsedUpdateFrameTime,
            if (hasUpdateSpike) 0f else dampTime,
            elapsedUpdateFrameTime
        )

        displayedFpsCount = if (hasUpdateSpike) {
            // Show spike time using raw elapsed value, to account for `framesPerSecond` being so averaged spike frames
            // do not show.
            1f / elapsedUpdateFrameTime
        } else {
            Interpolation.dampContinuously(
                displayedFpsCount,
                updateClock.framesPerSecond,
                dampTime,
                updateClock.timeInfo.elapsed
            )
        }

        // Force update if we are below the target by a certain threshold.
        val hasSignificantChanges = aimRatesChanged || hasUpdateSpike || displayedFpsCount < aimFPS * 0.6f

        timeSinceLastUpdate += updateClock.timeInfo.elapsed

        if (!hasSignificantChanges && timeSinceLastUpdate < updateInterval) {
            return
        }

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

        timeSinceLastUpdate = 0f
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
    }

    private fun updateAimFPS(): Boolean {
        val refreshRate = getGlobal().mainActivity.refreshRate

        val newAimFPS = if (updateClock.throttling && updateClock.maximumUpdateHz > 0) {
            min(updateClock.maximumUpdateHz, refreshRate)
        } else {
            refreshRate
        }

        if (aimFPS != newAimFPS) {
            aimFPS = newAimFPS
            return true
        }

        return false
    }

    private fun updateColor() {
        val performanceRatio = displayedFpsCount / aimFPS
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