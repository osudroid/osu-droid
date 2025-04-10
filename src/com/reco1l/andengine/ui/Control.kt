package com.reco1l.andengine.ui

import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.ui.form.*

/**
 * Interface for a control that has a value and can notify when the value changes.
 */
abstract class Control<T : Any?>(initialValue: T) : Container() {

    /**
     * The value of the control.
     */
    open var value = initialValue
        set(value) {
            if (field != value) {
                field = value
                onValueChanged()
            }
        }

    /**
     * Whether the control is enabled or not. If disabled, the control will not process any
     * touch events.
     */
    open var isEnabled = true
        set(value) {
            if (field != value) {
                field = value
                onEnableStateChange()
            }
        }


    //region Callbacks

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


    /**
     * Called when the enable state of the button changes.
     */
    open fun onEnableStateChange() {
        clearModifiers(ModifierType.Alpha)

        if (isEnabled) {
            fadeTo(1f, 0.2f)
        } else {
            fadeTo(0.25f, 0.2f)
        }
    }

    //endregion
}