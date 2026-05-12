package com.rian.andengine.timing

/**
 * An interface for classes that can receive an [IClock], either to be used as a custom/inherited clock or for other
 * purposes.
 *
 * @param T The type of [IClock] that can be received by this [IClockReceiver].
 */
interface IClockReceiver<in T : IClock?> {
    /**
     * Updates the [IClock] for this [IClockReceiver].
     *
     * @param clock The [IClock] to use.
     */
    fun updateClock(clock: T)
}