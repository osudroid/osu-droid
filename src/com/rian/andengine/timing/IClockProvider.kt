package com.rian.andengine.timing

/**
 * Provides an [IClock] to its implementors.
 *
 * @param T The type of [IClock] provided.
 */
interface IClockProvider<out T : IClock?> {
    /**
     * The [IClock] provided by this [IClockProvider].
     */
    val clock: T
}