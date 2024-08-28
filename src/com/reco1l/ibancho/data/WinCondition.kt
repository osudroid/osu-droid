package com.reco1l.ibancho.data

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

/**
 * Represents the win condition of a room.
 */
enum class WinCondition {

    /**
     * The player with the highest score (using V1 scoring system) wins.
     */
    SCORE_V1,

    /**
     * The player with the highest accuracy wins.
     */
    ACCURACY,

    /**
     * The player with the highest combo wins.
     */
    MAX_COMBO,

    /**
     * The player with the highest score (using V2 scoring system) wins.
     *
     * @see [GameMod.MOD_SCOREV2]
     */
    SCORE_V2;


    companion object {
        fun from(ordinal: Int) = entries[ordinal]
    }
}