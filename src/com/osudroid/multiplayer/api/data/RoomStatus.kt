package com.osudroid.multiplayer.api.data

/**
 * Represents the status of a room.
 */
enum class RoomStatus {

    /**
     * The room is idle (not playing).
     */
    Idle,

    /**
     * The room is in the process of changing the beatmap.
     */
    ChangingBeatmap,

    /**
     * The room is playing.
     */
    Playing;


    companion object {
        /**
         * Returns the [RoomStatus] for [ordinal], or `null` if the ordinal is not recognised (EH-1).
         */
        operator fun get(ordinal: Int): RoomStatus? = entries.getOrNull(ordinal)
    }
}