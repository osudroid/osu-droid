package com.rian.spectator

import ru.nsu.ccfit.zuev.osu.scoring.Replay.ReplayObjectData

/**
 * Represents the replay data of an object.
 */
data class SpectatorObjectData(
    /**
     * The time at which the object was hit, in milliseconds.
     */
    @JvmField
    val time: Double,

    /**
     * The replay data of the object.
     */
    @JvmField
    val data: ReplayObjectData
)