package com.reco1l.andengine.ui.form

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.math.*

open class FormSelect<T : Any>(initialValues: List<T> = emptyList()) : FormControl<List<T>, UISelect<T>>(initialValues) {

    final override val control = UISelect<T>().apply {
        width = FillParent
    }

    // Value is already reflected in the control.
    override val valueText = null


    var selectionMode by control::selectionMode

    var placeholder by control::placeholder

    var options by control::options


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