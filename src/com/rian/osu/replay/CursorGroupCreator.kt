package com.rian.osu.replay

import ru.nsu.ccfit.zuev.osu.scoring.Replay
import ru.nsu.ccfit.zuev.osu.scoring.Replay.MoveArray
import ru.nsu.ccfit.zuev.osu.scoring.TouchType

/**
 * Creates a list of [CursorGroup]s from a list of [MoveArray]s.
 *
 * @param moves The list of [MoveArray]s.
 * @return The list of [CursorGroup]s.
 */
fun createCursorGroups(moves: List<MoveArray>) = mutableListOf<List<CursorGroup>>().apply {
    for (move in moves) {
        val groups = mutableListOf<CursorGroup>()
        var downMovement: Replay.ReplayMovement? = null
        var moveMovements = mutableListOf<Replay.ReplayMovement>()

        move.movements.forEach {
            when (it.touchType) {
                TouchType.DOWN -> downMovement = it
                TouchType.MOVE -> moveMovements.add(it)
                TouchType.UP -> {
                    if (downMovement != null) {
                        groups.add(CursorGroup(downMovement!!, moveMovements, it))
                    }

                    downMovement = null
                    moveMovements = mutableListOf()
                }
                else -> return@forEach
            }
        }

        // Add the final group as the loop may not catch it for special cases.
        if (downMovement != null && moveMovements.isNotEmpty()) {
            groups.add(CursorGroup(downMovement!!, moveMovements))
        }

        add(groups)
    }
}.toList()