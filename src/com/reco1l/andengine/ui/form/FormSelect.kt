package com.reco1l.andengine.ui.form

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.srem
import com.reco1l.andengine.ui.*

open class FormSelect<T : Any>(initialValues: List<T> = emptyList()) : FormControl<List<T>, UISelect<T>>(initialValues) {

    final override val control = UISelect<T>().apply {
        width = Size.Full
    }

    override val valueText = null


    //region Shortcuts

    var selectionMode by control::selectionMode
    var placeholder by control::placeholder
    var options by control::options

    //endregion


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

}