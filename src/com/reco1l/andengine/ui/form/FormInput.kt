package com.reco1l.andengine.ui.form

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.math.*

@Suppress("LeakingThis")
open class FormInput(initialValue: String = "") : FormControl<String, TextInput>(initialValue) {

    override val control = TextInput(initialValue).apply {
        width = FillParent
    }

    override val valueText = null


    init {
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