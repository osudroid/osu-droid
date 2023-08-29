package com.rian.spectator

/**
 * Represents a spectator event.
 */
data class SpectatorEvent(
    /**
     * The time at which the event occurred, in milliseconds.
     */
    @JvmField
    val time: Float,

    /**
     * The score of the player after this event.
     */
    @JvmField
    val score: Int,

    /**
     * The combo of the player after this event.
     */
    @JvmField
    val combo: Int,

    /**
     * The accuracy of the player after this event, from 0 to 1.
     */
    @JvmField
    val accuracy: Float,
)