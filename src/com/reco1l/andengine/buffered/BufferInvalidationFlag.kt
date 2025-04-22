package com.reco1l.andengine.buffered

import androidx.annotation.*

@Suppress("ConstPropertyName")
@IntDef(
    BufferInvalidationFlag.Instance,
    BufferInvalidationFlag.Data
)
annotation class BufferInvalidationFlag {
    companion object {

        /**
         * Indicates that the buffer should be rebuilt.
         */
        const val Instance = 1

        /**
         * Indicates that the buffer's data should be updated.
         */
        const val Data = 1 shl 1

    }
}
