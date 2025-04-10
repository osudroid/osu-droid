package com.reco1l.andengine.ui.form

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import ru.nsu.ccfit.zuev.osu.ResourceManager

@Suppress("LeakingThis")
open class FormSlider(initialValue: Float = 0f) : FormControl<Float, Slider>(initialValue) {

    override val control = Slider(initialValue)

    override val labelText = ExtendedText().apply {
        font = ResourceManager.getInstance().getFont("smallFont")
        padding = Vec4(0f, 0f, 0f, 18f)
    }

    override val valueText = ExtendedText().apply {
        font = ResourceManager.getInstance().getFont("smallFont")
        anchor = Anchor.TopRight
        origin = Anchor.TopRight
        padding = Vec4(4f, 0f)
        alignment = Anchor.Center

        background = RoundedBox().apply {
            color = ColorARGB.Black
            alpha = 0.1f
            cornerRadius = 8f
        }
    }


    init {
        attachChild(LinearContainer().apply {
            width = FitParent
            padding = Vec4(24f)
            orientation = Orientation.Vertical

            attachChild(Container().apply {
                width = FitParent
                attachChild(labelText)
                attachChild(valueText)
            })

            attachChild(control)
        })
    }
}