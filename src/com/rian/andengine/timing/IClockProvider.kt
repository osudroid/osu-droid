package com.rian.andengine.timing

/**
 * An interface for classes that provides an [IClock].
 *
 * @param T The type of [IClock] provided.
 */
interface IClockProvider<out T : IClock?> {
    /**
     * The [IClock] provided by this [IClockProvider].
     */
    val clock: T
}