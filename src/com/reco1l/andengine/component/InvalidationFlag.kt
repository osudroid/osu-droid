package com.reco1l.andengine.component

import androidx.annotation.*

@Suppress("ConstPropertyName")
@IntDef(value = [
    InvalidationFlag.Size,
    InvalidationFlag.Position,
    InvalidationFlag.Content,
    InvalidationFlag.Transformations,
    InvalidationFlag.InputBindings
])
annotation class InvalidationFlag {
    companion object {

        /**
         * The size of the entity has changed. Calls [UIComponent.onSizeChanged].
         */
        const val Size = 1

        /**
         * The content of the entity has changed. Calls [UIComponent.onContentChanged].
         */
        const val Content = 1 shl 1

        /**
         * The position of the entity has changed. Calls [UIComponent.onPositionChanged].
         */
        const val Position = 1 shl 2

        /**
         * The transformations of the entity have changed. Calls [UIComponent.onInvalidateTransformations].
         */
        const val Transformations = 1 shl 3

        /**
         * The input bindings were removed. Calls [UIComponent.onInvalidateInputBindings].
         */
        const val InputBindings = 1 shl 4

    }
}