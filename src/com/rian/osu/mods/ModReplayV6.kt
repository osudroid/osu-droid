package com.rian.osu.mods

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.hitobject.HitCircle
import com.rian.osu.utils.CircleSizeCalculator
import kotlin.math.pow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * Represents the Replay V6 mod.
 *
 * Some behavior of beatmap parsing was changed in replay version 7. More specifically, stacking behavior now matches
 * osu!stable and osu!lazer.
 *
 * This [Mod] is meant to reapply the stacking behavior prior to replay version 7 to a [Beatmap] that was played in
 * replays recorded in version 6 and older for replayability and difficulty calculation.
 */
class ModReplayV6 : Mod(), IModApplicableToBeatmap {
    override val name = "Replay V6"
    override val acronym = "RV6"
    override val description = "Applies the old behavior of stacking to a beatmap."
    override val type = ModType.System

    override val isUserPlayable = false

    override fun applyToBeatmap(beatmap: Beatmap, scope: CoroutineScope?) {
        val objects = beatmap.hitObjects.objects

        if (objects.isEmpty()) {
            return
        }

        val droidDifficultyScale =
            CircleSizeCalculator.standardScaleToDroidDifficultyScale(objects[0].difficultyScale, true)

        val maxDeltaTime = 2000 * beatmap.general.stackLeniency

        for (i in 0 until objects.size - 1) {
            scope?.ensureActive()

            val current = objects[i]
            val next = objects[i + 1]

            next.stackOffsetMultiplier = 4f

            if (current is HitCircle && next.startTime - current.startTime < maxDeltaTime) {
                val distanceSquared = next.position.getDistance(current.position).pow(2)

                next.difficultyStackHeight =
                    if (distanceSquared < droidDifficultyScale) current.difficultyStackHeight + 1
                    else 0

                next.gameplayStackHeight =
                    if (distanceSquared < current.gameplayScale) current.gameplayStackHeight + 1
                    else 0
            }
        }
    }

    override fun deepCopy() = ModReplayV6()
}