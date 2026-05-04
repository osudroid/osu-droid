package com.rian.andengine.timing

/**
 * A basic clock for keeping time.
 */
interface IClock {
    /**
     * The current time of this [IClock], in milliseconds.
     */
    val currentTime: Float

    /**
     * The rate this [IClock] is running at, relative to real-time.
     */
    val rate: Float

    /**
     * Whether this [IClock] is currently running.
     */
    val isRunning: Boolean
}