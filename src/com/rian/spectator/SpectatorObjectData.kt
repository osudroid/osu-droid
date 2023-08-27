package com.rian.spectator

import ru.nsu.ccfit.zuev.osu.scoring.Replay.ReplayObjectData

/**
 * Emitted whenever a player hits an object.
 */
data class SpectatorObjectData(
    /**
     * The score of the player after hitting this object.
     */
    @JvmField
    val currentScore: Int,

    /**
     * The combo of the player after hitting this object.
     */
    @JvmField
    val currentCombo: Int,

    /**
     * The accuracy of the player after hitting this object, from 0 to 1.
     */
    @JvmField
    val currentAccuracy: Float,

    /**
     * The replay data of the object.
     */
    @JvmField
    val data: ReplayObjectData
) 