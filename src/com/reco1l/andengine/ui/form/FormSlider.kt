package com.reco1l.andengine.ui.form

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.theme.FontSize
import com.reco1l.andengine.theme.Radius
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.srem
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import ru.nsu.ccfit.zuev.osu.Config

@Suppress("LeakingThis")
open class FormSlider(initialValue: Float = 0f) : FormControl<Float, UISlider>(initialValue) {

    override val control = UISlider(initialValue).apply {
        width = Size.Full
    }

    override val valueText = UIText().apply {
        anchor = Anchor.CenterLeft
        origin = Anchor.CenterLeft
        alignment = Anchor.Center
        style = {
            fontSize = FontSize.XS
            padding = Vec4(2f.srem, 1f.srem)
            color = it.accentColor
            backgroundColor = Color4.Black / 0.1f
            radius = Radius.MD
        }
    }


    init {
        orientation = Orientation.Vertical
        style += {
            spacing = 2f.srem
        }

        fillContainer {
            width = Size.Full

            linearContainer {
                width = Size.Full
                style = {
                    spacing = 2f.srem
                }
                +labelText
                +resetButton
            }

            +valueText
        }

        +control
    }
}

class FloatPreferenceSlider(private val preferenceKey: String, fallbackValue: Float = 0f) : FormSlider(Config.getFloat(preferenceKey, fallbackValue)) {
    override fun onControlValueChanged() {
        Config.setFloat(preferenceKey, control.value)
        super.onControlValueChanged()
    }
}

class IntPreferenceSlider(private val preferenceKey: String, fallbackValue: Int = 0) : FormSlider(Config.getInt(preferenceKey, fallbackValue).toFloat()) {
    override fun onControlValueChanged() {
        Config.setInt(preferenceKey, control.value.toInt())
        super.onControlValueChanged()
    }
}