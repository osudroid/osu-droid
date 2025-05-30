package com.reco1l.andengine.ui

import com.reco1l.andengine.*

interface IFocusable {


    val isFocused
        get() = ExtendedEngine.Current.focusedEntity == this as? ExtendedEntity


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
        ExtendedEngine.Current.focusedEntity = this as ExtendedEntity
    }

    /**
     * Clear focus for this entity.
     */
    fun blur() {
        if (ExtendedEngine.Current.focusedEntity == this) {
            ExtendedEngine.Current.focusedEntity = null
        }
    }
}
