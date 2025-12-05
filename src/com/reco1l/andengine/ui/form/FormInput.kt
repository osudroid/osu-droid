package com.reco1l.andengine.ui.form

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.srem
import com.reco1l.andengine.ui.*

@Suppress("LeakingThis")
open class FormInput(private val initialValue: String = "") : FormControl<String, UITextInput>(initialValue) {

    final override val control = createControl().apply {
        width = Size.Full
    }

    override val valueText = null


    init {
        orientation = Orientation.Vertical
        style += {
            spacing = 2f.srem
        }

        linearContainer {
            width = Size.Full
            style = {
                spacing = 2f.srem
            }
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