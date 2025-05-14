package com.reco1l.andengine.ui.form

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.math.*

@Suppress("LeakingThis")
open class FormInput(initialValue: String = "") : FormControl<String, TextInput>(initialValue) {

    override val control = TextInput(initialValue)
    override val valueText = null


    init {
        control.width = FillParent

        orientation = Orientation.Vertical
        spacing = 12f

        linearContainer {
            width = FillParent
            padding = Vec4(0f, 12f)
            spacing = 12f
            +labelText
            +resetButton
        }
        +control
    }
}

open class IntegerFormInput(
    initialValue: Int,
    minValue: Int? = -Int.MAX_VALUE,
    maxValue: Int? = Int.MAX_VALUE
) : FormInput(initialValue.toString()) {
    override val control = IntegerTextInput(initialValue, minValue, maxValue)
}

open class FloatFormInput(
    initialValue: Float,
    minValue: Float? = -Float.MAX_VALUE,
    maxValue: Float? = Float.MAX_VALUE
) : FormInput(initialValue.toString()) {
    override val control = FloatTextInput(initialValue, minValue, maxValue)
}