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
         * Returns the [RoomStatus] for the given wire string, or `null` if not recognized.
         */
        fun fromWire(value: String): RoomStatus? = runCatching { enumValueOf<RoomStatus>(value) }.getOrNull()
    }
}
