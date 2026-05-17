package com.rian.andengine.timing

/**
 * An [IClock] that can be started, stopped, reset, etc.
 */
interface IAdjustableClock : IClock {
    /**
     * Stop and reset position.
     */
    fun reset()

    /**
     * Start (resume) running.
     */
    fun start()

    /**
     * Stop (pause) running.
     */
    fun stop()

    /**
     * Seek to a specified time position.
     *
     * @param position The time position in seconds.
     * @return Whether a seek was possible.
     */
    fun seek(position: Float): Boolean

    /**
     * The rate this [IAdjustableClock] is running at, relative to real-time.
     */
    override var rate: Float

    /**
     * Reset the rate to a stable value.
     */
    fun resetSpeedAdjustments()
}