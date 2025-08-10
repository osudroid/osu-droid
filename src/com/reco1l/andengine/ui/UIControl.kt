package com.reco1l.andengine.ui

import com.reco1l.andengine.container.*
import com.reco1l.andengine.ui.form.*

/**
 * Interface for a control that has a value and can notify when the value changes.
 */
abstract class UIControl<T : Any>(initialValue: T) : UIContainer() {

    /**
     * The key of the control, used for identification in [FormContainer] during submission.
     */
    var key: String? = null

    /**
     * The value of the control.
     */
    var value = initialValue
        set(value) {
            if (field != value) {
                field = onProcessValue(value)
                onValueChanged()
            }
        }


    //region Callbacks

    /**
     * Processes the value of the control. This is called when the value is set to a new value.
     */
    open fun onProcessValue(value: T) = value

    /**
     * The callback that is called when the value of the control changes.
     */
    open fun onValueChanged() {
        var parent = parent
        while (parent != null) {
            if (parent is FormControl<*, *>) {
                parent.onControlValueChanged()
                break
            }
            parent = parent.parent
        }
    }

    //endregion
}