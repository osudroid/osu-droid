package com.reco1l.andengine.ui

import com.reco1l.andengine.*
import com.reco1l.andengine.component.*

interface IFocusable {


    val isFocused
        get() = UIEngine.current.focusedEntity == this as? UIComponent


    /**
     * Called when the entity is focused.
     */
    fun onFocus()

    /**
     * Called when the entity is blurred.
     */
    fun onBlur()


    /**
     * Request focus for this entity.
     */
    fun focus() {
        UIEngine.current.focusedEntity = this as UIComponent
    }

    /**
     * Clear focus for this entity.
     */
    fun blur() {
        if (UIEngine.current.focusedEntity == this) {
            UIEngine.current.focusedEntity = null
        }
    }
}
