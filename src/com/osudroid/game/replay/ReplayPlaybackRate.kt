package com.osudroid.game.replay

import com.reco1l.andengine.Anchor
import com.reco1l.andengine.container.Orientation
import com.reco1l.andengine.container.UILinearContainer
import com.reco1l.andengine.linearContainer
import com.reco1l.andengine.textButton
import com.reco1l.andengine.ui.form.FormSlider
import com.reco1l.framework.math.Vec4
import java.text.DecimalFormat

class ReplayPlaybackRate : UILinearContainer() {
    /**
     * The rate at which gameplay should progress.
     */
    var rate = 1f
        private set

    private val rateFormatter = DecimalFormat("0.00x")

    init {
        orientation = Orientation.Vertical
        width = FillParent

        val slider = FormSlider(rate).apply {
            label = "Playback speed"
            control.min = 0.05f
            control.max = 2f
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
            addStepButton(0.01f)
            addStepButton(0.05f)
        }
    }
}