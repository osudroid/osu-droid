package com.reco1l.ibancho.data

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

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
    HighestAccuracy,

    /**
     * The player with the highest combo wins.
     */
    MaximumCombo,

    /**
     * The player with the highest score (using V2 scoring system) wins.
     *
     * @see [GameMod.MOD_SCOREV2]
     */
    ScoreV2;


    companion object {
        fun from(ordinal: Int) = entries[ordinal]
    }
}