package com.rian.andengine.timing

/**
 * An [IClock] which has a source that can be changed.
 */
interface ISourceChangeableClock : IClock {
    /**
     * The source [IClock].
     */
    val source: IClock

    /**
     * Change the source [IClock].
     *
     * @param source The new source [IClock].
     */
    fun changeSource(source: IClock?)
}