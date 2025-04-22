package com.osudroid.multiplayer.api.data

/**
 * Represents gameplay-related settings of a multiplayer room.
 */
data class RoomGameplaySettings(
    /**
     * Whether this room allows free mod.
     */
    var isFreeMod: Boolean,

    /**
     * Whether this room has the remove slider lock setting enabled.
     */
    var isRemoveSliderLock: Boolean
)