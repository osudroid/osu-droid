package com.osudroid.multiplayer.api.data

import com.osudroid.mods.ModScoreV2

/**
 * Represents the win condition of a room.
 */
enum class WinCondition {

    /**
     * The player with the highest score (using V1 scoring system) wins.
     */
    ScoreV1,

    /**
     * The player with the highest accuracy wins.
     */
    Accuracy,

    /**
     * The player with the highest combo wins.
     */
    MaxCombo,

    /**
     * The player with the highest score (using V2 scoring system) wins.
     *
     * @see [ModScoreV2]
     */
    ScoreV2;


    companion object {
        /**
         * Returns the [WinCondition] for the given wire string, or `null` if not recognized.
         */
        fun fromWire(value: String): WinCondition? = runCatching { enumValueOf<WinCondition>(value) }.getOrNull()
    }
}
