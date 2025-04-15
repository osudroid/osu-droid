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

    override val control = Slider(initialValue).apply {
        anchor = Anchor.BottomLeft
        origin = Anchor.TopLeft
        translationY = 14f
    }

    override val valueText = ExtendedText().apply {
        font = ResourceManager.getInstance().getFont("smallFont")
        anchor = Anchor.TopRight
        origin = Anchor.TopRight
        padding = Vec4(6f, 0f)
        alignment = Anchor.Center

        background = Box().apply {
            color = ColorARGB.Black
            alpha = 0.1f
            cornerRadius = 8f
        }
    }


    init {
        +labelText
        +valueText
        +control
        +resetButton

        addConstraint(control, labelText)
        addConstraint(resetButton, labelText)
    }
}