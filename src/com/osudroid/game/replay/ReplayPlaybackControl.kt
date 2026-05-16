package com.osudroid.game.replay

import com.reco1l.andengine.Anchor
import com.reco1l.andengine.iconButton
import com.reco1l.andengine.linearContainer
import com.reco1l.andengine.textButton
import com.reco1l.andengine.ui.UICard
import com.reco1l.andengine.ui.form.FormSlider
import com.reco1l.framework.math.Vec4
import java.text.DecimalFormat
import ru.nsu.ccfit.zuev.osu.ResourceManager

/**
 * Controls playback of gameplay when replaying.
 */
class ReplayPlaybackControl : UICard() {
    /**
     * The rate at which gameplay should progress.
     */
    var rate = 1f
        private set

    /**
     * Whether the user has paused playback via the play/pause button.
     */
    var isPlaybackPaused = false
        private set

    /**
     * Called when the user presses the play/pause button. The argument is `true` when pausing, `false` when resuming.
     */
    var onPauseToggle: ((Boolean) -> Unit)? = null

    private val rateFormatter = DecimalFormat("0.00x")

    init {
        val resourceManager = ResourceManager.getInstance()

        width = FillParent
        title = "Playback"

        content.apply {
            val slider = FormSlider().apply {
                label = "Playback speed"
                control.min = 0.05f
                control.max = 2f
                value = rate
                defaultValue = rate
                valueFormatter = { rateFormatter.format(it) }
                onValueChanged = { rate = it }
            }

            +slider

            linearContainer {
                spacing = 10f
                padding = Vec4(0f, 16f)
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter

                fun addStepButton(step: Float) = textButton {
                    text = "%+.2f".format(step)
                    height = 42f
                    padding = Vec4(12f, 0f)
                    onActionUp = { slider.value += step }
                }

                addStepButton(-0.05f)
                addStepButton(-0.01f)

                iconButton {
                    height = 42f
                    padding = Vec4(12f, 0f)
                    icon = resourceManager.getTexture("music_pause")

                    onActionUp = {
                        isPlaybackPaused = !isPlaybackPaused
                        icon = resourceManager.getTexture(if (isPlaybackPaused) "music_play" else "music_pause")
                        onPauseToggle?.invoke(isPlaybackPaused)
                    }
                }

                addStepButton(0.01f)
                addStepButton(0.05f)
            }
        }
    }
}
