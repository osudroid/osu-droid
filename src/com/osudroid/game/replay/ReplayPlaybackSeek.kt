package com.osudroid.game.replay

import com.reco1l.andengine.Anchor
import com.reco1l.andengine.container.Orientation
import com.reco1l.andengine.container.UILinearContainer
import com.reco1l.andengine.iconButton
import com.reco1l.andengine.linearContainer
import com.reco1l.andengine.textButton
import com.reco1l.andengine.ui.form.FormSlider
import com.reco1l.framework.math.Vec4
import java.text.DecimalFormat
import kotlin.math.abs
import ru.nsu.ccfit.zuev.osu.ResourceManager

class ReplayPlaybackSeek : UILinearContainer() {
    /**
     * Called when the user seeks to a position. The argument is the target time in seconds.
     */
    var onSeek: ((Float) -> Unit)? = null

    /**
     * Whether the user has paused playback via the play/pause button.
     */
    var isPlaybackPaused = false
        private set

    /**
     * Called when the user presses the play/pause button. The argument is `true` when pausing, `false` when resuming.
     */
    var onPauseToggle: ((Boolean) -> Unit)? = null

    private var minDuration = 0f
    private var totalDuration = 0f
    private var rawCurrentSeconds = 0f
    private var isUserDragging = false

    private val secondsFormatter = DecimalFormat("00")

    private val seekBar = FormSlider().apply {
        label = "Seek"
        control.min = 0f
        control.max = 0f
        showResetButton = false

        valueFormatter = {
            val seconds = (rawCurrentSeconds - minDuration).toInt()
            val duration = (totalDuration - minDuration).toInt().coerceAtLeast(0)

            val sAbs = abs(seconds)
            val sign = if (seconds < 0) "-" else ""

            "$sign${sAbs / 60}:${secondsFormatter.format(sAbs % 60)} / ${duration / 60}:${secondsFormatter.format(duration % 60)}"
        }

        onValueChanged = {
            if (isUserDragging) {
                rawCurrentSeconds = it
            }
        }

        control.onStartDragging = { isUserDragging = true }

        control.onStopDragging = {
            isUserDragging = false
            onSeek?.invoke(value)
        }
    }

    init {
        orientation = Orientation.Vertical
        width = FillParent

        +seekBar

        linearContainer {
            spacing = 10f
            padding = Vec4(0f, 16f)
            anchor = Anchor.TopCenter
            origin = Anchor.TopCenter

            fun addSeekButton(delta: Float) = textButton {
                text = "%+d s".format(delta.toInt())
                height = 42f
                padding = Vec4(12f, 0f)
                onActionUp = { onSeek?.invoke((seekBar.value + delta).coerceIn(minDuration, totalDuration)) }
            }

            addSeekButton(-10f)
            addSeekButton(-1f)

            iconButton {
                val resourceManager = ResourceManager.getInstance()

                height = 42f
                padding = Vec4(12f, 0f)
                icon = resourceManager.getTexture("music_pause")

                onActionUp = {
                    isPlaybackPaused = !isPlaybackPaused
                    icon = resourceManager.getTexture(if (isPlaybackPaused) "music_play" else "music_pause")
                    onPauseToggle?.invoke(isPlaybackPaused)
                }
            }

            addSeekButton(1f)
            addSeekButton(10f)
        }
    }

    /**
     * Updates the seek bar to reflect the current playback position.
     * Should be called from the game update thread each frame.
     *
     * @param currentSeconds The current playback position in seconds.
     * @param minSeconds The earliest seekable position in seconds (first object's start time).
     * @param maxSeconds The latest seekable position in seconds (last object's end time).
     */
    fun updateSeekPosition(currentSeconds: Float, minSeconds: Float, maxSeconds: Float) {
        if (isUserDragging) {
            return
        }

        minDuration = minSeconds
        totalDuration = maxSeconds
        rawCurrentSeconds = currentSeconds
        seekBar.control.min = minSeconds
        seekBar.control.max = maxSeconds

        if (currentSeconds < minSeconds) {
            // During lead-in, the slider stays pinned at min, so its value won't change and
            // valueFormatter won't be triggered automatically. Force a refresh to update the displayed time.
            seekBar.onControlValueChanged()
        } else {
            seekBar.value = currentSeconds.coerceAtMost(maxSeconds)
        }
    }
}