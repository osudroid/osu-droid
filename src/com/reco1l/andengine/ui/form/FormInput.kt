package com.reco1l.andengine.ui.form

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.math.*

@Suppress("LeakingThis")
open class FormInput(private val initialValue: String = "") : FormControl<String, UITextInput>(initialValue) {

    final override val control = createControl().apply {
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

    open fun createControl() = UITextInput(initialValue)
}

open class IntegerFormInput(
    initialValue: Int?,
    val minValue: Int? = -Int.MAX_VALUE,
    val maxValue: Int? = Int.MAX_VALUE
) : FormInput(initialValue?.toString() ?: "") {
    override fun createControl() = IntegerTextInput(defaultValue.toIntOrNull(), minValue, maxValue)
}

open class FloatFormInput(
    initialValue: Float?,
    val minValue: Float? = -Float.MAX_VALUE,
    val maxValue: Float? = Float.MAX_VALUE
) : FormInput(initialValue?.toString() ?: "") {
    override fun createControl() = FloatTextInput(defaultValue.toFloatOrNull(), minValue, maxValue)
}