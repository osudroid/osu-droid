package com.rian.andengine.timing

/**
 * A basic clock for keeping time.
 */
interface IClock {
    /**
     * The current time of this [IClock], in milliseconds.
     */
    val currentTime: Double

    /**
     * The rate this [IClock] is running at, relative to real-time.
     */
    val rate: Double

    /**
     * Whether this [IClock] is currently running.
     */
    val isRunning: Boolean
}