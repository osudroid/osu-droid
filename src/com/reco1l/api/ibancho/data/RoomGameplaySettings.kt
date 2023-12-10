package com.reco1l.api.ibancho.data

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
    var isRemoveSliderLock: Boolean,

    /**
     * Whether this room allows the use of forced difficulty statistics
     * under free mod.
     */
    var allowForceDifficultyStatistics: Boolean,

    /**
     * Whether this room allows players to place more than three cursors at
     * once during gameplay.
     */
    var allowMoreThanThreeCursors: Boolean
)