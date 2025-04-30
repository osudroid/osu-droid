package com.reco1l.andengine.ui.form

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ResourceManager

@Suppress("LeakingThis")
open class FormSlider(initialValue: Float = 0f) : FormControl<Float, Slider>(initialValue) {

    override val control = Slider(initialValue).apply {
        width = FillParent
    }

    override val valueText = ExtendedText().apply {
        font = ResourceManager.getInstance().getFont("smallFont")
        anchor = Anchor.CenterRight
        origin = Anchor.CenterRight
        padding = Vec4(6f, 0f)
        alignment = Anchor.Center
        onThemeChange = { color = it.accentColor }

        background = Box().apply {
            color = ColorARGB.Black
            alpha = 0.1f
            cornerRadius = 8f
        }
    }


    init {
        orientation = Orientation.Vertical
        spacing = 12f

        linearContainer {
            width = FillParent
            padding = Vec4(0f, 12f)
            spacing = 12f
            +labelText
            +resetButton

            container {
                width = FillParent
                +valueText
            }
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