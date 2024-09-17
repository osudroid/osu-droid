package com.rian.osu.replay

import ru.nsu.ccfit.zuev.osu.scoring.Replay.ReplayMovement
import ru.nsu.ccfit.zuev.osu.scoring.TouchType

/**
 * Represents a group of cursor movements representing a cursor instance's movement when a player places their finger
 * on the screen.
 */
data class CursorGroup(
    /**
     * The cursor movement of type [TouchType.DOWN].
     */
    @JvmField
    val down: ReplayMovement,

    /**
     * The cursor movements of type [TouchType.MOVE].
     */
    @JvmField
    val moves: List<ReplayMovement>,

    /**
     * The cursor movement of type [TouchType.UP].
     *
     * May not exist, such as when the player holds their cursor until the end of a beatmap.
     */
    @JvmField
    val up: ReplayMovement? = null
) {
    /**
     * The start time of this [CursorGroup].
     */
    val startTime
        get() = down.time

    /**
     * The end time of this [CursorGroup].
     */
    val endTime
        get() = up?.time ?: moves.lastOrNull()?.time ?: down.time

    /**
     * All cursor movements in this [CursorGroup].
     *
     * This iterates through all movements and as such should be used sparingly or stored locally.
     */
    val allMovements
        get() = mutableListOf<ReplayMovement>().apply {
            add(down)
            addAll(moves)
            up?.let { add(it) }
        }

    /**
     * Determines whether this [CursorGroup] is active at the specified time.
     *
     * @param time The time.
     * @return Whether this [CursorGroup] is active at the specified time.
     */
    fun isActiveAt(time: Double) = time in startTime.toDouble()..endTime.toDouble()
}